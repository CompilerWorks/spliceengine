package com.splicemachine.db.impl.sql.catalog;

import com.splicemachine.db.catalog.UUID;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.uuid.UUIDFactory;
import com.splicemachine.db.iapi.sql.dictionary.*;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.db.iapi.sql.execute.ExecutionFactory;
import com.splicemachine.db.iapi.types.*;
import com.splicemachine.db.shared.common.sanity.SanityManager;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Properties;

/**
 * @author Scott Fines
 *         Date: 2/25/15
 */
public class SYSPARTITIONSTATISTICSRowFactory extends CatalogRowFactory {
    public static final String TABLENAME_STRING = "SYSPARTITIONSTATS";
    public static final int SYSPARTITIONSTATISTICS_COLUMN_COUNT = 8;
    public static final int CONGLOMID = 1;
    public static final int PARTITIONID = 2;
    public static final int TIMESTAMP = 3;
    public static final int STALENESS = 4;
    public static final int INPROGRESS = 5;
    public static final int ROWCOUNT = 6;
    public static final int PARTITION_SIZE = 7;
    public static final int MEANROWWIDTH= 8;

    protected static final int		SYSTABLESTATISTICS_INDEX1_ID = 0;
    protected static final int		SYSTABLESTATISTICS_INDEX2_ID = 1;
    protected static final int		SYSTABLESTATISTICS_INDEX3_ID = 2;


    private String[] uuids = {
            "08264012-014b-c29b-a826-000003009390",
            "0826401a-014b-c29b-a826-000003009390",
            "08264014-014b-c29b-a826-000003009390",
            "08264016-014b-c29b-a826-000003009390",
            "08264018-014b-c29b-a826-000003009390"
    };

    private	static	final	boolean[]	uniqueness = {
            true,
            false,
            false
    };

    private static final int[][] indexColumnPositions = {
                    {CONGLOMID, PARTITIONID,TIMESTAMP},
                    {CONGLOMID, PARTITIONID},
                    {CONGLOMID},
            };

    public SYSPARTITIONSTATISTICSRowFactory(UUIDFactory uuidf, ExecutionFactory ef, DataValueFactory dvf) {
        super(uuidf, ef, dvf);
        initInfo(SYSPARTITIONSTATISTICS_COLUMN_COUNT,TABLENAME_STRING,indexColumnPositions,uniqueness,uuids);
    }

    @Override
    public ExecRow makeRow(TupleDescriptor td, TupleDescriptor parent) throws StandardException {
        long conglomId = 0;
        String partitionId = null;
        long timestamp = 0;
        boolean staleness = false;
        boolean inProgress = false;
        long rowCount = 0;
        long partitionSize = 0;
        int meanRowWidth=0;

        if(td!=null){
            PartitionStatisticsDescriptor tsd = (PartitionStatisticsDescriptor)td;
            conglomId = tsd.getConglomerateId();
            partitionId = tsd.getPartitionId();
            timestamp  = tsd.getTimestamp();
            staleness = tsd.isStale();
            inProgress= tsd.isInProgress();
            rowCount = tsd.getRowCount();
            partitionSize = tsd.getPartitionSize();
            meanRowWidth = tsd.getMeanRowWidth();
        }

        ExecRow row = getExecutionFactory().getValueRow(SYSPARTITIONSTATISTICS_COLUMN_COUNT);
        row.setColumn(CONGLOMID,new SQLLongint(conglomId));
        row.setColumn(PARTITIONID,new SQLVarchar(partitionId));
        row.setColumn(TIMESTAMP,new SQLTimestamp(new Timestamp(timestamp)));
        row.setColumn(STALENESS,new SQLBoolean(staleness));
        row.setColumn(INPROGRESS,new SQLBoolean(inProgress));
        row.setColumn(ROWCOUNT,new SQLLongint(rowCount));
        row.setColumn(PARTITION_SIZE,new SQLLongint(partitionSize));
        row.setColumn(MEANROWWIDTH,new SQLInteger(meanRowWidth));
        return row;
    }

