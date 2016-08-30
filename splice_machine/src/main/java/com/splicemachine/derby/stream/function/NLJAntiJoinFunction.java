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

import com.splicemachine.EngineDriver;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.derby.iapi.sql.execute.SpliceOperation;
import com.splicemachine.derby.impl.sql.execute.operations.JoinUtils;
import com.splicemachine.derby.impl.sql.execute.operations.LocatedRow;
import com.splicemachine.derby.stream.iapi.DataSet;
import com.splicemachine.derby.stream.iapi.OperationContext;
import com.splicemachine.derby.stream.utils.StreamLogUtils;
import com.splicemachine.derby.stream.utils.StreamUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by jleach on 4/24/15.
 */
public class NLJAntiJoinFunction<Op extends SpliceOperation> extends SpliceJoinFlatMapFunction<Op, LocatedRow, LocatedRow> {


    public NLJAntiJoinFunction() {}

    public NLJAntiJoinFunction(OperationContext<Op> operationContext) {
        super(operationContext);
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
    public Iterator<LocatedRow> call(LocatedRow from) throws Exception {
        checkInit();
        DataSet dataSet = null;
        try {
            op.getRightOperation().openCore(EngineDriver.driver().processorFactory().localProcessor(null,op));
            Iterator<LocatedRow> rightSideNLJ = op.getRightOperation().getLocatedRowIterator();
            if (rightSideNLJ.hasNext()) {
                StreamLogUtils.logOperationRecordWithMessage(from, operationContext, "anti-join filtered");
                return Collections.<LocatedRow>emptyList().iterator();
            }
            ExecRow mergedRow = JoinUtils.getMergedRow(from.getRow(), op.getEmptyRow(),
                    op.wasRightOuterJoin, executionFactory.getValueRow(numberOfColumns));
            StreamLogUtils.logOperationRecordWithMessage(from, operationContext, "anti-join");
            op.setCurrentRow(mergedRow);
            op.setCurrentRowLocation(from.getRowLocation());
            return Collections.singletonList(new LocatedRow(from.getRowLocation(), mergedRow)).iterator();
        } finally {
            if (op.getRightOperation()!= null)
                op.getRightOperation().close();
        }

    }
}
