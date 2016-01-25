package com.splicemachine.access.hbase;

import com.splicemachine.access.HConfiguration;
import com.splicemachine.access.api.SConfiguration;
import com.splicemachine.si.api.SIConfigurations;
import com.splicemachine.utils.SpliceLogUtils;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.splicemachine.si.constants.SIConstants.*;


/**
 * ConnectionFactory for the HBase architecture.
 * <p/>
 * Created by jleach on 11/18/15.
 */
@ThreadSafe
public class HBaseConnectionFactory{
    private static final Logger LOG=Logger.getLogger(HBaseConnectionFactory.class);
    private static volatile HBaseConnectionFactory INSTANCE;
    private final Connection connection;
    private final SConfiguration config;
    private final String namespace;
    private final byte[] namespaceBytes;

    private HBaseConnectionFactory(SConfiguration configuration){
        this.config=configuration;
        this.namespace=configuration.getString(HConfiguration.NAMESPACE);
        this.namespaceBytes=Bytes.toBytes(namespace);
        try{
            this.connection=ConnectionFactory.createConnection(((HConfiguration)configuration).unwrapDelegate());
        }catch(IOException ioe){
            throw new RuntimeException(ioe);
        }
    }

    public static HBaseConnectionFactory getInstance(SConfiguration config){
        HBaseConnectionFactory hbcf = INSTANCE;
        if(hbcf==null){
            synchronized(HBaseConnectionFactory.class){
                hbcf = INSTANCE;
                if(hbcf==null)
                    hbcf = INSTANCE = new HBaseConnectionFactory(config);
            }
        }
        return hbcf;

    }


    public Connection getConnection() throws IOException{
        return connection;
    }

    public Admin getAdmin() throws IOException{
        return connection.getAdmin();
    }

    /**
     * Returns list of active region server names
     */
    public List<ServerName> getServers() throws SQLException{
        Admin admin=null;
        List<ServerName> servers=null;
        try{
            admin=getAdmin();
            try{
                servers=new ArrayList<>(admin.getClusterStatus().getServers());
            }catch(IOException e){
                throw new SQLException(e);
            }

        }catch(IOException ioe){
            throw new SQLException(ioe);
        }finally{
            if(admin!=null)
                try{
                    admin.close();
                }catch(IOException e){
                    // ignore
                }
        }
        return servers;
    }

    /**
     * Returns master server name
     */
    public ServerName getMasterServer() throws SQLException{
        try(Admin admin=getAdmin()){
            try{
                return admin.getClusterStatus().getMaster();
            }catch(IOException e){
                throw new SQLException(e);
            }
        }catch(IOException ioe){
            throw new SQLException(ioe);
        }
    }

    public static void deleteTable(HBaseAdmin admin,HTableDescriptor table) throws IOException{
        deleteTable(admin,table.getName());
    }

    public static void deleteTable(HBaseAdmin admin,long conglomerateID) throws IOException{
        deleteTable(admin,Bytes.toBytes(Long.toString(conglomerateID)));
    }

    public static void deleteTable(HBaseAdmin admin,byte[] id) throws IOException{
        admin.disableTable(id);
        admin.deleteTable(id);
    }

    public HTableDescriptor generateDefaultSIGovernedTable(String tableName){
        HTableDescriptor desc=new HTableDescriptor(TableName.valueOf(namespace,tableName));
        desc.addFamily(createDataFamily());
        return desc;
    }

    public HTableDescriptor generateNonSITable(String tableName){
        HTableDescriptor desc=new HTableDescriptor(TableName.valueOf(namespace,tableName));
        desc.addFamily(createDataFamily());
        return desc;
    }

    public HTableDescriptor generateTransactionTable(){
        HTableDescriptor desc=new HTableDescriptor(TableName.valueOf(namespaceBytes,HConfiguration.TRANSACTION_TABLE_BYTES));
        HColumnDescriptor columnDescriptor=new HColumnDescriptor(DEFAULT_FAMILY_BYTES);
        columnDescriptor.setMaxVersions(5);
        Compression.Algorithm compress=Compression.Algorithm.valueOf(config.getString(HConfiguration.COMPRESSION_ALGORITHM));
        columnDescriptor.setCompressionType(compress);
        columnDescriptor.setInMemory(HConfiguration.DEFAULT_IN_MEMORY);
        columnDescriptor.setBlockCacheEnabled(HConfiguration.DEFAULT_BLOCKCACHE);
        columnDescriptor.setBloomFilterType(BloomType.valueOf(HConfiguration.DEFAULT_BLOOMFILTER.toUpperCase()));
        columnDescriptor.setTimeToLive(HConfiguration.DEFAULT_TTL);
        desc.addFamily(columnDescriptor);
        desc.addFamily(new HColumnDescriptor(Bytes.toBytes(SI_PERMISSION_FAMILY)));
        return desc;
    }

