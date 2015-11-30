package com.splicemachine.pipeline.writehandler;

import com.carrotsearch.hppc.ObjectObjectOpenHashMap;
import com.splicemachine.hbase.KVPair;
import com.splicemachine.pipeline.api.PreFlushHook;
import com.splicemachine.pipeline.api.WriteContext;
import com.splicemachine.utils.SpliceLogUtils;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Attempt to share the call buffer between contexts using the context to lookup pre flush values...
 */
public class IndexSharedPreFlushHook implements PreFlushHook {

    private static final Logger LOG = Logger.getLogger(IndexSharedPreFlushHook.class);

    private List<Pair<WriteContext, ObjectObjectOpenHashMap<KVPair, KVPair>>> sharedMainMutationList = new ArrayList<>();

		@Override
		public Collection<KVPair> transform(Collection<KVPair> buffer) throws Exception {
				if (LOG.isTraceEnabled())
						SpliceLogUtils.trace(LOG, "transform buffer rows=%d",buffer.size());
				Collection<KVPair> newList = new ArrayList<>(buffer.size());
				for (KVPair indexPair:buffer) {
						for (Pair<WriteContext,ObjectObjectOpenHashMap<KVPair,KVPair>> pair: sharedMainMutationList) {
								KVPair base = pair.getSecond().get(indexPair);
								if (base != null) {
										if (pair.getFirst().canRun(base))
												newList.add(indexPair);
										break;
								}
						}
				}
				if (LOG.isTraceEnabled())
						SpliceLogUtils.trace(LOG, "transform returns buffer rows=%d",newList.size());
				return newList;
		}
    public void registerContext(WriteContext context, ObjectObjectOpenHashMap<KVPair, KVPair> indexToMainMutationMap) {
        sharedMainMutationList.add(Pair.newPair(context, indexToMainMutationMap));
    }

		public void cleanup() {
				sharedMainMutationList.clear();
				sharedMainMutationList = null;
		}

}