package edu.brown.cs.systems.pivottracing.agent.advice;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.AdviceTestUtils;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import junit.framework.TestCase;

public class ObserveTest extends TestCase {
    
    public static List<String> randomStrings(Random r, int count) {
        List<String> strings = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            strings.add(randomString(r));
        }
        return strings;
    }
    
    public static String randomString(Random r) {
        return RandomStringUtils.random(r.nextInt(10) + 2, 'a', 'z', true, false, null, r);
    }
    
    /** If we observe no keys and emit no keys, still expect output tuples, they are just empty */
    @Test
    public void testEmptyTuple() throws InvalidAdviceException {
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe().emit("test1").build(baggage, results);
        
        assertTrue("Expect nothing packed yet", baggage.packed.isEmpty());
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise();
        results.expectTuple();
        assertTrue("Expect nothing packed", baggage.packed.isEmpty());
        assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
    }
    
    /** Tests a variety of combinations of input and output keys */
    @Test
    public void testObserveEmit() throws InvalidAdviceException {
        Random r = new Random(10);
        
        /* 10 keys in total */
        int numKeys = 10;
        int numTests = 1000;
        int tuplesPerTest = 20;
        List<String> keysToObserve = randomStrings(r, numKeys);
        
        for (int i = 0; i < numTests; i++) {
            // Select subset of keys
            List<String> keysToEmit = Lists.newArrayList();
            for (String key : keysToObserve) {
                if (r.nextBoolean()) {
                    keysToEmit.add(key);
                }
            }
            
            // Shuffle
            for (int j = 0; j < keysToEmit.size(); j++) {
                int randomSwapIndex = r.nextInt(keysToEmit.size());
                String a = keysToEmit.get(j);
                String b = keysToEmit.get(randomSwapIndex);
                keysToEmit.add(j, b);
                keysToEmit.remove(j+1);
                keysToEmit.add(randomSwapIndex, a);
                keysToEmit.remove(randomSwapIndex+1);
            }
            
            // Set up advice
            String outputId = randomString(r);
            BaggageAPIForTest baggage = new BaggageAPIForTest();
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe(keysToObserve).emit(outputId, keysToEmit).build(baggage, results);
            
            for (int j = 0; j < tuplesPerTest; j++) {
                // Generate a tuple
                Map<String, String> values = Maps.newHashMap();
                List<String> observed = Lists.newArrayList();
                for (String k : keysToObserve) {
                    String v = randomString(r);
                    values.put(k, v);
                    observed.add(v);
                }
                
                // Save expected output tuple
                List<String> expectedTuple = Lists.newArrayList();
                for (String k : keysToEmit) {
                    expectedTuple.add(values.get(k));
                }
                
                // Run advice and save expected results
                advice.advise(observed.toArray());
                results.expectTuple(expectedTuple);
                
            }
            
            // Finally, check results
            results.check();
        }
    }
    
    /** Try to emit an unknown variable */
    @Test
    public void testEmitUnknownVar() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").emit(outputId, "x2").spec();
        try {
            new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
        }
    }
    
    /** Try to emit an unknown variable */
    @Test
    public void testEmitUnknownAggVar() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").emit(outputId, "x", "x2", Agg.SUM).spec();
        try {
            new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
        }
    }
    
    /** Try to emit an unknown variable, but with count */
    @Test
    public void testEmitCount() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").emit(outputId, "x", "x2", Agg.COUNT).spec();
        try {
            AdviceImpl impl = new AdviceImpl(advice, baggage, results);
            impl.advise("hello");
            results.expectTuple("hello", 1L);
        } catch (InvalidAdviceException e) {
            fail();
        }
    }
    
    /** Try to pack an unknown variable */
    @Test
    public void testPackUnknownVar() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").pack(outputId, "x2").spec();
        try {
            new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
        }
    }
    
    /** Try to pack an unknown aggregate variable */
    @Test
    public void testPackUnknownAggVar() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").pack(outputId, "x", "x2", Agg.SUM).spec();
        try {
            new AdviceImpl(advice, baggage, results);
            fail();
        } catch (InvalidAdviceException e) {
        }
    }
    
    /** Try to pack an unknown aggregate variable but with count */
    @Test
    public void testPackCount() {    
        // Set up advice
        String outputId = "output";
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        EmitAPIForTest results = new EmitAPIForTest();
        AdviceSpec advice = AdviceTestUtils.newAdvice().observe("x").pack(outputId, "x", "x2", Agg.COUNT).spec();
        try {
            AdviceImpl impl = new AdviceImpl(advice, baggage, results);            
            impl.advise("hello");
            baggage.expect("hello", 1L);
            impl.advise("hello");
            baggage.expect("hello", 1L);
            impl.advise("hello");
            baggage.expect("hello", 1L);
            impl.advise("hello");
            baggage.expect("hello", 1L);
            baggage.check();
        } catch (InvalidAdviceException e) {
            fail();
        }
    }
}