    @Override
    public TupleDescriptor buildDescriptor(ExecRow row, TupleDescriptor parentTuple, DataDictionary dataDictionary) throws StandardException {
        if (SanityManager.DEBUG) {
            SanityManager.ASSERT( row.nColumns() == SYSPARTITIONSTATISTICS_COLUMN_COUNT,
                    "Wrong number of columns for a STATEMENTHISTORY row");
        }

        DataValueDescriptor col = row.getColumn(CONGLOMID);
        long conglomId = col.getLong();
        col = row.getColumn(PARTITIONID);
        String partitionId = col.getString();
        col = row.getColumn(TIMESTAMP);
        Timestamp timestamp = col.getTimestamp(null);
        col = row.getColumn(STALENESS);
        boolean isStale = col.getBoolean();
        col = row.getColumn(INPROGRESS);
        boolean inProgress = col.getBoolean();
        col = row.getColumn(ROWCOUNT);
        long rowCount = col.getLong();
        col = row.getColumn(PARTITION_SIZE);
        long partitionSize = col.getLong();
        col = row.getColumn(MEANROWWIDTH);
        int rowWidth = col.getInt();

        return new PartitionStatisticsDescriptor(conglomId,
                partitionId,
                timestamp.getTime(),
                isStale,
                inProgress,
                rowCount,
                partitionSize,
                rowWidth);
    }

    @Override
    public SystemColumn[] buildColumnList() throws StandardException {
        return new SystemColumn[]{
                SystemColumnImpl.getColumn("CONGLOMERATEID", Types.BIGINT,false),
                SystemColumnImpl.getColumn("PARTITIONID",Types.VARCHAR,false),
                SystemColumnImpl.getColumn("LAST_UPDATED",Types.TIMESTAMP,false),
                SystemColumnImpl.getColumn("IS_STALE",Types.BOOLEAN,false),
                SystemColumnImpl.getColumn("IN_PROGRESS", Types.BOOLEAN, false),
                SystemColumnImpl.getColumn("ROWCOUNT",Types.BIGINT,true),
                SystemColumnImpl.getColumn("PARTITION_SIZE",Types.BIGINT,true),
                SystemColumnImpl.getColumn("MEANROWWIDTH",Types.INTEGER,true)
        };
    }

    @Override
    public Properties getCreateHeapProperties() {
        return super.getCreateHeapProperties();
    }

    public static ColumnDescriptor[] getViewColumns(TableDescriptor view,UUID viewId) throws StandardException {
        DataTypeDescriptor varcharType = DataTypeDescriptor.getBuiltInDataTypeDescriptor(Types.VARCHAR);
        DataTypeDescriptor longType = DataTypeDescriptor.getBuiltInDataTypeDescriptor(Types.BIGINT);
        return new ColumnDescriptor[]{
                new ColumnDescriptor("SCHEMANAME"               ,1,1,varcharType,null,null,view,viewId,0,0,0),
                new ColumnDescriptor("TABLENAME"                ,2,2,varcharType,null,null,view,viewId,0,0,1),
                new ColumnDescriptor("CONGLOMERATENAME"         ,3,3,varcharType,null,null,view,viewId,0,0,2),
                new ColumnDescriptor("TOTAL_ROW_COUNT"          ,4,4,longType,null,null,view,viewId,0,0,3),
                new ColumnDescriptor("AVG_ROW_COUNT"            ,5,5,longType,null,null,view,viewId,0,0,4),
                new ColumnDescriptor("TOTAL_SIZE"               ,6,6,longType,null,null,view,viewId,0,0,5),
                new ColumnDescriptor("NUM_PARTITIONS"           ,7,7,longType,null,null,view,viewId,0,0,6),
                new ColumnDescriptor("AVG_PARTITION_SIZE"       ,8,8,longType,null,null,view,viewId,0,0,7),
                new ColumnDescriptor("ROW_WIDTH"                ,9,9,longType,null,null,view,viewId,0,0,8)
        };
    }

    public static final String STATS_VIEW_SQL = "create view systablestatistics as select " +
            "s.schemaname" +
            ",t.tablename" + // 1
            ",c.conglomeratename" + //2
            ",sum(ts.rowCount) as TOTAL_ROW_COUNT" +  //3
            ",avg(ts.rowCount) as AVG_ROW_COUNT" +      //4
            ",sum(ts.partition_size) as TOTAL_SIZE" + //5
            ",count(ts.rowCount) as NUM_PARTITIONS" + //6
            ",avg(ts.partition_size) as AVG_PARTITION_SIZE" + //7
            ",max(ts.meanrowWidth) as ROW_WIDTH" + //8
            " from " +
            "sys.systables t" +
            ",sys.sysschemas s" +
            ",sys.sysconglomerates c" +
            ",sys.syspartitionstats ts" +
            " where " +
            "t.tableid = c.tableid " +
            "and c.conglomeratenumber = ts.conglomerateid " +
            "and t.schemaid = s.schemaid " +
            " group by " +
            "s.schemaname" +
            ",t.tablename"+
            ",c.conglomeratename";

    public static void main(String...args) {
        System.out.println(STATS_VIEW_SQL);
    }




}
