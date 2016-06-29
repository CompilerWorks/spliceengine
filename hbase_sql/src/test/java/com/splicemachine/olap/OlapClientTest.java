package com.splicemachine.olap;

import com.splicemachine.access.HConfiguration;
import com.splicemachine.concurrent.Clock;
import com.splicemachine.concurrent.SystemClock;
import com.splicemachine.derby.iapi.sql.olap.AbstractOlapResult;
import com.splicemachine.derby.iapi.sql.olap.OlapClient;
import com.splicemachine.derby.iapi.sql.olap.DistributedJob;
import com.splicemachine.derby.iapi.sql.olap.OlapStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * Basic tests around the OlapServer's functionality.
 *
 * Created by dgomezferro on 3/17/16.
 */
@SuppressWarnings("unused")
public class OlapClientTest {
    private static final Logger LOG = Logger.getLogger(OlapClientTest.class);

    private static OlapServer olapServer;
    private static OlapClient olapClient;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Logger.getLogger(MappedJobRegistry.class).setLevel(Level.INFO);
        Logger.getLogger(OlapPipelineFactory.class).setLevel(Level.INFO);
        Logger.getLogger("splice.config").setLevel(Level.WARN);
        Logger.getLogger(OlapRequestHandler.class).setLevel(Level.WARN);
        setupServer();
    }


    @AfterClass
    public static void afterClass() throws Exception {
        olapClient.shutdown();
        olapServer.stopServer();
    }

    @Test(timeout = 3000)
    public void simpleTest() throws Exception {
        final Random rand = new Random(0);
        int sleep = rand.nextInt(200);
        DumbOlapResult result = olapClient.execute(new DumbDistributedJob(sleep,13));
        Assert.assertNotNull(result);
        Assert.assertEquals(13, result.order);
    }

    @Test(timeout = 8000)
    public void longRunningTest() throws Exception {
        final Random rand = new Random(0);
        int sleep = 4000;
        DumbOlapResult result = olapClient.execute(new DumbDistributedJob(sleep,13));
        Assert.assertNotNull(result);
        Assert.assertEquals(13, result.order);
    }

    @Test(timeout = 3000, expected = IllegalStateException.class)
    public void cantReuseJobsTest() throws Exception {
        final Random rand = new Random(0);
        int sleep = rand.nextInt(200);
        DumbDistributedJob ddj = new DumbDistributedJob(sleep,13);
        DumbOlapResult result = olapClient.execute(ddj);
        Assert.assertNotNull(result);
        Assert.assertEquals(13, result.order);
        DumbOlapResult result2 = olapClient.execute(ddj);
        Assert.fail("Should have raised exception");
    }

    @Test(timeout = 3000)
    @Ignore // per sf
    public void failingJobTest() throws Exception {
        try {
            DumbOlapResult result = olapClient.execute(new FailingDistributedJob("failingJob"));
            Assert.fail("Didn't raise exception");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("Expected exception"));
        }
    }

    @Test
    @Ignore // per sf
    public void repeatedFailingJob() throws Exception{
        for(int i=0;i<100;i++){
            failingJobTest();
        }
    }

    @Test(timeout = 3000)
    public void concurrencyTest() throws Exception {
        int size = 32;
        Thread[] threads = new Thread[size];
        final DumbOlapResult[] results = new DumbOlapResult[size];
        final Random rand = new Random(size);
        for (int i = 0; i < size; ++i) {
            final int j = i;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    int sleep = rand.nextInt(200);
                    try {
                        results[j] = olapClient.execute(new DumbDistributedJob(sleep,j));
                    } catch (IOException e) {
                        results[j] = null;
                    }catch(TimeoutException te){
                        Assert.fail("Timed out");
                    }
                }
            };
            threads[i].start();
        }
        for (int i = 0; i < size; ++i) {
            threads[i].join();
        }
        for (int i = 0; i < size; ++i) {
            Assert.assertNotNull(results[i]);
            Assert.assertEquals(i, results[i].order);
        }
    }

    @Test(timeout = 3000)
    public void concurrencySameNameTest() throws Exception {
        int size = 32;
        Thread[] threads = new Thread[size];
        final DumbOlapResult[] results = new DumbOlapResult[size];
        final Random rand = new Random(size);
        for (int i = 0; i < size; ++i) {
            final int j = i;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    int sleep = rand.nextInt(200);
                    try {
                        results[j] = olapClient.execute(new SameNameJob(sleep,j));
                    } catch (IOException e) {
                        results[j] = null;
                    }catch(TimeoutException te){
                        Assert.fail("Timed out");
                    }
                }
            };
            threads[i].start();
        }
        for (int i = 0; i < size; ++i) {
            threads[i].join();
        }
        for (int i = 0; i < size; ++i) {
            Assert.assertNotNull(results[i]);
            Assert.assertEquals(i, results[i].order);
        }
    }

    @Test(timeout = 5000)
    public void overflowTest() throws Exception {
        int size = 32;
        Thread[] threads = new Thread[size];
        final DumbOlapResult[] results = new DumbOlapResult[size];
        final Random rand = new Random(size);
        for (int i = 0; i < size; ++i) {
            final int j = i;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    int sleep = rand.nextInt(2000);
                    try {
                        results[j] = olapClient.execute(new DumbDistributedJob(sleep,j));
                    } catch (IOException e) {
                        results[j] = null;
                    }catch(TimeoutException te){
                        Assert.fail("Timed out");
                    }
                }
            };
            threads[i].start();
        }
        for (int i = 0; i < size; ++i) {
            threads[i].join();
        }
        for (int i = 0; i < size; ++i) {
            Assert.assertNotNull(results[i]);
            Assert.assertEquals(i, results[i].order);
        }
    }

    @Test(timeout=5000)
    public void testServerFailureAfterSubmit() throws Exception{
       /*
        * Tests what would happen if the server went down after we had successfully submitted, but while
        * we are waiting. Because this is inherently concurrent, we use multiple threads
        */
        final DumbOlapResult[]results = new DumbOlapResult[1];
        final Throwable[] errors = new Throwable[1];
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    results[0] = olapClient.execute(new DumbDistributedJob(100000,0));
                }catch(IOException | TimeoutException e){
                    errors[0]=e;
                    results[0] = null;
                }
            }
        });
        t.start();

        Thread.sleep(1000);
        //shut down the server
        olapServer.stopServer();

        try{
            t.join();
            Assert.assertNull(results[0]);
            Assert.assertNotNull(errors[0]);
        }finally{
            //restart the server
            olapClient.shutdown();
            setupServer();
        }
    }

    private static class DumbOlapResult extends AbstractOlapResult {
        int order;

        public DumbOlapResult(){
        }

        DumbOlapResult(int order) {
            this.order = order;
        }

        @Override
        public boolean isSuccess(){
            return true;
        }
    }

    private static class DumbDistributedJob extends DistributedJob{
        private static final Logger LOG = Logger.getLogger(DumbDistributedJob.class);
        int order;
        int sleep;

        public DumbDistributedJob(){ }

        DumbDistributedJob(int sleep,int order) {
            this.sleep = sleep;
            this.order = order;
        }

        @Override
        public Callable<Void> toCallable(final OlapStatus jobStatus,Clock clock,long clientTimeoutCheckIntervalMs){
            return new Callable<Void>(){
                @Override
                public Void call() throws Exception{
                    jobStatus.markRunning();
                    LOG.trace("started job " + getUniqueName() + " with order " + order);
                    Thread.sleep(sleep);
                    LOG.trace("finished job " + getUniqueName() + " with order " + order);
                    jobStatus.markCompleted(new DumbOlapResult(order));
                    return null;
                }
            };
        }

        @Override
        public String getName(){
            return "DumbDistributedJob["+order+"]";
        }

    }

    private static class SameNameJob extends DumbDistributedJob {

        public SameNameJob() {}

        SameNameJob(int sleep,int order) {
            super(sleep, order);
        }

        @Override
        public String getName(){
            return "SameNameJob";
        }

    }

    private static class FailingDistributedJob extends DistributedJob{
        private String uniqueId;

        public FailingDistributedJob(){
        }

        FailingDistributedJob(String uniqueId){
            this.uniqueId=uniqueId;
        }

        @Override
        public Callable<Void> toCallable(final OlapStatus jobStatus,Clock clock,long clientTimeoutCheckIntervalMs){
            return new Callable<Void>(){
                @Override
                public Void call() throws Exception{
                    jobStatus.markRunning();
                    jobStatus.markCompleted(new FailedOlapResult(new IOException("Expected exception")));
                    return null;
                }
            };
        }

        @Override
        public String getName(){
            return uniqueId;
        }

    }

    private static void setupServer(){
        Clock clock=new SystemClock();
        olapServer = new OlapServer(0,clock); // any port
        olapServer.startServer(HConfiguration.getConfiguration());
        JobExecutor nl = new AsyncOlapNIOLayer(olapServer.getBoundHost(),olapServer.getBoundPort());
        olapClient = new TimedOlapClient(nl,10000);
    }
}
