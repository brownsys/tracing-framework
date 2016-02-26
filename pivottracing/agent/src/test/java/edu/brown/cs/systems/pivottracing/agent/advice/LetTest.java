package edu.brown.cs.systems.pivottracing.agent.advice;

import java.util.Random;

import org.junit.Test;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.AdviceTestUtils;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import junit.framework.TestCase;

public class LetTest extends TestCase {
    
    /** Test a numeric let condition with plus  */
    @Test
    public void testPlus() throws InvalidAdviceException {
        Random r = new Random();
        int numTests = 100;
        int tuplesPerTest = 10;
        for (int i = 0; i < numTests; i++) {

            BaggageAPIForTest baggage = new BaggageAPIForTest();
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "{}+{}", "x", "y").emit("q1", "z").build(baggage, results);
            
            assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            
            for (int j = 0; j < tuplesPerTest; j++) {
                double x = r.nextInt(2000000) - 1000000;
                double y = r.nextInt(2000000) - 1000000;
                double z = x + y;
                advice.advise(x, y);
                results.expectTuple(z);                            
            }
            results.check();
        }        
    }
    
    /** Test a numeric let condition with times */
    @Test
    public void testTimes() throws InvalidAdviceException {
        Random r = new Random(3);
        int numTests = 100;
        int tuplesPerTest = 10;
        for (int i = 0; i < numTests; i++) {

            BaggageAPIForTest baggage = new BaggageAPIForTest();
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "{}*{}", "x", "y").emit("q1", "z").build(baggage, results);
            
            assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            
            for (int j = 0; j < tuplesPerTest; j++) {
                double x = r.nextInt(20000) - 10000;
                double y = r.nextInt(20000) - 10000;
                double z = x * y;
                advice.advise(x, y);
                results.expectTuple(z);                            
            }
            results.check();
        }        
    }
    
    /** Test multiple lets */
    @Test
    public void testMultiLet() throws InvalidAdviceException {
        Random r = new Random(3);
        int numTests = 100;
        int tuplesPerTest = 10;
        for (int i = 0; i < numTests; i++) {

            BaggageAPIForTest baggage = new BaggageAPIForTest();
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "{}*{}", "x", "y").let("q", "{}*{}", "z", "z").emit("q1", "q").build(baggage, results);
            
            assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            
            for (int j = 0; j < tuplesPerTest; j++) {
                double x = r.nextInt(20000) - 10000;
                double y = r.nextInt(20000) - 10000;
                double z = x * y;
                double q = z * z;
                advice.advise(x, y);
                results.expectTuple(q);                            
            }
            results.check();
        }        
    }

    
    /** Test an invalid let condition  */
    @Test
    public void testInvalidLet() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "{}+", "x", "y").emit("q1", "z").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        double x = 7;
        double y = 10;
        advice.advise(x, y);        
        results.check();
    }
    
    /** Test a string let */
    @Test
    public void testStringLet() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "\"{}\"", "x").emit("q1", "z").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("jon", "mace");
        results.expectTuple("jon");
        results.check();
    }
    
    /** Test a where condition, in which the value of a variable has quote marks in it  */
    @Test
    public void testTrickyLetString() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "\"{}\"+\"{}\"", "y", "x").emit("q1", "z").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        
        advice.advise("j\"on", "m\"ace");
        results.expectTuple("m\"acej\"on");
        results.check();        
    }
    
    @Test
    public void testUnknownLetVariable() {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x", "y").let("z", "{}+", "x2", "y").emit("q1", "z").spec();
        try {
            AdviceImpl impl = new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
            // Good
        } 
    }

}
