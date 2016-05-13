package com.splicemachine.olap;

import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;
import com.splicemachine.backup.OlapMessage;
import com.splicemachine.derby.iapi.sql.olap.DistributedJob;
import com.splicemachine.derby.iapi.sql.olap.OlapResult;
import com.splicemachine.pipeline.Exceptions;
import org.sparkproject.io.netty.bootstrap.Bootstrap;
import org.sparkproject.io.netty.channel.*;
import org.sparkproject.io.netty.channel.nio.NioEventLoopGroup;
import org.sparkproject.io.netty.channel.pool.AbstractChannelPoolHandler;
import org.sparkproject.io.netty.channel.pool.ChannelPool;
import org.sparkproject.io.netty.channel.pool.SimpleChannelPool;
import org.sparkproject.io.netty.channel.socket.nio.NioSocketChannel;
import org.sparkproject.io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.sparkproject.io.netty.handler.codec.LengthFieldPrepender;
import org.sparkproject.io.netty.handler.codec.protobuf.ProtobufDecoder;
import org.sparkproject.io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.sparkproject.io.netty.util.concurrent.Future;
import org.sparkproject.io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.Logger;
import org.sparkproject.guava.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Scott Fines
 *         Date: 4/4/16
 */
public class AsyncOlapNIOLayer implements JobExecutor{
    private static final Logger LOG=Logger.getLogger(AsyncOlapNIOLayer.class);

    private final ChannelPool channelPool;
    private final ProtobufDecoder decoder=new ProtobufDecoder(OlapMessage.Response.getDefaultInstance(),buildExtensionRegistry());

    private ExtensionRegistry buildExtensionRegistry(){
        ExtensionRegistry er=ExtensionRegistry.newInstance();
        er.add(OlapMessage.FailedResponse.response);
        er.add(OlapMessage.CancelledResponse.response);
        er.add(OlapMessage.ProgressResponse.response);
        er.add(OlapMessage.Result.response);
        return er;
    }


    public AsyncOlapNIOLayer(String host,int port){
        InetSocketAddress socketAddr=new InetSocketAddress(host,port);
        Bootstrap bootstrap=new Bootstrap();
        NioEventLoopGroup group=new NioEventLoopGroup(5,new ThreadFactoryBuilder().setNameFormat("olapClientWorker-%d").setDaemon(true).build());
        bootstrap.channel(NioSocketChannel.class)
                .group(group)
                .option(ChannelOption.SO_KEEPALIVE,true)
                .remoteAddress(socketAddr);

        //TODO -sf- this may be excessive network usage --consider a bounded pool to prevent over-connection?
        this.channelPool=new SimpleChannelPool(bootstrap,new AbstractChannelPoolHandler(){
            @Override
            public void channelCreated(Channel channel) throws Exception{
                ChannelPipeline p=channel.pipeline();
                p.addLast("frameEncoder",new LengthFieldPrepender(4));
                p.addLast("protobufEncoder",new ProtobufEncoder());
                p.addLast("frameDecoder",new LengthFieldBasedFrameDecoder(1<<20,0,4,0,4));
                p.addLast("protobufDecoder",decoder);
            }
        });
    }

    @Override
    public java.util.concurrent.Future<OlapResult> submit(DistributedJob job) throws IOException{
        assert job.isSubmitted();
        OlapFuture future=new OlapFuture(job);
        future.doSubmit();
        return future;
    }


    @Override
    public void shutdown(){
        channelPool.close(); //disconnect everything
    }


    /* ****************************************************************************************************************/
    /*Private Helper methods and classes*/