    public static byte[][] generateTransactionSplits(){
        byte[][] result=new byte[TRANSACTION_TABLE_BUCKET_COUNT-1][];
        for(int i=0;i<result.length;i++){
            result[i]=new byte[]{(byte)(i+1)};
        }
        return result;
    }

    public HColumnDescriptor createDataFamily(){
        HColumnDescriptor snapshot=new HColumnDescriptor(DEFAULT_FAMILY_BYTES);
        snapshot.setMaxVersions(Integer.MAX_VALUE);
        Compression.Algorithm compress=Compression.Algorithm.valueOf(config.getString(HConfiguration.COMPRESSION_ALGORITHM));
        snapshot.setCompressionType(compress);
        snapshot.setInMemory(HConfiguration.DEFAULT_IN_MEMORY);
        snapshot.setBlockCacheEnabled(HConfiguration.DEFAULT_BLOCKCACHE);
        snapshot.setBloomFilterType(BloomType.ROW);
        snapshot.setTimeToLive(HConfiguration.DEFAULT_TTL);
        return snapshot;
    }

    public boolean createSpliceHBaseTables(){
        SpliceLogUtils.info(LOG,"Creating Splice Required HBase Tables");

        try(Admin admin=connection.getAdmin()){
            admin.createNamespace(NamespaceDescriptor.create("splice").build());

            if(!admin.tableExists(TableName.valueOf(namespace,HConfiguration.TRANSACTION_TABLE))){
                HTableDescriptor td=generateTransactionTable();
                admin.createTable(td,generateTransactionSplits());
                SpliceLogUtils.info(LOG,HConfiguration.TRANSACTION_TABLE+" created");
            }
            if(!admin.tableExists(TableName.valueOf(namespace,HConfiguration.TENTATIVE_TABLE))){
                HTableDescriptor td=generateDefaultSIGovernedTable(HConfiguration.TENTATIVE_TABLE);
                admin.createTable(td);
                SpliceLogUtils.info(LOG,HConfiguration.TENTATIVE_TABLE+" created");
            }

            if(!admin.tableExists(TableName.valueOf(namespaceBytes,SIConfigurations.CONGLOMERATE_TABLE_NAME_BYTES))){
                HTableDescriptor td=generateDefaultSIGovernedTable(SIConfigurations.CONGLOMERATE_TABLE_NAME);
                admin.createTable(td);
                SpliceLogUtils.info(LOG,SIConfigurations.CONGLOMERATE_TABLE_NAME+" created");
            }

			/*
             * We have to have a special table to hold our Sequence values,
			 * because we shouldn't manage sequential generators
			 * transactionally.
			 */
            if(!admin.tableExists(TableName.valueOf(namespace,HConfiguration.SEQUENCE_TABLE_NAME))){
                HTableDescriptor td=generateNonSITable(HConfiguration.SEQUENCE_TABLE_NAME);
                admin.createTable(td);
                SpliceLogUtils.info(LOG,
                        com.splicemachine.si.constants.SIConstants.SEQUENCE_TABLE_NAME+" created");
            }

            createRestoreTableIfNecessary();
            return true;
        }catch(Exception e){
            SpliceLogUtils.error(LOG,"Unable to set up HBase Tables",e);
            return false;
        }
    }

    public void createRestoreTableIfNecessary(){
        try(Admin admin=connection.getAdmin()){
            if(!admin.tableExists(TableName.valueOf(namespace,HConfiguration.RESTORE_TABLE_NAME))){
                HTableDescriptor td=generateNonSITable(HConfiguration.RESTORE_TABLE_NAME);
                admin.createTable(td);
                SpliceLogUtils.info(LOG,HConfiguration.RESTORE_TABLE_NAME+" created");
            }
        }catch(Exception e){
            SpliceLogUtils.error(LOG,"Unable to set up HBase Tables",e);
        }
    }

    public static String escape(String first){
        // escape single quotes | compress multiple whitespace chars into one,
        // (replacing tab, newline, etc)
        return first.replaceAll("\\'","\\'\\'").replaceAll("\\s+"," ");
    }

}
