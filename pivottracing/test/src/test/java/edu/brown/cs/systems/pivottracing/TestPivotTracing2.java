package edu.brown.cs.systems.pivottracing;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoints;
import edu.brown.cs.systems.pubsub.PubSub;
import junit.framework.TestCase;

public class TestPivotTracing2 extends TestCase {

    public void myMethod1(String a, int b) {

    }

    public void myMethod2(String a, int b) {

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

        PTQuery q1 = PTQuery.From(myMethod1).Select("a");
        PTQuery q2 = PTQuery.From(myMethod2).Select("a");

        CallbackForTest cb1 = new CallbackForTest();
        CallbackForTest cb2 = new CallbackForTest();
        client.install(q1, cb1);
        client.install(q2, cb2);

        QueryResults r;
        
        /* No results in first report */

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());
        
        /* Results for Q1 only, none for Q2 */
        
        Random random = new Random(0);
        List<ResultsTuple> expectedResults = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            myMethod1("hello1", random.nextInt(10000) + 10000);
            expectedResults.add(ResultsTuple.newBuilder().addValue("hello1").build());
        }

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(1000, r.getTupleCount());
        assertEquals(expectedResults, r.getTupleList());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());
        
        /* No results in next report */

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());
        
        /* Results for Q2 only, none for Q1 */
        
        expectedResults = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            myMethod2("hello2", random.nextInt(10000) + 10000);
            expectedResults.add(ResultsTuple.newBuilder().addValue("hello2").build());
        }

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(1000, r.getTupleCount());
        assertEquals(expectedResults, r.getTupleList());
        
        /* No results in next report */

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());
        
        /* Results for both */
        List<ResultsTuple> expectedResults1 = Lists.newArrayList();
        List<ResultsTuple> expectedResults2 = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            myMethod1("hello1", random.nextInt(10000) + 10000);
            myMethod2("hello2", random.nextInt(10000) + 10000);
            expectedResults1.add(ResultsTuple.newBuilder().addValue("hello1").build());
            expectedResults2.add(ResultsTuple.newBuilder().addValue("hello2").build());
        }

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(1000, r.getTupleCount());
        assertEquals(expectedResults1, r.getTupleList());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(1000, r.getTupleCount());
        assertEquals(expectedResults2, r.getTupleList());
        
        /* No results in next report */

        r = cb1.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());

        r = cb2.awaitResults();
        assertNotNull(r);
        assertEquals(0, r.getGroupCount());
        assertEquals(0, r.getTupleCount());
    }
}
