package com.splicemachine.derby.impl.sql.execute.operations.window.function;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.loader.ClassFactory;
import com.splicemachine.db.iapi.sql.execute.WindowFunction;
import com.splicemachine.db.iapi.types.DataTypeDescriptor;
import com.splicemachine.db.iapi.types.DataValueDescriptor;

/**
 * @author Jeff Cunningham
 *         Date: 9/30/15
 */
public class LastValueFunction extends SpliceGenericWindowFunction {
    // TODO: JC - implementing FIRST_VALUE is trivial.
    boolean lastValue;

    public WindowFunction setup(ClassFactory cf, String aggregateName, DataTypeDescriptor returnType ) {
        super.setup( cf, aggregateName, returnType );
        lastValue = aggregateName.equals("LAST_VALUE");
        return this;
    }

    @Override
    public void accumulate(DataValueDescriptor[] valueDescriptors) throws StandardException {
        this.add(valueDescriptors);
    }

    @Override
    protected void calculateOnAdd(WindowChunk chunk, DataValueDescriptor[] dvds) throws StandardException {
        DataValueDescriptor result = chunk.getResult();
        if (result == null || result.isNull()) {
            chunk.setResult(dvds[0].cloneValue(false));
        } else {
            DataValueDescriptor input = dvds[0];
            if (input != null && !input.isNull()) {
                chunk.setResult(input);
            }
        }
    }

    @Override
    protected void calculateOnRemove(WindowChunk chunk, DataValueDescriptor[] dvds) throws StandardException {
       // nothing to do here we've already set all the results. For LAST_VALUE(), result is just the last item we added.
    }

    @Override
    public DataValueDescriptor getResult() throws StandardException {
        // for LAST_VALUE(), we just get the last result. For FIRST_VALUE(), it's the first.
        WindowChunk last = chunks.get(chunks.size()-1);
        return last.getResult();
    }

    @Override
    public WindowFunction newWindowFunction() {
        return new LastValueFunction();
    }
}
