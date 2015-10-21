package com.splicemachine.derby.stream.utils;

import com.splicemachine.db.iapi.sql.Activation;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.db.iapi.types.RowLocation;
import com.splicemachine.derby.iapi.sql.execute.SpliceOperation;
import com.splicemachine.derby.stream.control.HregionDataSetProcessor;
import com.splicemachine.derby.stream.iapi.DataSetProcessor;
import com.splicemachine.derby.stream.control.ControlDataSetProcessor;
import com.splicemachine.derby.stream.spark.SparkDataSetProcessor;
import org.apache.log4j.Logger;

/**
 * Created by jleach on 4/16/15.
 */
public class StreamUtils {

    private static final Logger LOG = Logger.getLogger(StreamUtils.class);
    public static final DataSetProcessor controlDataSetProcessor = new ControlDataSetProcessor();
    public static final DataSetProcessor sparkDataSetProcessor = new SparkDataSetProcessor();
    public static final DataSetProcessor hregionDataSetProcessor = new HregionDataSetProcessor();
    private static final double CONTROL_SIDE_THRESHOLD = 1E20; // based on a TPCC1000 run on an 8 node cluster
    private static final double CONTROL_SIDE_ROWCOUNT_THRESHOLD = 1E6;

    public static DataSetProcessor getControlDataSetProcessor() {
        return controlDataSetProcessor;
    }

    public static DataSetProcessor getSparkDataSetProcessor() {
        return sparkDataSetProcessor;
    }

    public static <Op extends SpliceOperation> DataSetProcessor getDataSetProcessorFromActivation(Activation activation, SpliceOperation op) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("op estimated cost " + op.getEstimatedCost());
            if (activation.getResultSet() != null) {
               LOG.trace("resultset estimated cost " + ((SpliceOperation) activation.getResultSet()).getEstimatedCost());
            }
        }
        double estimatedCost = activation.getResultSet() == null ? op.getEstimatedCost() : ((SpliceOperation)activation.getResultSet()).getEstimatedCost();
        double estimatedRowCount = activation.getResultSet() == null ? op.getEstimatedRowCount() : ((SpliceOperation)activation.getResultSet()).getEstimatedRowCount();
        return (estimatedCost > CONTROL_SIDE_THRESHOLD || estimatedRowCount > CONTROL_SIDE_ROWCOUNT_THRESHOLD) ?sparkDataSetProcessor:controlDataSetProcessor;
    }

    public static <Op extends SpliceOperation> DataSetProcessor getLocalDataSetProcessorFromActivation(Activation activation, SpliceOperation op) {
        LOG.warn("op estimated cost " + op.getEstimatedCost());
        if (activation.getResultSet() != null) {
            LOG.warn("resultset estimated cost " + ((SpliceOperation) activation.getResultSet()).getEstimatedCost());
        }
        double estimatedCost = activation.getResultSet() == null ? op.getEstimatedCost() : ((SpliceOperation)activation.getResultSet()).getEstimatedCost();

        if (estimatedCost > CONTROL_SIDE_THRESHOLD) {
            return hregionDataSetProcessor;
        } else {
            return controlDataSetProcessor;
        }
    }

    public static DataSetProcessor getDataSetProcessor() {
        //  System.out.println("activation rowCount=%d, estimatedCost=%d" + activation.getMaxRows());
        //  System.out.println("activation2" + ((SpliceOperation) activation.getResultSet()).getEstimatedRowCount());
        //  System.out.println("activation3" + ((SpliceOperation)activation.getResultSet()).getEstimatedCost());
//        if ( (activation.getResultSet() == null && op.getEstimatedCost() > 40000.00)  || ((SpliceOperation)activation.getResultSet()).getEstimatedCost() > 40000.00) {
//            return sparkDataSetProcessor;
        //       }
        return controlDataSetProcessor;
    }


}