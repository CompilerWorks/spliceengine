
package com.splicemachine.derby.impl.store.access.btree;

import com.google.common.io.Closeables;
import com.splicemachine.access.api.PartitionFactory;import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.io.FormatableBitSet;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.db.iapi.store.access.ConglomerateController;
import com.splicemachine.db.iapi.store.raw.Transaction;
import com.splicemachine.db.iapi.types.DataValueDescriptor;
import com.splicemachine.db.iapi.types.RowLocation;
import com.splicemachine.db.impl.sql.execute.ValueRow;
import com.splicemachine.derby.impl.store.access.base.OpenSpliceConglomerate;
import com.splicemachine.derby.impl.store.access.base.SpliceController;
import com.splicemachine.derby.utils.DerbyBytesUtil;
import com.splicemachine.derby.utils.SpliceUtils;
import com.splicemachine.derby.utils.marshall.BareKeyHash;
import com.splicemachine.derby.utils.marshall.KeyHashDecoder;
import com.splicemachine.derby.utils.marshall.dvd.DescriptorSerializer;
import com.splicemachine.derby.utils.marshall.dvd.VersionedSerializers;
import com.splicemachine.pipeline.Exceptions;
import com.splicemachine.si.api.data.TxnOperationFactory;
import com.splicemachine.storage.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;


public class IndexController extends SpliceController{
    private static Logger LOG=Logger.getLogger(IndexController.class);
    private int nKeyFields;

    public IndexController(OpenSpliceConglomerate openSpliceConglomerate,
                           Transaction trans,
                           PartitionFactory partitionFactory,
                           TxnOperationFactory txnOperationFactory,
                           int nKeyFields){
        super(openSpliceConglomerate,trans,partitionFactory,txnOperationFactory);
        this.nKeyFields=nKeyFields;
    }

    private byte[] generateIndexKey(DataValueDescriptor[] row,boolean[] order) throws IOException, StandardException{
        if(row.length==nKeyFields){
            return DerbyBytesUtil.generateIndexKey(row,order,"1.0",false);
        }
        DataValueDescriptor[] uniqueRow=new DataValueDescriptor[nKeyFields];
        System.arraycopy(row,0,uniqueRow,0,nKeyFields);
        return DerbyBytesUtil.generateIndexKey(uniqueRow,order,"1.0",false);
    }

    public int nKeyFields(){
        return nKeyFields;
    }

    @Override
    public int insert(DataValueDescriptor[] row) throws StandardException{
        if(LOG.isTraceEnabled())
            LOG.trace(String.format("insert row into conglomerate: %s, row: %s",this.getConglomerate(),(row==null?null:Arrays.toString(row))));
        try(Partition htable = getTable()){
            boolean[] order=((IndexConglomerate)this.openSpliceConglomerate.getConglomerate()).getAscDescInfo();
            byte[] rowKey=generateIndexKey(row,order);
                        /*
						 * Check if the rowKey already exists.
						 * TODO: An optimization would be to not check for existence of a rowKey if the index is non-unique.
						 *		 Unfortunately, this information is not available here and would need to be passed down from
						 *		 DataDictionaryImpl through TabInfoImpl.  Something worth looking into in the future.
						 */
            DataGet get=opFactory.newDataGet(trans.getTxnInformation(),rowKey,null);
            DataResult result=htable.get(get,null);
            if(result==null||result.size()<=0){
                DataPut put=opFactory.newDataPut(trans.getTxnInformation(),rowKey);//SpliceUtils.createPut(rowKey,((SpliceTransaction)trans).getTxn());
                encodeRow(row,put,null,null);
                htable.put(put);
                return 0;
            }else{
                return ConglomerateController.ROWISDUPLICATE;
            }
        }catch(Exception e){
            LOG.error(e.getMessage(),e);
            throw Exceptions.parseException(e);
        }
    }

    @Override
    public void insertAndFetchLocation(DataValueDescriptor[] row,RowLocation destRowLocation) throws StandardException{
        if(LOG.isTraceEnabled())
            LOG.trace(String.format("insertAndFetchLocation into conglomerate: %s, row: %s, rowLocation: %s",this.getConglomerate(),(row==null?null:Arrays.toString(row)),destRowLocation));
        try(Partition htable = getTable()){
            boolean[] order=((IndexConglomerate)this.openSpliceConglomerate.getConglomerate()).getAscDescInfo();
            byte[] rowKey=generateIndexKey(row,order);
            DataPut put=opFactory.newDataPut(trans.getTxnInformation(),rowKey);//SpliceUtils.createPut(rowKey,((SpliceTransaction)trans).getTxn());
            encodeRow(row,put,null,null);

            destRowLocation.setValue(put.key());
            htable.put(put);
        }catch(Exception e){
            throw StandardException.newException("insert and fetch location error",e);
        }
    }

    @Override
    public boolean replace(RowLocation loc,DataValueDescriptor[] row,FormatableBitSet validColumns) throws StandardException{
        if(LOG.isTraceEnabled())
            LOG.trace(String.format("replace conglomerate: %s, rowlocation: %s, destRow: %s, validColumns: %s",this.getConglomerate(),loc,(row==null?null:Arrays.toString(row)),validColumns));
        try(Partition htable = getTable()){
            boolean[] sortOrder=((IndexConglomerate)this.openSpliceConglomerate.getConglomerate()).getAscDescInfo();
            DataPut put;
            int[] validCols;
            if(openSpliceConglomerate.cloneRowTemplate().length==row.length && validColumns==null){
                put=opFactory.newDataPut(trans.getTxnInformation(),DerbyBytesUtil.generateIndexKey(row,sortOrder,"1.0",false));
                validCols=null;
            }else{
                DataValueDescriptor[] oldValues=openSpliceConglomerate.cloneRowTemplate();
                DataGet get=opFactory.newDataGet(trans.getTxnInformation(),loc.getBytes(),null);
                get = createGet(get,oldValues,null);
                DataResult result=htable.get(get,null);
                ExecRow execRow=new ValueRow(oldValues.length);
                execRow.setRowArray(oldValues);
                DescriptorSerializer[] serializers=VersionedSerializers.forVersion("1.0",true).getSerializers(execRow);
                KeyHashDecoder decoder=BareKeyHash.decoder(null,null,serializers);
                try{
                    DataCell kv=result.userData();
                    decoder.set(kv.valueArray(),
                                kv.valueOffset(),
                            kv.valueLength());
                    decoder.decode(execRow);
                    validCols=new int[validColumns.getNumBitsSet()];
                    int pos=0;
                    for(int i=validColumns.anySetBit();i!=-1;i=validColumns.anySetBit(i)){
                        oldValues[i]=row[i];
                        validCols[pos]=i;
                    }
                    byte[] rowKey=generateIndexKey(row,sortOrder);
                    put=opFactory.newDataPut(trans.getTxnInformation(),rowKey);
                }finally{
                    try{decoder.close();}catch(IOException ignored){}
                }
            }

            encodeRow(row,put,validCols,validColumns);
            htable.put(put);
            super.delete(loc);
            return true;
        }catch(Exception e){
            throw StandardException.newException("Error during replace "+e);
        }
    }

    @Override
    public boolean isKeyed(){
        return true;
    }

}
