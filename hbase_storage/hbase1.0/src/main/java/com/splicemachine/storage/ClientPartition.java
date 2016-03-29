package com.splicemachine.storage;

import com.google.common.base.Function;
import org.sparkproject.guava.collect.ImmutableList;
import org.sparkproject.guava.collect.Iterables;
import org.sparkproject.guava.collect.Iterators;
import com.google.protobuf.Service;
import com.splicemachine.concurrent.Clock;
import com.splicemachine.kvpair.KVPair;
import com.splicemachine.primitives.Bytes;
import com.splicemachine.storage.util.PartitionInRangePredicate;
import com.splicemachine.utils.Pair;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.protobuf.generated.AdminProtos;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Represents an HBase Table as a single Partition.
 *
 * @author Scott Fines
 *         Date: 12/17/15
 */
@NotThreadSafe
public class ClientPartition extends SkeletonHBaseClientPartition{
    private TableName tableName;
    private final Table table;
    private final Connection connection;
    private final Clock clock;
    private PartitionInfoCache partitionInfoCache;

    public ClientPartition(Connection connection,
                           TableName tableName,
                           Table table,
                           Clock clock,
                           PartitionInfoCache partitionInfoCache){
        assert tableName!=null:"Passed in tableName is null";
        this.tableName=tableName;
        this.table=table;
        this.connection=connection;
        this.clock = clock;
        this.partitionInfoCache = partitionInfoCache;
    }

    @Override
    public String getTableName(){
        return table.getName().getQualifierAsString();
    }

    @Override
    public void close() throws IOException{
        table.close();
    }

    @Override
    protected Result doGet(Get get) throws IOException{
        return table.get(get);
    }

    @Override
    protected ResultScanner getScanner(Scan scan) throws IOException{
        return table.getScanner(scan);
    }

    @Override
    protected void doDelete(Delete delete) throws IOException{
        table.delete(delete);
    }

    @Override
    protected void doPut(Put put) throws IOException{
        table.put(put);
    }

    @Override
    protected void doPut(List<Put> puts) throws IOException{
        table.put(puts);
    }

    @Override
    protected long doIncrement(Increment incr) throws IOException{
        return Bytes.toLong(table.increment(incr).value());
    }

    @Override
    public Iterator<DataResult> batchGet(Attributable attributes,List<byte[]> rowKeys) throws IOException{
        List<Get> gets=new ArrayList<>(rowKeys.size());
        for(byte[] rowKey : rowKeys){
            Get g=new Get(rowKey);
            if(attributes!=null){
                for(Map.Entry<String, byte[]> attr : attributes.allAttributes().entrySet()){
                    g.setAttribute(attr.getKey(),attr.getValue());
                }
            }
            gets.add(g);
        }
        Result[] results=table.get(gets);
        if(results.length<=0) return Collections.emptyIterator();
        final HResult retResult=new HResult();
        return Iterators.transform(Iterators.forArray(results),new Function<Result, DataResult>(){
            @Override
            public DataResult apply(Result input){
                retResult.set(input);
                return retResult;
            }
        });
    }

    @Override
    public boolean checkAndPut(byte[] key,byte[] family,byte[] qualifier,byte[] expectedValue,DataPut put) throws IOException{
        return table.checkAndPut(key,family,qualifier,expectedValue,((HPut)put).unwrapDelegate());
    }

    @Override
    public List<Partition> subPartitions(){
        return subPartitions(false);
    }

