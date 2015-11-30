package com.splicemachine.si.impl.checkpoint;

import com.splicemachine.constants.SIConstants;
import com.splicemachine.si.api.*;
import com.splicemachine.si.data.hbase.HRowAccumulator;
import com.splicemachine.si.impl.ActiveReadTxn;
import com.splicemachine.si.impl.CheckpointFilter;
import com.splicemachine.si.impl.DataStore;
import com.splicemachine.si.impl.TxnFilter;
import com.splicemachine.storage.EntryDecoder;
import com.splicemachine.storage.EntryPredicateFilter;
import com.splicemachine.utils.ByteSlice;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Get;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * @author Scott Fines
 *         Date: 11/3/15
 */
@ThreadSafe
public class PartitionCheckpointResolver implements CheckpointResolver{
    private final Partition partition;
    private final Checkpointer checkpointer;
    private final DataStore dataStore;
    private final TxnSupplier txnSupplier;
    private final int checkpointThreshold;
    private final TxnFilterFactory filterFactory;

    public PartitionCheckpointResolver(Partition partition,
                                       int checkpointThreshold,
                                       Checkpointer checkpointer,
                                       DataStore dataStore,
                                       TxnSupplier txnSupplier,
                                       TxnFilterFactory txnFilterFactory){
        this.partition=partition;
        this.checkpointer= checkpointer;
        this.dataStore=dataStore;
        this.txnSupplier = txnSupplier;
        assert checkpointThreshold>0: "Checkpoint threshold set too low!";
        this.checkpointThreshold = checkpointThreshold;
        this.filterFactory = txnFilterFactory;
    }

    @Override
    public void resolveCheckpoint(Checkpoint... checkpoint) throws IOException{
        if(checkpoint.length<=0) return; //nothing to do
        else if(checkpoint.length==1){
            Checkpoint cp=checkpoint[0];
            byte[] rowKey=cp.rowKey;
            int keyOff = cp.offset;
            int len = cp.length;

            resolveCheckpoint(rowKey,keyOff,len,cp.checkpointTimestamp);
            return;
        }

        RowAccumulator<Cell> accumulator =new HRowAccumulator<>(
                dataStore.getDataLib(),EntryPredicateFilter.EMPTY_PREDICATE,
                new EntryDecoder(),false);
        //noinspection ForLoopReplaceableByForEach
        for(int i=0;i<checkpoint.length;i++){
            accumulator.reset();
            Checkpoint cp = checkpoint[i];
            doResolve(cp.rowKey,cp.offset,cp.length,cp.checkpointTimestamp,accumulator);
        }
    }

    @Override
    public void resolveCheckpoint(ByteSlice rowKey,long checkpointTimestamp) throws IOException{
        resolveCheckpoint(rowKey.array(),rowKey.offset(),rowKey.length(),checkpointTimestamp);
    }

    @Override
    public void resolveCheckpoint(byte[] rowKeyArray,int offset,int length,long checkpointTimestamp) throws IOException{

        RowAccumulator<Cell> accumulator =new HRowAccumulator<>(
                dataStore.getDataLib(),EntryPredicateFilter.EMPTY_PREDICATE,
                new EntryDecoder(),false);
        doResolve(rowKeyArray,offset,length,checkpointTimestamp,accumulator);
    }

    /* ****************************************************************************************************************/
    /*private helper methods*/
    private void doResolve(byte[] rowKeyArray,int offset,int length,
                           long checkpointTimestamp,RowAccumulator<Cell> accumulator) throws IOException{
        byte[] rowKey = copy(rowKeyArray,offset,length);
        Lock lock=partition.lock(rowKey);
        if(lock.tryLock()){
            Get get = checkpointGet(rowKey,checkpointTimestamp);
            try{
                List<Cell> cells=partition.get(get);
                if(cells==null||cells.size()<checkpointThreshold) return; //nothing to do
                long highestTs = cells.get(0).getTimestamp();

                for(Cell c:cells){
                    if(accumulator.isOfInterest(c)){
                        accumulator.accumulate(c);
                    }
                    if(accumulator.isFinished()) break; //should never happen, but just in case
                }

                long cTs=txnSupplier.getTransaction(highestTs).getGlobalCommitTimestamp();
                checkpointer.checkpoint(rowKeyArray,offset,length, accumulator.result(), highestTs,cTs);
            }finally{
                lock.unlock();
            }
        }
    }

    private byte[] copy(byte[] rowKeyArray,int offset,int length){
        byte[] rk = new byte[length];
        System.arraycopy(rowKeyArray,offset,rk,0,length);
        return rk;
    }

    private Get checkpointGet(byte[] rowKey,long checkpointTimestamp) throws IOException{
        Get get = new Get(rowKey);
        get.setMaxVersions();
        get.setTimeRange(0,checkpointTimestamp+1);
        TxnFilter<Cell> txnFilter=filterFactory.unpackedFilter(new ActiveReadTxn(checkpointTimestamp));
        CheckpointFilter filter=new CheckpointFilter(txnFilter,SIConstants.checkpointSeekThreshold,NoOpCheckpointResolver.INSTANCE,0l);
        get.setFilter(filter);
        get.setAttribute(SIConstants.SUPPRESS_INDEXING_ATTRIBUTE_NAME,SIConstants.SUPPRESS_INDEXING_ATTRIBUTE_VALUE);

        return get;
    }
}