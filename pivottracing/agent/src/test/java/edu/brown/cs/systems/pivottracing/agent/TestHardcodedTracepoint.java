package edu.brown.cs.systems.pivottracing.agent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.PivotTracingUpdate;
import edu.brown.cs.systems.pivottracing.agent.PTAgent.PTAgentException;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.WeaveSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPIImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.AdviceTestUtils;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import junit.framework.TestCase;

public class TestHardcodedTracepoint extends TestCase {
    
    @Test
    public void testRegisterHardcodedTracepoint() throws PTAgentException {
        // Create dummy PT agent
        EmitAPIForTest results = new EmitAPIForTest();
        PTAgent agent = new PTAgent(new BaggageAPIImpl(), results, null);
        
        // Create tracepoint
        HardcodedTracepoint t = agent.registerHardcodedTracepoint("test", "a", "b", "c");
        
        // Try advising -- nothing should happen
        t.Advise(3,5,3);
        results.check();
        
        // Try with wrong number of args -- nothing should happen (except a warning printed)
        t.Advise(3,5,3, 2);
        results.check();
        t.Advise(3);
        results.check();
        
        final AtomicInteger count = new AtomicInteger(0);
        final List<Object[]> allValues = Lists.newArrayList();
        Advice advice = new Advice() {
            public void advise(Object... values) {
                count.getAndIncrement();
                allValues.add(values);
            }
        };
        
        int weaveid = t.weave(advice, Lists.newArrayList("b"));
        t.Advise(1,2,3,4);
        results.check(); // should do nothing
        assertEquals(0, count.get());
        
        Object[][] tuples = {
                { 1, 2, 3},
                { 4, 5, 6},
                { 7, 8, 9}
        };
        
        for (int i = 0; i < tuples.length; i++) {
            Object[] tuple = tuples[i];
            t.Advise(tuple);
            assertEquals(i+1, count.get());
            assertEquals(1, allValues.get(i).length);
            assertEquals(tuple[1], allValues.get(i)[0]);
        }
        
        t.unweave(weaveid);
        int expectCount = count.get();

        t.Advise(7);
        t.Advise(7, 8);
        t.Advise(7, 8, 9);
        t.Advise(7, 8, 9, 10);
        
        assertEquals(expectCount, count.get());
        
        try {
            t.unweave(weaveid);
            fail("Did not throw expected exception");
        } catch (PTAgentException e) {
            // good
        }
        
    }
    
    @Test
    public void testWeaveToHardcodedTracepoint() {

        // Create dummy PT agent
        EmitAPIForTest results = new EmitAPIForTest();
        PTAgent agent = new PTAgent(new BaggageAPIImpl(), results, null);
        
        // Create tracepoint
        HardcodedTracepoint t = agent.registerHardcodedTracepoint("test", "a", "b", "c");
        
        ByteString weaveId = ByteString.copyFromUtf8("jontest");
        PivotTracingUpdate.Builder b = PivotTracingUpdate.newBuilder();
        WeaveSpec.Builder weave = b.addWeaveBuilder().setId(weaveId);
        weave.setAdvice(AdviceTestUtils.newAdvice().observe("a", "b").emit("q1", "b").spec());
        weave.addTracepointBuilder().getHardcodedTracepointBuilder().setId("test").addExport("a").addExport("b");
        
        results.check();
        t.Advise(7, 8, 9);
        results.check();
        
        assertFalse(t.hasWovenAdvice());
        agent.install(b.build());
        assertTrue(t.hasWovenAdvice());

        results.check();
        Object[] allAdvised = { 7, 8, 9 };
        Object[] expected = { allAdvised[1] };
        t.Advise(allAdvised);
        results.expectTuple(expected);
        results.check();
        
    }

}
