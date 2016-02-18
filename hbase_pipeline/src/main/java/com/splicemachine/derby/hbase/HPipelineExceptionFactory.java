package com.splicemachine.derby.hbase;

import com.google.common.base.Throwables;
import com.splicemachine.access.api.CallTimeoutException;
import com.splicemachine.access.api.NotServingPartitionException;
import com.splicemachine.access.api.WrongPartitionException;
import com.splicemachine.pipeline.*;
import com.splicemachine.pipeline.api.Code;
import com.splicemachine.pipeline.api.PipelineExceptionFactory;
import com.splicemachine.pipeline.api.PipelineTooBusy;
import com.splicemachine.pipeline.client.WriteResult;
import com.splicemachine.pipeline.constraint.ConstraintContext;
import com.splicemachine.pipeline.exception.IndexNotSetUpException;
import com.splicemachine.si.api.server.DisconnectException;
import com.splicemachine.si.api.server.FailedServerException;
import com.splicemachine.si.api.txn.lifecycle.CannotCommitException;
import com.splicemachine.si.data.HExceptionFactory;
import com.splicemachine.si.impl.HNotServingRegion;
import com.splicemachine.si.impl.HWriteConflict;
import com.splicemachine.si.impl.HWrongRegion;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * @author Scott Fines
 *         Date: 12/23/15
 */
public class HPipelineExceptionFactory extends HExceptionFactory implements PipelineExceptionFactory{
     public static final PipelineExceptionFactory INSTANCE=new HPipelineExceptionFactory();

    private HPipelineExceptionFactory(){
        super();
    }

    @Override
    public IOException primaryKeyViolation(ConstraintContext constraintContext){
        return new ConstraintViolation.PkViolation("",constraintContext);
    }

    @Override
    public IOException foreignKeyViolation(ConstraintContext constraintContext){
        return new ConstraintViolation.FkViolation("",constraintContext);
    }

    @Override
    public IOException uniqueViolation(ConstraintContext constraintContext){
        return new ConstraintViolation.UniqueViolation("",constraintContext);
    }

    @Override
    public IOException notNullViolation(ConstraintContext constraintContext){
        return new ConstraintViolation.NotNullViolation("",constraintContext);
    }


    @Override
    public Throwable processPipelineException(Throwable t){
        t = Throwables.getRootCause(t);
        if(t instanceof ConstraintViolation)
            return t;
        else if(t instanceof HWrongRegion)
            return t;
        else if(t instanceof HTooBusy)
            return t;
        else if(t instanceof IOException)
            return super.processRemoteException(t);
        else return super.processRemoteException(new IOException(t));
    }

    @Override
    public boolean needsTransactionalRetry(Throwable t){
        t=Throwables.getRootCause(t);
        t=processPipelineException(t);
        if(t instanceof CannotCommitException) return true;
        else if(t instanceof CallTimeoutException) return true;
        else if(t instanceof SocketTimeoutException) return true;
        else if(t instanceof DisconnectException) return true;
        else if(t instanceof FailedServerException) return true;
        else return false;
    }

    @Override
    public boolean canFinitelyRetry(Throwable t){
        t=Throwables.getRootCause(t);
        t=processPipelineException(t);
        if(t instanceof CallTimeoutException) return true;
        else if(t instanceof ConnectException) return true;
        else return false;
    }

    @Override
    public boolean canInfinitelyRetry(Throwable t){
        t=Throwables.getRootCause(t);
        t=processPipelineException(t);
        if(t instanceof NotServingPartitionException
                || t instanceof WrongPartitionException
                || t instanceof PipelineTooBusy
                || t instanceof IndexNotSetUpException) return true;
        return false;
    }

    @Override
    public Throwable processErrorResult(WriteResult result){
        Code writeErrorCode=result.getCode();

        if(writeErrorCode!=null){
            switch(writeErrorCode){
                case FAILED:
                    return new IOException(result.getErrorMessage());
                case WRITE_CONFLICT:
                    return HWriteConflict.fromString(result.getErrorMessage());
                case SUCCESS:
                    return null; //won't happen
                case PRIMARY_KEY_VIOLATION:
                    return primaryKeyViolation(result.getConstraintContext());
                case UNIQUE_VIOLATION:
                    return uniqueViolation(result.getConstraintContext());
                case FOREIGN_KEY_VIOLATION:
                    return foreignKeyViolation(result.getConstraintContext());
                case NOT_SERVING_REGION:
                    return new HNotServingRegion();
                case WRONG_REGION:
                    return new HWrongRegion();
                case REGION_TOO_BUSY:
                    return new HTooBusy();
                case NOT_RUN:
                    //won't happen
                    return new IOException("Unexpected NotRun code for an error");
            }
        }
        return doNotRetry(result.getErrorMessage());
    }
}
