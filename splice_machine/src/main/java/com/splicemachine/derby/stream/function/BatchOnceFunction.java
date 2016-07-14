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

package com.splicemachine.derby.stream.function;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.sparkproject.guava.collect.ArrayListMultimap;
import org.sparkproject.guava.collect.Lists;
import org.sparkproject.guava.collect.Multimap;
import org.sparkproject.guava.collect.Sets;

import com.splicemachine.EngineDriver;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.db.iapi.types.DataValueDescriptor;
import com.splicemachine.db.iapi.types.SQLRef;
import com.splicemachine.db.impl.sql.execute.ValueRow;
import com.splicemachine.db.shared.common.reference.SQLState;
import com.splicemachine.derby.iapi.sql.execute.SpliceOperation;
import com.splicemachine.derby.impl.sql.execute.operations.LocatedRow;
import com.splicemachine.derby.impl.sql.execute.operations.batchonce.BatchOnceOperation;
import com.splicemachine.derby.stream.iapi.OperationContext;

/**
 *
 * Created by jyuan on 10/7/15.
 */
public class BatchOnceFunction<Op extends SpliceOperation>
        extends SpliceFlatMapFunction<Op,Iterator<LocatedRow>, LocatedRow> implements Serializable {

    private boolean initialized;
    private BatchOnceOperation op;
    private SpliceOperation subquerySource;
    private int sourceCorrelatedColumnPosition;
    private int subqueryCorrelatedColumnPosition;

    /* Collection of ExecRows we have read from the source and updated with results from the subquery but
     * not yet returned to the operation above us. */
    private Queue<LocatedRow> rowQueue;

    private final int batchSize;

    /* Constants for the ExecRow this operation emits. */
    private static final int OLD_COL = 1;
    private static final int NEW_COL = 2;
    private static final int ROW_LOC_COL = 3;

    public BatchOnceFunction() {
        batchSize = EngineDriver.driver().getConfiguration().getBatchOnceBatchSize();
    }

    public BatchOnceFunction (OperationContext<Op> operationContext) {
        super(operationContext);
        batchSize = EngineDriver.driver().getConfiguration().getBatchOnceBatchSize();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }

    @Override
    public Iterable<LocatedRow> call(Iterator<LocatedRow> locatedRows) throws Exception {

        if (!initialized) {
            init();
            initialized = true;
        }
        //pull a batch of rows
        if (rowQueue.size() == 0) {
            loadNextBatch(locatedRows);
        }
        return rowQueue;
    }

    private void init() {
        this.op = (BatchOnceOperation)getOperation();
        this.subquerySource = op.getSubquerySource();
        this.sourceCorrelatedColumnPosition = op.getSourceCorrelatedColumnPosition();
        this.subqueryCorrelatedColumnPosition = op.getSubqueryCorrelatedColumnPosition();
        this.rowQueue = Lists.newLinkedList();
    }

    private void loadNextBatch(Iterator<LocatedRow> locatedRows) throws StandardException{
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        //
        // STEP 1: Read batchSize rows from the source
        //
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // for quickly finding source rows with a given key
        Multimap<DataValueDescriptor, LocatedRow> sourceRowsMap = ArrayListMultimap.create(batchSize, 1);
        DataValueDescriptor nullValue = op.getSource().getExecRowDefinition().cloneColumn(1).getNewNull();

        LocatedRow sourceRow;
        ExecRow newRow;
        while (locatedRows.hasNext() && rowQueue.size() <batchSize) {
            sourceRow = locatedRows.next();
            DataValueDescriptor sourceKey = sourceRow.getRow().getColumn(sourceCorrelatedColumnPosition);
            DataValueDescriptor sourceOldValue = sourceRow.getRow().getColumn(sourceCorrelatedColumnPosition == 1 ? 2 : 1);

            newRow = new ValueRow(3);
            //
            // old value from source
            //
            newRow.setColumn(OLD_COL, sourceOldValue);
            //
            // new value will (possibly) come from subquery (subquery could return null, or return no row)
            //
            newRow.setColumn(NEW_COL, nullValue);
            //
            // row location
            //
            newRow.setColumn(ROW_LOC_COL, new SQLRef(sourceRow.getRowLocation()));

            LocatedRow newLocatedRow = new LocatedRow(sourceRow.getRowLocation(), newRow);
            sourceRowsMap.put(sourceKey, newLocatedRow);
        }

        /* Don't execute the subquery again if there were no more source rows. */
        if (sourceRowsMap.isEmpty()) {
            return;
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        //
        // STEP 2: Populate source row columns with values from subquery.  This will read the entire subquery
        // table every time.  Even if all rows for the batch are found quickly at the beginning of the subquery's scan
        // we must scan the entire table to throw LANG_SCALAR_SUBQUERY_CARDINALITY_VIOLATION if appropriate.
        //
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        try {
            subquerySource.openCore();
            Iterator<LocatedRow> subqueryIterator = subquerySource.getLocatedRowIterator();
            ExecRow nextRowCore;
            Set<DataValueDescriptor> uniqueKeySet = Sets.newHashSetWithExpectedSize(batchSize);
            while (subqueryIterator.hasNext()) {
                nextRowCore = subqueryIterator.next().getRow();
                DataValueDescriptor keyColumn = nextRowCore.getColumn(subqueryCorrelatedColumnPosition);
                Collection<LocatedRow> correspondingSourceRows = sourceRowsMap.get(keyColumn);
                for (LocatedRow correspondingSourceRow : correspondingSourceRows) {
                    correspondingSourceRow.getRow().setColumn(NEW_COL, nextRowCore.getColumn(subqueryCorrelatedColumnPosition == 1 ? 2 : 1));
                    rowQueue.add(correspondingSourceRow);
                }
                if (!uniqueKeySet.add(keyColumn)) {
                    throw StandardException.newException(SQLState.LANG_SCALAR_SUBQUERY_CARDINALITY_VIOLATION);
                }
            }
        } finally {
            subquerySource.close();
        }
    }
}
