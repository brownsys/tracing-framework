package edu.brown.cs.systems.pivottracing;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.agent.PTAgent;
import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoints;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubServer;
import junit.framework.TestCase;

public class TestPivotTracing1 extends TestCase {
    
    public void myMethod1(String a, int b) {
        
    }
    
    @Test
    public void testQuery() throws PTQueryException, IOException, InterruptedException {
        // Comment / uncomment to show console logging
//        BasicConfigurator.configure();
        
        // Start a pubsub server
        PubSubServer server = PubSub.startServer();
        
        // Initialize a PT agent for this process
        PivotTracing.initialize();
        PTAgent agent = PivotTracing.agent();
        
        Thread.sleep(100);
        
        // Get a client for submitting queries
        PivotTracingClient client = PivotTracingClient.newInstance();
        
        // Create a tracepoint
        Tracepoint myMethod1 = Tracepoints.create(getClass(), "myMethod1", "a", "b");
        client.addTracepoint(myMethod1);
        
        PTQuery query = PTQuery.From(myMethod1).GroupBy().Aggregate("b", Agg.MAX);
        
        CallbackForTest resultsCallback = new CallbackForTest();
        client.install(query, resultsCallback);
        
        Random r = new Random(0);
        for (int i = 0; i < 1000; i++) {
            myMethod1("hello", r.nextInt(10000) + 10000);
        }
        
        // Wait for results, up to 2 seconds
        QueryResults results = resultsCallback.awaitResults();

        assertNotNull(results);
        assertEquals(1, results.getGroupCount());
        assertEquals(0, results.getGroup(0).getGroupByCount());
        assertEquals(1, results.getGroup(0).getAggregationCount());
        assertTrue(results.getGroup(0).getAggregation(0) >= 10000);
        assertTrue(results.getGroup(0).getAggregation(0) <= 20000);
        
        results = resultsCallback.awaitResults();
        assertNotNull(results);
        assertEquals(0, results.getGroupCount());
    }
}
