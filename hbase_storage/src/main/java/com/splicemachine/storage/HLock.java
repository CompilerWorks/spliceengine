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

package com.splicemachine.storage;

import com.splicemachine.si.data.HExceptionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.hadoop.hbase.regionserver.HRegion;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author Scott Fines
 *         Date: 12/15/15
 */
public class HLock implements Lock{
    private final byte[] key;

    private HRegion.RowLock delegate;
    private HRegion region;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",justification = "Intentional")
    public HLock(HRegion region,byte[] key){
        this.key = key;
        this.region = region;
    }

    @Override public void lock(){
        try{
            delegate = region.getRowLock(key,true);
        }catch(IOException e){
            throw new RuntimeException(HExceptionFactory.INSTANCE.processRemoteException(e));
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException{
        lock();
    }

    @Override
    public boolean tryLock(){
        try{
            delegate = region.getRowLock(key,false); // Null Lock Delegate means not run...
            return delegate!=null;
        }catch(IOException e){
            throw new RuntimeException(HExceptionFactory.INSTANCE.processRemoteException(e));
        }
    }

    @Override
    public boolean tryLock(long time,@Nonnull TimeUnit unit) throws InterruptedException{
        try{
            delegate = region.getRowLock(key,false);
            return true;
        }catch(IOException e){
            throw new RuntimeException(HExceptionFactory.INSTANCE.processRemoteException(e));
        }
    }

    @Override
    public void unlock(){
        if(delegate!=null) delegate.release();
    }

    @Override
    public @Nonnull Condition newCondition(){
        throw new UnsupportedOperationException("Cannot support conditions on an HLock");
    }
}