    public List<Partition> subPartitions(boolean refresh) {
        try {
            List<Partition> partitions;
            if (!refresh) {
                partitions = partitionInfoCache.getIfPresent(tableName);
                if (partitions == null) {
                    partitions = formatPartitions(getAllRegionLocations());
                    assert partitions!=null:"partitions are null";
                    partitionInfoCache.put(tableName, partitions);
                }
                return partitions;
            }
            partitions = formatPartitions(getAllRegionLocations());
            partitionInfoCache.invalidate(tableName);
            assert partitions!=null:"partitions are null";
            partitionInfoCache.put(tableName,partitions);
            return partitions;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Partition> formatPartitions(List<HRegionLocation> tableLocations) {
        List<Partition> partitions=new ArrayList<>(tableLocations.size());
        for(HRegionLocation location : tableLocations){
            HRegionInfo regionInfo=location.getRegionInfo();
            partitions.add(new RangedClientPartition(connection,tableName,table,regionInfo,new RLServer(location),clock,partitionInfoCache));
        }
        return partitions;
    }

    @Override
    public PartitionServer owningServer(){
        throw new UnsupportedOperationException("A Table is not owned by a single server, but by the cluster as a whole");
    }
    @Override
    public List<Partition> subPartitions(byte[] startRow,byte[] stopRow) {
        return subPartitions(startRow,stopRow,false);
    }

    @Override
    public List<Partition> subPartitions(byte[] startRow,byte[] stopRow, boolean refresh) {
        return ImmutableList.copyOf(Iterables.filter(subPartitions(refresh),new PartitionInRangePredicate(startRow,stopRow)));
    }

    @Override
    public PartitionLoad getLoad() throws IOException{
        Map<ServerName, List<HRegionInfo>> serverToRegionMap=new HashMap<>();
        try(RegionLocator rl=connection.getRegionLocator(tableName)){
            List<HRegionLocation> locations=rl.getAllRegionLocations();
            for(HRegionLocation location : locations){
                List<HRegionInfo> info=serverToRegionMap.get(location.getServerName());
                if(info==null){
                    info = new LinkedList<>();
                    serverToRegionMap.put(location.getServerName(),info);
                }
                info.add(location.getRegionInfo());
            }
        }
        int totalStoreFileSizeMB = 0;
        int totalMemstoreSieMB = 0;
        int storefileIndexSizeMB = 0;
        try(Admin admin=connection.getAdmin()){
            ClusterStatus clusterStatus=admin.getClusterStatus();
            for(Map.Entry<ServerName,List<HRegionInfo>> entry:serverToRegionMap.entrySet()){
                ServerLoad load=clusterStatus.getLoad(entry.getKey());
                Map<byte[], RegionLoad> regionsLoad=load.getRegionsLoad();
                for(HRegionInfo info:entry.getValue()){
                    RegionLoad rl = regionsLoad.get(info.getRegionName());
                    totalStoreFileSizeMB+=rl.getStorefileSizeMB();
                    totalMemstoreSieMB+=rl.getMemStoreSizeMB();
                    storefileIndexSizeMB+=rl.getStorefileIndexSizeMB();
                }
            }
        }
        return new HPartitionLoad(getName(),totalStoreFileSizeMB,totalMemstoreSieMB,storefileIndexSizeMB);
    }

    @Override
    public void compact() throws IOException{
        //TODO -sf- this is inherently synchronous
        try(Admin admin = connection.getAdmin()){
            admin.majorCompact(tableName);
            AdminProtos.GetRegionInfoResponse.CompactionState compactionState;
            do{
                try{
                    clock.sleep(500l,TimeUnit.MILLISECONDS);
                }catch(InterruptedException e){
                    throw new InterruptedIOException();
                }
                compactionState=admin.getCompactionState(tableName);
            }while(compactionState!=AdminProtos.GetRegionInfoResponse.CompactionState.NONE);
        }
    }

    /**
     * Flush a table. Synchronous operation.
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        try(Admin admin = connection.getAdmin()) {
            admin.flush(tableName);
        }
    }

    public <T extends Service,V> Map<byte[],V> coprocessorExec(Class<T> serviceClass,Batch.Call<T,V> call) throws Throwable{
        return table.coprocessorService(serviceClass,getStartKey(),getEndKey(),call);
    }

    public Table unwrapDelegate(){
        return table;
    }


    private List<HRegionLocation> getAllRegionLocations() throws IOException {
        try(RegionLocator regionLocator=connection.getRegionLocator(tableName)){
            return regionLocator.getAllRegionLocations();
        }
    }

    @Override
    public BitSet getBloomInMemoryCheck(boolean hasConstraintChecker,Pair<KVPair, Lock>[] dataAndLocks) throws IOException {
        return null;
    }
}
