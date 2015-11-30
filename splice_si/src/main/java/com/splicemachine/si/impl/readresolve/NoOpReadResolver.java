package com.splicemachine.si.impl.readresolve;

import com.splicemachine.si.api.ReadResolver;
import com.splicemachine.utils.ByteSlice;
import org.apache.hadoop.hbase.regionserver.HRegion;

/**
 * @author Scott Fines
 *         Date: 6/26/14
 */
public class NoOpReadResolver implements ReadResolver {
		public static final ReadResolver INSTANCE = new NoOpReadResolver();

    @Override public void resolve(ByteSlice rowKey, long txnId) {  }

    //no-ops
    @Override public void pauseResolution(){ }
    @Override public void resumeResolution(){ }
}