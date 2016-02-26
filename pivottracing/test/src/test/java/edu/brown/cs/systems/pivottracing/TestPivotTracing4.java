package edu.brown.cs.systems.pivottracing;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoints;
import edu.brown.cs.systems.pubsub.PubSub;
import junit.framework.TestCase;

public class TestPivotTracing4 extends TestCase {
    
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();
    
    private static class Execution {
        
        private final String a1, a2, a3, a4;
        private final int b1, b2, b3, b4;
        
        private final CountDownLatch latch = new CountDownLatch(1);
        private DetachedBaggage ctx;
        
        public Execution(String a1, String a2, String a3, String a4, int b1, int b2, int b3, int b4) {
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
            this.a4 = a4;
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
            this.b4 = b4;
        }
        
        public void start() {
            Baggage.discard();
            
            myMethod1(a1, b1);
            submitFirst();
        }
        
        public void awaitCompletion() throws InterruptedException {
            latch.await();
            
            Baggage.start(ctx);
            myMethod4(a4, b4);
            Baggage.discard();
        }
        
        private void submitFirst() {
            ctx = Baggage.stop();
            exec.submit(new Runnable() {
                public void run() {
                    Baggage.join(ctx);
                    
                    // Briefly sleep
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                    
                    myMethod2(a2, b2);
                    submitSecond();
                }
            });
        }
        
        private void submitSecond() {
            ctx = Baggage.stop();
            exec.submit(new Runnable() {
                public void run() {
                    Baggage.join(ctx);
                    
                    myMethod3(a3, b3);
                    
                    ctx = Baggage.stop();
                    latch.countDown();
                }
            });
        }
        
    }

    public static void myMethod1(String a, int b) {
    }

    public static void myMethod2(String a, int b) {
    }

    public static void myMethod3(String a, int b) {
    }

    public static void myMethod4(String a, int b) {
    }
    
    private static ResultsTuple tuple(String... values) {
        return ResultsTuple.newBuilder().addAllValue(Lists.newArrayList(values)).build();
    }

    @Test
    public void testQuery() throws PTQueryException, IOException, InterruptedException {
        // Comment / uncomment to show console logging
//        BasicConfigurator.configure();

        // Start a pubsub server and initialize the PT agent
        PubSub.startServer();
        PivotTracing.initialize();
        Thread.sleep(100);

        // Get a client for submitting queries
        PivotTracingClient client = PivotTracingClient.newInstance();

        // Create a tracepoint
        Tracepoint myMethod1 = Tracepoints.create(getClass(), "myMethod1", "a", "b");
        Tracepoint myMethod2 = Tracepoints.create(getClass(), "myMethod2", "a", "b");
        Tracepoint myMethod3 = Tracepoints.create(getClass(), "myMethod3", "a", "b");
        Tracepoint myMethod4 = Tracepoints.create(getClass(), "myMethod4", "a", "b");

        PTQuery q = PTQuery.From("m", myMethod1).HappenedBefore2("m", myMethod2, "m1")
                .HappenedBefore2("m", myMethod3, "m2")
                .HappenedBefore2("m", myMethod4, "m3")
                .Select("m3.m2.m1.m.a", "m3.m2.m.a", "m3.m.a", "m.a");

        CallbackForTest cb = new CallbackForTest();
        client.install(q, cb);

        /* No results in first report */
        cb.expectNoResults();
        
        /* Invoke once */
        Execution execution = new Execution("first", "second", "third", "fourth", 0,0,0,0);
        execution.start();
        execution.awaitCompletion();
        cb.expectTuple("first", "second", "third", "fourth");

        /* No results in third report */
        cb.expectNoResults();
        
        /* Invoke many */
        List<Execution> executions = Lists.newArrayList();
        List<ResultsTuple> expectedResults = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            String first = "first" + i;
            String second = "second" + i;
            String third = "third" + i;
            String fourth = "fourth" + i;
            execution = new Execution(first, second, third, fourth, 0,0,0,0);
            executions.add(execution);
            expectedResults.add(tuple(first, second, third, fourth));
            execution.start();
        }
        for (Execution exec : executions) {
            exec.awaitCompletion();
        }

        /* No results in third report */
        cb.expectTuples(expectedResults);
    }
}
