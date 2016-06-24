package com.splicemachine.pipeline.mem;

import com.splicemachine.pipeline.PipelineWriter;
import com.splicemachine.pipeline.api.*;
import com.splicemachine.pipeline.client.WriteCoordinator;
import com.splicemachine.pipeline.traffic.SpliceWriteControl;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

/**
 * @author Scott Fines
 *         Date: 12/23/15
 */
@ThreadSafe
public class DirectBulkWriterFactory implements BulkWriterFactory{
    private volatile PipelineWriter pipelineWriter;

    public DirectBulkWriterFactory(WritePipelineFactory wpf,
                                   SpliceWriteControl writeControl,
                                   PipelineExceptionFactory exceptionFactory,
                                   PipelineMeter meter) throws IOException{
        this.pipelineWriter = new PipelineWriter(exceptionFactory,wpf,writeControl,meter);
    }

    public void setWriteCoordinator(WriteCoordinator writeCoordinator){
        this.pipelineWriter.setWriteCoordinator(writeCoordinator);
    }

    @Override
    public BulkWriter newWriter(byte[] tableName){
        return new DirectBulkWriter(pipelineWriter);
    }

    @Override
    public void invalidateCache(byte[] tableName){
        //no-op for in-memory
    }

    @Override
    public void setPipeline(WritePipelineFactory writePipelineFactory){
        //no-op
    }

    @Override
    public void setWriter(PipelineWriter pipelineWriter){
        this.pipelineWriter = pipelineWriter;
    }
}