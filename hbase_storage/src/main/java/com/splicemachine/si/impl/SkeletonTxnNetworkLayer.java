/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.splicemachine.si.impl;

import com.carrotsearch.hppc.LongOpenHashSet;
import com.splicemachine.hbase.SpliceRpcController;
import com.splicemachine.si.coprocessor.TxnMessage;
import com.splicemachine.utils.SpliceLogUtils;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Scott Fines
 *         Date: 12/22/15
 */
public abstract class SkeletonTxnNetworkLayer implements TxnNetworkLayer{
    private static Logger LOG=Logger.getLogger(SkeletonTxnNetworkLayer.class);
    @Override
    public void beginTransaction(byte[] rowKey,TxnMessage.TxnInfo txnInfo) throws IOException{
        TxnMessage.TxnLifecycleService service=getLifecycleService(rowKey);

        SpliceRpcController controller=new SpliceRpcController();
        service.beginTransaction(controller,txnInfo,new BlockingRpcCallback<TxnMessage.VoidResponse>());
        dealWithError(controller);
    }


    @Override
    public TxnMessage.ActionResponse lifecycleAction(byte[] rowKey,TxnMessage.TxnLifecycleMessage lifecycleMessage) throws IOException{
        TxnMessage.TxnLifecycleService service=getLifecycleService(rowKey);
        SpliceRpcController controller=new SpliceRpcController();
        BlockingRpcCallback<TxnMessage.ActionResponse> done=new BlockingRpcCallback<>();
        service.lifecycleAction(controller,lifecycleMessage,done);
        dealWithError(controller);
        return done.get();
    }

    @Override
    public void elevate(byte[] rowKey,TxnMessage.ElevateRequest elevateRequest) throws IOException{
        TxnMessage.TxnLifecycleService service=getLifecycleService(rowKey);

        SpliceRpcController controller=new SpliceRpcController();
        service.elevateTransaction(controller,elevateRequest,new BlockingRpcCallback<TxnMessage.VoidResponse>());
        dealWithError(controller);
    }

    @Override
    public long[] getActiveTxnIds(final TxnMessage.ActiveTxnRequest request) throws IOException{
            Map<byte[], TxnMessage.ActiveTxnIdResponse> data=coprocessorService(TxnMessage.TxnLifecycleService.class,
                    HConstants.EMPTY_START_ROW,HConstants.EMPTY_END_ROW,new Batch.Call<TxnMessage.TxnLifecycleService, TxnMessage.ActiveTxnIdResponse>(){
                        @Override
                        public TxnMessage.ActiveTxnIdResponse call(TxnMessage.TxnLifecycleService instance) throws IOException{
                            SpliceRpcController controller=new SpliceRpcController();
                            BlockingRpcCallback<TxnMessage.ActiveTxnIdResponse> response=new BlockingRpcCallback<>();

                            instance.getActiveTransactionIds(controller,request,response);
                            dealWithError(controller);
                            return response.get();
                        }
                    });

            LongOpenHashSet txns=LongOpenHashSet.newInstance(); //TODO -sf- do we really need to check for duplicates? In case of Transaction table splits?
            for(TxnMessage.ActiveTxnIdResponse response : data.values()){
                int activeTxnIdsCount=response.getActiveTxnIdsCount();
                for(int i=0;i<activeTxnIdsCount;i++){
                    txns.add(response.getActiveTxnIds(i));
                }
            }
            long[] finalTxns=txns.toArray();
            Arrays.sort(finalTxns);
            return finalTxns;
    }


    @Override
    public Collection<TxnMessage.ActiveTxnResponse> getActiveTxns(final TxnMessage.ActiveTxnRequest request) throws IOException{
            Map<byte[], TxnMessage.ActiveTxnResponse> data=coprocessorService(TxnMessage.TxnLifecycleService.class,
                    HConstants.EMPTY_START_ROW,HConstants.EMPTY_END_ROW,new Batch.Call<TxnMessage.TxnLifecycleService, TxnMessage.ActiveTxnResponse>(){
                        @Override
                        public TxnMessage.ActiveTxnResponse call(TxnMessage.TxnLifecycleService instance) throws IOException{
                            SpliceRpcController controller=new SpliceRpcController();
                            BlockingRpcCallback<TxnMessage.ActiveTxnResponse> response=new BlockingRpcCallback<>();

                            instance.getActiveTransactions(controller,request,response);
                            dealWithError(controller);
                            return response.get();
                        }
                    });
        return data.values();
    }

    @Override
    public TxnMessage.Txn getTxn(byte[] rowKey,TxnMessage.TxnRequest request) throws IOException{
        TxnMessage.TxnLifecycleService service=getLifecycleService(rowKey);
        SpliceRpcController controller=new SpliceRpcController();
        BlockingRpcCallback<TxnMessage.Txn> done=new BlockingRpcCallback<>();
        service.getTransaction(controller,request,done);
        dealWithError(controller);
        return done.get();
    }

    protected abstract TxnMessage.TxnLifecycleService getLifecycleService(byte[] rowKey) throws IOException;

    protected abstract <C> Map<byte[],C> coprocessorService(Class<TxnMessage.TxnLifecycleService> txnLifecycleServiceClass,
                                                                                     byte[] startRow,
                                                                                     byte[] endRow,
                                                                                     Batch.Call<TxnMessage.TxnLifecycleService, C> call) throws IOException;

    /* ***************************************************************************************************************/
    /*private helper methods*/
    private void dealWithError(SpliceRpcController controller) throws IOException{
        if(!controller.failed()) return; //nothing to worry about
        SpliceLogUtils.error(LOG,controller.getThrowable());
        Throwable t=controller.getThrowable();
        if(t instanceof IOException)
            throw (IOException)t;
        else throw new IOException(t);
    }
}