    private OlapResult parseFromResponse(OlapMessage.Response response) throws IOException{
        switch(response.getType()){
            case NOT_SUBMITTED:
                return new NotSubmittedResult();
            case FAILED:
                OlapMessage.FailedResponse fr=response.getExtension(OlapMessage.FailedResponse.response);
                throw Exceptions.rawIOException((Throwable)OlapSerializationUtils.decode(fr.getErrorBytes()));
            case IN_PROGRESS:
                OlapMessage.ProgressResponse pr=response.getExtension(OlapMessage.ProgressResponse.response);
                return new SubmittedResult(pr.getTickTimeMillis());
            case CANCELLED:
                return new CancelledResult();
            case COMPLETED:
                OlapMessage.Result r=response.getExtension(OlapMessage.Result.response);
                return OlapSerializationUtils.decode(r.getResultBytes());
            default:
                throw new IllegalStateException("Programmer error: unexpected response type");
        }
    }

    private class OlapFuture implements java.util.concurrent.Future<OlapResult>{
        private final DistributedJob job;
        private final Lock checkLock=new ReentrantLock();
        private final Condition signal=checkLock.newCondition();
        private final ChannelHandler resultHandler = new ResultHandler(this);
        private final ChannelHandler submitHandler = new SubmitHandler(this);

        private final GenericFutureListener<Future<Void>> failListener=new GenericFutureListener<Future<Void>>(){
            @Override
            public void operationComplete(Future<Void> future) throws Exception{
                if(!future.isSuccess()){
                    fail(future.cause());
                    signal();
                }
            }
        };


        private volatile OlapResult finalResult;
        private volatile boolean cancelled=false;
        private volatile boolean failed=false;
        private volatile boolean submitted=false;
        private volatile Throwable cause=null;
        private volatile long tickTimeNanos=TimeUnit.MILLISECONDS.toNanos(1000L);

        OlapFuture(DistributedJob job){
            this.job=job;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning){
            if(isDone()) return false;
            else if(cancelled) return true;
            doCancel();

            /*
             *This is a bit weird, since we'll return true before we know whether or not it can be cancelled,
             *but the Future interface is awkward enough that that's the way it goes
             */
            return true;
        }

        @Override
        public boolean isCancelled(){
            return cancelled;
        }

        @Override
        public boolean isDone(){
            return cancelled || failed || finalResult!=null;
        }

