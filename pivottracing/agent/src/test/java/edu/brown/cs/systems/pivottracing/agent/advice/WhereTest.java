package edu.brown.cs.systems.pivottracing.agent.advice;

import org.junit.Test;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.AdviceTestUtils;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import junit.framework.TestCase;

public class WhereTest extends TestCase {
    
    /** Test a numeric where condition  */
    @Test
    public void testSimpleNumericWhere() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("{}!={}", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise(5, 5);
        results.check();
        
        advice.advise(5, 6);
        results.expectTuple(5, 6);
        results.check();        
    }
    
    /** Test a numeric where condition  */
    @Test
    public void testSimpleNumericWhere2() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("{}>{}", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);

        advice.advise(2, 5);
        advice.advise(7, 5);
        results.expectTuple(7, 5);
        advice.advise(2, 5);
        advice.advise(9, 5);
        results.expectTuple(9, 5);
        advice.advise(1, 5);
        results.check();
    }
    
    /** Test a string where condition  */
    @Test
    public void testSimpleStringWhere() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("\"{}\"!=\"{}\"", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("jon", "jon");
        results.check();
        
        advice.advise("jon", "mace");
        results.expectTuple("jon", "mace");
        results.check();        
    }
    
    /** Test a string where condition  */
    @Test
    public void testSimpleStringWhere2() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("!\"{}\".equals(\"{}\")", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("jon", "jon");
        results.check();
        
        advice.advise("jon", "mace");
        results.expectTuple("jon", "mace");
        results.check();        
    }
    
    /** Test an invalid where condition  */
    @Test
    public void testInvalidWhere() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("!{}.equals({})", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("jon", "jon");
        results.check();
        
        advice.advise("jon", "mace");
        results.check();        
    }
    
    /** Test a where condition, in which the value of a variable has quote marks in it  */
    @Test
    public void testTrickyWhereString() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").where("!\"{}\".equals(\"{}\")", "x", "y").emit("q1", "x", "y").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("j\"on", "j\"on");
        results.check();
        
        advice.advise("j\"on", "m\"ace");
        results.expectTuple("j\"on", "m\"ace");
        results.check();        
    }
    
    @Test
    public void testUnknownWhereVariable() {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x", "y").where("{}!=5", "x2").emit("q1", "z").spec();
        try {
            AdviceImpl impl = new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
            // Good
        } 
    }

}
