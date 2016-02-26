package edu.brown.cs.systems.pivottracing;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoints;
import edu.brown.cs.systems.pubsub.PubSub;
import junit.framework.TestCase;

public class TestPivotTracing3 extends TestCase {
    
    public void exampleExecution(String a1, String a2, int b1, int b2) {
        Baggage.start();
        
        myMethod1(a1, b1);
        myMethod2(a2, b2);
        
        Baggage.stop();
    }
    
    public void exampleExecutionReversed(String a1, String a2, int b1, int b2) {
        Baggage.start();

        myMethod2(a2, b2);
        myMethod1(a1, b1);
        
        Baggage.stop();
    }

    public void myMethod1(String a, int b) {
    }

    public void myMethod2(String a, int b) {
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

        PTQuery q = PTQuery.From(myMethod2).HappenedBeforeJoin("m1", myMethod1).Select("m1.a", "a");

        CallbackForTest cb = new CallbackForTest();
        client.install(q, cb);

        /* No results in first report */
        cb.expectNoResults();
        
        /* Invoke once */
        exampleExecution("first", "second", 10, 100);
        cb.expectTuple("first", "second");
        
        /* No results in third report */
        cb.expectNoResults();
        
        /* Execution doesn't satisfy HBjoin, so no results in fourth report */
        exampleExecutionReversed("first", "second", 10, 100);
        cb.expectNoResults();
        
        /* Invoke several times */
        exampleExecution("a", "b", 10, 100);
        exampleExecution("c", "d", 10, 100);
        exampleExecutionReversed("g", "h", 10, 100);
        exampleExecution("e", "f", 10, 100);
        exampleExecutionReversed("j", "k", 10, 100);
        cb.expectTuples(tuple("a", "b"), tuple("c", "d"), tuple("e", "f"));
        
        cb.expectNoResults();
        
    }
}