        @Override
        public OlapResult get() throws InterruptedException, ExecutionException{
            try{
                return get(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
            }catch(TimeoutException e){
                //this will never happen, because we wait for forever. But just in case, wrap it in a runtime and
                //throw it anyway
                throw new RuntimeException(e);
            }
        }

        @Override
        public OlapResult get(long timeout,@Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException{
            long nanosRemaining=unit.toNanos(timeout);
            while(nanosRemaining>0){
                if(finalResult!=null)
                    return finalResult;
                else if(failed)
                    throw new ExecutionException(cause);
                else if(cancelled)
                    throw new CancellationException("Job " + job.getUniqueName() +" was cancelled.");
                if(Thread.currentThread().isInterrupted())
                    throw new InterruptedException();

                if (submitted) {
                    // don't request status until submitted
                    Future<Channel> cFut = channelPool.acquire();
                    cFut.addListener(new StatusListener(this)).sync();
                }
                checkLock.lock();
                try{
                    long window=Math.min(tickTimeNanos,nanosRemaining);
                    long remaining=signal.awaitNanos(window);
                    nanosRemaining-=(window-remaining);
                    long millisRemaining = TimeUnit.NANOSECONDS.toMillis(remaining);
                    if (!isDone() && millisRemaining > 0) {
                        // we are not done yet, wait the whole window before a new status check
                        Thread.sleep(millisRemaining);
                    }
                }finally{
                    checkLock.unlock();
                }
            }

            if(finalResult!=null)
                return finalResult;
            else if(failed)
                throw new ExecutionException(cause);

            throw new TimeoutException();
        }

        private void doCancel(){
            Future<Channel> channelFuture=channelPool.acquire();
            channelFuture.addListener(new CancelCommand(job.getUniqueName()));
            cancelled=true;
        }

        void fail(Throwable cause){
            this.cause=cause;
            failed=true;
        }

        void doSubmit() throws IOException{
            Future<Channel> channelFuture=channelPool.acquire();
            try{
                channelFuture.addListener(new SubmitCommand(this)).sync();
            }catch(InterruptedException ie){
                /*
                 * We were interrupted, which pretty much means that we are shutting down.
                 * Still, the API doesn't allow us to throw an Interrupt here, but we don't want to
                 * completely ignore it either. So we mark the current thread interrupted,
                 * then return (allowing early breakout without the exceptional error case).
                 */
                Thread.currentThread().interrupt();
            }
        }

        void signal(){
            checkLock.lock();
            try{
                signal.signalAll();
            }finally{
                checkLock.unlock();
            }
        }
    }

    private class SubmitCommand implements GenericFutureListener<Future<Channel>>{
        private OlapFuture olapFuture;

        SubmitCommand(OlapFuture olapFuture){
            this.olapFuture=olapFuture;
        }

        @Override
        public void operationComplete(Future<Channel> channelFuture) throws Exception{
            if(!channelFuture.isSuccess()){
                olapFuture.fail(channelFuture.cause());
                return;
            }
            final Channel c=channelFuture.getNow();
            ChannelPipeline writePipeline=c.pipeline();
            writePipeline.addLast("handler",olapFuture.submitHandler);

            if (LOG.isTraceEnabled()) {
                LOG.trace("Submitted job " + olapFuture.job.getUniqueName());
            }

            ByteString data=OlapSerializationUtils.encode(olapFuture.job);
            OlapMessage.Submit submit=OlapMessage.Submit.newBuilder().setCommandBytes(data).build();
            OlapMessage.Command cmd=OlapMessage.Command.newBuilder()
                    .setUniqueName(olapFuture.job.getUniqueName())
                    .setExtension(OlapMessage.Submit.command,submit)
                    .setType(OlapMessage.Command.Type.SUBMIT)
                    .build();
            ChannelFuture writeFuture=c.writeAndFlush(cmd);
            writeFuture.addListener(olapFuture.failListener);
        }
    }

    private class StatusListener implements GenericFutureListener<Future<Channel>>{
        private final OlapFuture olapFuture;

        StatusListener(OlapFuture olapFuture){
            this.olapFuture=olapFuture;
        }

        @Override
        public void operationComplete(Future<Channel> channelFuture) throws Exception{
            if(!channelFuture.isSuccess()){
                olapFuture.fail(channelFuture.cause());
                return;
            }

            final Channel c=channelFuture.getNow();
            ChannelPipeline writePipeline=c.pipeline();
            writePipeline.addLast("handler",olapFuture.resultHandler);

            if (LOG.isTraceEnabled()) {
                LOG.trace("Status check job " + olapFuture.job.getUniqueName());
            }

            OlapMessage.Status status=OlapMessage.Status.newBuilder().build();
            OlapMessage.Command cmd=OlapMessage.Command.newBuilder()
                    .setUniqueName(olapFuture.job.getUniqueName())
                    .setType(OlapMessage.Command.Type.STATUS)
                    .setExtension(OlapMessage.Status.command,status).build();
            ChannelFuture writeFuture=c.writeAndFlush(cmd);
            writeFuture.addListener(new GenericFutureListener<Future<Void>>(){
                @Override
                public void operationComplete(Future<Void> future) throws Exception{
                    //TODO -sf- possible failover/retry mechanism in place here?
                    if(!future.isSuccess()){
                        olapFuture.fail(future.cause());
                        olapFuture.signal();
                    }
                }
            });
        }
    }

    private class CancelCommand implements GenericFutureListener<Future<Channel>>{
        private String uniqueName;

        CancelCommand(String uniqueName){
            this.uniqueName=uniqueName;
        }

        @Override
        public void operationComplete(Future<Channel> channelFuture) throws Exception{
            if(!channelFuture.isSuccess()){
                 /*
                  * Unfortunately, no one is listening to this, so there's really no
                  * way to communicate this back to the client (the client has moved on).
                  * So just note the error and move on.
                  */
                LOG.error("Unable to cancel job "+uniqueName+": Unable to obtain channel",channelFuture.cause());
                return;
            }

            final Channel c=channelFuture.getNow();

            OlapMessage.Cancel cancel=OlapMessage.Cancel.newBuilder().build();
            OlapMessage.Command cmd=OlapMessage.Command.newBuilder()
                    .setUniqueName(uniqueName)
                    .setType(OlapMessage.Command.Type.CANCEL)
                    .setExtension(OlapMessage.Cancel.command,cancel).build();
            ChannelFuture writeFuture=c.writeAndFlush(cmd);
            writeFuture.addListener(new GenericFutureListener<Future<Void>>(){
                @Override
                public void operationComplete(Future<Void> future) throws Exception{
                    if(future.isSuccess()){
                        if(LOG.isTraceEnabled()){
                            LOG.trace("job "+uniqueName+" cancelled successfully");
                        }
                    }else{
                        LOG.error("Unable to cancel job "+uniqueName+": Unable to write cancel command",future.cause());
                    }
                    //once the write is complete, release the channel from the pool
                    channelPool.release(c);
                }
            });
        }
    }

    @ChannelHandler.Sharable
    private final class ResultHandler extends SimpleChannelInboundHandler<OlapMessage.Response>{
        private final OlapFuture future;

        ResultHandler(OlapFuture future){
            this.future=future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx,OlapMessage.Response olapResult) throws Exception{
            OlapResult or=parseFromResponse(olapResult);
            //TODO -sf- deal with a OlapServer failover here (i.e. a move to NOT_SUBMITTED from any other state
            if(or instanceof SubmittedResult) {
                future.tickTimeNanos = TimeUnit.MILLISECONDS.toNanos(((SubmittedResult) or).getTickTime());
            }else if(future.submitted && !future.isDone() && or instanceof NotSubmittedResult) {
                // The job is no longer submitted, assume aborted
                future.fail(new IOException("Status not available, assuming aborted due to client timeout"));
            }else if(or.isSuccess()){
                future.finalResult=or;
            }else{
                Throwable t=or.getThrowable();
                if(t!=null){
                    future.fail(t);
                }
            }
            ctx.pipeline().remove(this); //we don't want this in the pipeline anymore
            Channel channel=ctx.channel();
            channelPool.release(channel); //release the underlying channel back to the pool cause we're done
            future.signal();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception{
            future.fail(cause);
            ctx.pipeline().remove(this); //we don't want this in the pipeline anymore
            channelPool.release(ctx.channel());
            future.signal();
        }
    }

    @ChannelHandler.Sharable
    private final class SubmitHandler extends SimpleChannelInboundHandler<OlapMessage.Response>{
        private final OlapFuture future;

        SubmitHandler(OlapFuture future){
            this.future=future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx,OlapMessage.Response olapResult) throws Exception{
            OlapResult or=parseFromResponse(olapResult);
            if(or instanceof SubmittedResult) {
                future.tickTimeNanos = TimeUnit.MILLISECONDS.toNanos(((SubmittedResult) or).getTickTime());
                future.submitted = true;
            }else{
                Throwable t=or.getThrowable();
                LOG.error("Job wasn't submitted, result: " + or);
                if(t!=null){
                    future.fail(t);
                }else{
                    future.fail(new IOException("Job wasn't submitted, result: "+or));
                }
            }
            ctx.pipeline().remove(this); //we don't want this in the pipeline anymore
            Channel channel=ctx.channel();
            channelPool.release(channel); //release the underlying channel back to the pool cause we're done
            future.signal();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception{
            future.fail(cause);
            ctx.pipeline().remove(this); //we don't want this in the pipeline anymore
            channelPool.release(ctx.channel());
            future.signal();
        }
    }
}
