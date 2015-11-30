package com.splicemachine.pipeline.writeconfiguration;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.ObjectObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.splicemachine.constants.SpliceConstants;
import com.splicemachine.hbase.KVPair;
import com.splicemachine.metrics.MetricFactory;
import com.splicemachine.metrics.Metrics;
import com.splicemachine.pipeline.api.Code;
import com.splicemachine.pipeline.api.WriteContext;
import com.splicemachine.pipeline.api.WriteResponse;
import com.splicemachine.pipeline.impl.BulkWrite;
import com.splicemachine.pipeline.impl.BulkWriteResult;
import com.splicemachine.pipeline.impl.WriteResult;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexSharedWriteConfiguration extends BaseWriteConfiguration {

    private static final Logger LOG = Logger.getLogger(IndexSharedWriteConfiguration.class);

    private final List<Pair<WriteContext, ObjectObjectOpenHashMap<KVPair, KVPair>>> sharedMainMutationList = new CopyOnWriteArrayList<>();
    private final AtomicInteger completedCount = new AtomicInteger(0);

    @Override
    public void registerContext(WriteContext context, ObjectObjectOpenHashMap<KVPair, KVPair> indexToMainMutationMap) {
        sharedMainMutationList.add(Pair.newPair(context, indexToMainMutationMap));
        completedCount.incrementAndGet();
    }

    @Override
    public int getMaximumRetries() {
        return SpliceConstants.numRetries;
    }

    @Override
    public WriteResponse partialFailure(BulkWriteResult result, BulkWrite request) throws ExecutionException {
        try {
            IntObjectOpenHashMap<WriteResult> failedRows = result.getFailedRows();
            boolean canRetry = true;
            boolean regionTooBusy = false;
            for (IntObjectCursor<WriteResult> cursor : failedRows) {
                if (!cursor.value.canRetry()) {
                    canRetry = false;
                    break;
                }
                if (cursor.value.getCode() == Code.REGION_TOO_BUSY)
                    regionTooBusy = true;
            }

            if (regionTooBusy) {
                try {
                    Thread.sleep(2 * getPause());
                } catch (InterruptedException e) {
                    LOG.info("Interrupted while waiting due to a RegionTooBusyException", e);
                }
                return WriteResponse.RETRY;
            }
            if (canRetry) {
                return WriteResponse.RETRY;
            }
            else {
                List<KVPair> indexMutations = request.mutationsList();
                for (IntObjectCursor<WriteResult> cursor : failedRows) {
                    int row = cursor.key;
                    KVPair kvPair = indexMutations.get(row);
                    WriteResult mutationResult = cursor.value;
                    for (Pair<WriteContext, ObjectObjectOpenHashMap<KVPair, KVPair>> pair : sharedMainMutationList) {
                        KVPair main = pair.getSecond().get(kvPair);
                        WriteContext context = pair.getFirst();
                        // The "main" kvPair from the context may not match the one from the context that failed.
                        // This can happen, for instance, when we have an index update - there's a delete ctx
                        // and an update ctx. Both fail but it may be the constraint check on the update that
                        // caused it.
                        // However, ALL ctxs must NOT be null.
                        assert context != null;
                        context.failed(main, mutationResult);
                    }
                }
                return WriteResponse.IGNORE;
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public long getPause() {
        return SpliceConstants.pause;
    }

    @Override
    public void writeComplete(long timeTakenMs, long numRecordsWritten) {
        int remaining = completedCount.decrementAndGet();
        if (remaining <= 0) {
            sharedMainMutationList.clear();
        }
    }

    @Override
    public MetricFactory getMetricFactory() {
        return Metrics.noOpMetricFactory();
    }

    @Override
    public String toString() {
        return "IndexSharedWriteConfiguration{}";
    }

}
