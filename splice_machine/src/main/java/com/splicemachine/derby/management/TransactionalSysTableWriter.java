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

package com.splicemachine.derby.management;

import com.splicemachine.SqlExceptionFactory;
import com.splicemachine.access.api.PartitionFactory;
import com.splicemachine.db.iapi.services.context.ContextManager;
import com.splicemachine.db.iapi.services.context.ContextService;
import com.splicemachine.db.iapi.types.DataValueDescriptor;
import com.splicemachine.derby.jdbc.SpliceTransactionResourceImpl;
import com.splicemachine.derby.utils.marshall.dvd.DescriptorSerializer;
import com.splicemachine.derby.utils.marshall.DataHash;
import com.splicemachine.derby.utils.marshall.KeyHashDecoder;
import com.splicemachine.encoding.MultiFieldEncoder;
import com.splicemachine.kvpair.KVPair;
import com.splicemachine.pipeline.PipelineDriver;
import com.splicemachine.pipeline.callbuffer.RecordingCallBuffer;
import com.splicemachine.pipeline.client.WriteCoordinator;
import com.splicemachine.si.api.txn.TxnView;
import com.splicemachine.si.impl.driver.SIDriver;
import com.splicemachine.storage.DataResultScanner;
import com.splicemachine.storage.EntryDecoder;
import com.splicemachine.storage.EntryEncoder;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.sql.dictionary.SchemaDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.TableDescriptor;
import com.splicemachine.storage.Partition;
import com.splicemachine.utils.Pair;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Scott Fines
 *         Date: 9/11/14
 */
public abstract class TransactionalSysTableWriter<T> {

    private final String tableName;
    protected volatile String conglomIdString;

    protected DataResultScanner resultScanner = null;
    protected DataValueDescriptor[] dvds;
    protected DescriptorSerializer[] serializers;
    protected EntryDecoder entryDecoder;

    private final ThreadLocal<Pair<DataHash<T>,DataHash<T>>> hashLocals;
    private final SqlExceptionFactory exceptionFactory;

    protected TransactionalSysTableWriter(final String tableName,
                                          SqlExceptionFactory exceptionFactory) {
        this.tableName = tableName;
        this.exceptionFactory = exceptionFactory;
        this.hashLocals = new ThreadLocal<Pair<DataHash<T>, DataHash<T>>>(){
            @Override
            protected Pair<DataHash<T>, DataHash<T>> initialValue() {
                return Pair.newPair(getKeyHash(), getDataHash());
            }
        };
    }

    protected abstract DataHash<T> getDataHash();

    protected abstract DataHash<T> getKeyHash();

    public void report(T element,TxnView txn) throws IOException{
        if(!txn.allowsWrites())
            throw exceptionFactory.readOnlyModification("Cannot write data with a read-only transaction "+ txn.getTxnId());
        Pair<DataHash<T>,DataHash<T>> hashPair = hashLocals.get();
        DataHash<T> keyHash = hashPair.getFirst();
        DataHash<T> pairHash = hashPair.getSecond();

        keyHash.setRow(element);
        pairHash.setRow(element);

        String conglom = getConglomIdString(txn);
        try{
            KVPair toWrite=new KVPair(keyHash.encode(),pairHash.encode());
            writeEntry(txn,conglom,toWrite);
        }catch(Exception e){
            throw exceptionFactory.processRemoteException(e);
        }
    }

    protected void writeEntry(TxnView txn,String conglom,KVPair toWrite) throws Exception{
        WriteCoordinator tableWriter=PipelineDriver.driver().writeCoordinator();
        PartitionFactory pf=SIDriver.driver().getTableFactory();
        try(Partition p=pf.getTable(conglom)){
            try(RecordingCallBuffer<KVPair> callBuffer=tableWriter.synchronousWriteBuffer(p,txn)){
                callBuffer.add(toWrite);
                callBuffer.flushBufferAndWait();
            }
        }
    }

    public void remove(T element,TxnView txn) throws IOException{
        if(!txn.allowsWrites())
            throw exceptionFactory.readOnlyModification("Cannot write data with a read-only transaction "+ txn.getTxnId());
        Pair<DataHash<T>,DataHash<T>> hashPair = hashLocals.get();
        DataHash<T> keyHash = hashPair.getFirst();
        DataHash<T> pairHash = hashPair.getSecond();

        keyHash.setRow(element);
        pairHash.setRow(element);

        String conglom = getConglomIdString(txn);
        try{
            KVPair toWrite=new KVPair(keyHash.encode(),new byte[0],KVPair.Type.DELETE);
            writeEntry(txn,conglom,toWrite);
        }catch(Exception e){
            throw exceptionFactory.processRemoteException(e);
        }
    }

    public String getConglomIdString(TxnView txn) throws IOException {
        String conglom = conglomIdString;
        if(conglom==null){
            synchronized (this){
                conglom = conglomIdString;
                if(conglom==null){
                    try{
                    conglom = conglomIdString = fetchConglomId(txn);
                    }catch(SQLException | StandardException se){
                        throw exceptionFactory.processRemoteException(se);
                    }
                }
            }
        }
        return conglom;
    }

    private String fetchConglomId(TxnView txn) throws StandardException,SQLException {
        ContextManager currentCm = ContextService.getFactory().getCurrentContextManager();
        try {
            SpliceTransactionResourceImpl transactionResource = new SpliceTransactionResourceImpl();
            transactionResource.marshallTransaction(txn);

            LanguageConnectionContext lcc = transactionResource.getLcc();
            DataDictionary dd = lcc.getDataDictionary();
            SchemaDescriptor systemSchemaDescriptor = dd.getSystemSchemaDescriptor();
            TableDescriptor td = dd.getTableDescriptor(tableName, systemSchemaDescriptor, lcc.getTransactionExecute());
            return Long.toString(td.getHeapConglomerateId());
        }
        finally {
            if (currentCm != null)
                ContextService.getFactory().setCurrentContextManager(currentCm);
        }
    }

    protected static abstract class WriteableHash<T> implements DataHash<T> {
        protected T element;

        @Override public void setRow(T rowToEncode) { this.element = rowToEncode;	 }
        @Override public KeyHashDecoder getDecoder() { return null; }

        protected abstract void doEncode(MultiFieldEncoder encoder, T element);
    }

    protected static abstract class EntryWriteableHash<T> extends WriteableHash<T>{
        private EntryEncoder entryEncoder;

        @Override
        public final byte[] encode() throws StandardException, IOException {
            if(entryEncoder==null)
                entryEncoder = buildEncoder();

            MultiFieldEncoder fieldEncoder = entryEncoder.getEntryEncoder();
            fieldEncoder.reset();
            doEncode(fieldEncoder, element);
            return entryEncoder.encode();
        }

        protected abstract EntryEncoder buildEncoder();

        @Override
        public void close() throws IOException {
            if(entryEncoder!=null)
                entryEncoder.close();
        }
    }

    protected  static abstract class KeyWriteableHash<T> extends WriteableHash<T>{
        private MultiFieldEncoder entryEncoder;

        @Override
        public final byte[] encode() throws StandardException, IOException {
            if(entryEncoder==null)
                entryEncoder = MultiFieldEncoder.create(getNumFields());
            else
                entryEncoder.reset();

            doEncode(entryEncoder,element);
            return entryEncoder.build();
        }

        protected abstract int getNumFields();

        @Override
        public void close() throws IOException {

        }
    }
}
