package edu.brown.cs.systems.pivottracing.agent.advice;

import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.AdviceTestUtils;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import junit.framework.TestCase;

public class UnpackTest extends TestCase {
    
    /** Test that unpacked tuple with nothing observed still produces an output tuple */
    public void testUnpackNoObserve() throws InvalidAdviceException {
        String bag = "bag1";
        Object[][] packedTuple = {{"dsifji2oj", "23498ngnjs"}};
        BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe().unpack(bag, "pa", "pb").emit("test1", "pa", "pb").build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise();
        results.expectTuple("dsifji2oj", "23498ngnjs");
        results.check();
        assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
    }
    
    /** Test that unpacked tuple with no values, with nothing observed, still produces an output tuple */
    public void testUnpackNoValues() throws InvalidAdviceException {
        String bag = "e";
        Object[][] packedTuple = {{}};
        BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe().unpack(bag).emit("test1").build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise();
        results.expectTuple();
        results.check();
        assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
    }
    
    /** Test that unpacked tuple with no values, with nothing observed, still produces mutiple output tuple */
    public void testUnpackNoValuesMulti() throws InvalidAdviceException {
        String bag = "e";
        Object[][] packedTuple = {{}, {}, {}, {}};
        BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe().unpack(bag).emit("test1").build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise();
        results.expectTuple();
        results.expectTuple();
        results.expectTuple();
        results.expectTuple();
        results.check();
        assertTrue("Expect 4 output tuples emitted", results.emitted.size() == 4);
    }
    
    /** Test that if no packed tuples exist, nothing is joined, so no outputs */
    public void testUnpackNothing() throws InvalidAdviceException {
        {
            String bag = "e";
            Object[][] packedTuple = {};
            BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe().unpack(bag).emit("test1").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise();
            results.check();
            assertTrue("Expect 0 output tuples", results.emitted.size() == 0);
        }
        {
            String bag = "bag1";
            Object[][] packedTuple = {};
            BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack(bag, "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.check();
            assertTrue("Expect 0 output tuples", results.emitted.size() == 0);
        }
    }
    
    /** Test that if we unpack multiple bags, if only one bag contains nothing, still no outputs */
    public void testUnpackNothingMulti() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"v1", "v2"}});
        baggage.put("bag2", new Object[][] {});
        baggage.put("bag3", new Object[][] {{"v5", "v6"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .unpack("bag2", "e", "f")
                                                  .unpack("bag3", "g", "h")
                                                  .emit("test1", "a", "b", "c", "d", "e", "f", "g", "h")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        advice.advise("vva", "vvc");
        advice.advise("vva", "vve");
        results.check();
        assertTrue("Expect 0 output tuple emitted", results.emitted.size() == 0);
    }
    
    /** Unpack one tuple, construct correct tuple */
    public void testUnpack() throws InvalidAdviceException {
        {
            String bag = "bag1";
            Object[][] packedTuple = {{"dsifji2oj", "23498ngnjs"}};
            BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack(bag, "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2", "23498ngnjs", "dsifji2oj", "ooowoowq");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
        {
            String bag = "bag7";
            Object[][] packedTuple = {{"dsifji2oj", "23498ngnjs"}};
            BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack(bag, "pa", "pb").emit("test1", "oa").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
        {
            String bag = "bag4";
            Object[][] packedTuple = {{"dsifji2oj", "23498ngnjs"}};
            BaggageAPIForTest baggage = new BaggageAPIForTest().put(bag, packedTuple);
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack(bag, "pa", "pb").emit("test1", "pb").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("23498ngnjs");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
    }
    
    /** Test that tuples are unpacked from correct bag */
    public void testUnpackFromCorrectBag() throws InvalidAdviceException {
        Object[][] t1 = {{"dsifji2oj", "23498ngnjs"}};
        Object[][] t2 = {{"dsfj9", "u1h32jbn4l1  '"}};
        Object[][] t3 = {{"24oi23n", "022l;'][   "}};
        Object[][] t4 = {{"0m0lkj34", "hh2h2n  jds "}};
        BaggageAPIForTest baggage = new BaggageAPIForTest().put("bag1", t1).put("bag2", t2).put("bag3", t3).put("bag4", t4);

        {
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack("bag1", "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2", "23498ngnjs", "dsifji2oj", "ooowoowq");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
        {
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack("bag2", "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2", "u1h32jbn4l1  '", "dsfj9", "ooowoowq");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
        {
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack("bag3", "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2", "022l;'][   ", "24oi23n", "ooowoowq");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
        {
            EmitAPIForTest results = new EmitAPIForTest();
            Advice advice = AdviceTestUtils.newAdvice().observe("oa", "ob").unpack("bag4", "pa", "pb").emit("test1", "oa", "pb", "pa", "ob").build(baggage, results);
            
            assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
            advice.advise("d8jdj2", "ooowoowq");
            results.expectTuple("d8jdj2", "hh2h2n  jds ", "0m0lkj34", "ooowoowq");
            results.check();
            assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);
        }
    }
    
    /** If we hbjoin with multiple bags, make sure we get the correct join of tuples 
     * @throws InvalidAdviceException */
    public void testUnpackMultipleBags() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"v1", "v2"}});
        baggage.put("bag2", new Object[][] {{"v3", "v4"}});
        baggage.put("bag3", new Object[][] {{"v5", "v6"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .unpack("bag2", "e", "f")
                                                  .unpack("bag3", "g", "h")
                                                  .emit("test1", "a", "b", "c", "d", "e", "f", "g", "h")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        advice.advise("vva", "vvc");
        advice.advise("vva", "vve");
        results.expectTuple("vva", "vvb", "v1", "v2", "v3", "v4", "v5", "v6");
        results.expectTuple("vva", "vvc", "v1", "v2", "v3", "v4", "v5", "v6");
        results.expectTuple("vva", "vve", "v1", "v2", "v3", "v4", "v5", "v6");
        results.check();
        assertTrue("Expect 3 output tuple emitted", results.emitted.size() == 3);
    }
    
    /** If multiple tuples are unpacked, make sure it produces correct number of output tuples */
    public void testUnpackMultipleTuples() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"v1", "v2"}, {"v3", "v4"}, {"v5", "v6"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .emit("test1", "a", "b", "c", "d")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        results.expectTuple("vva", "vvb", "v1", "v2");
        results.expectTuple("vva", "vvb", "v3", "v4");
        results.expectTuple("vva", "vvb", "v5", "v6");
        results.check();
        assertTrue("Expect 3 output tuple emitted", results.emitted.size() == 3);        
    }
    
    /** If multiple tuples are unpacked, make sure it produces correct number of output tuples */
    public void testUnpackMultipleTuplesMultipleBags() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"b1_t1_v1", "b1_t1_v2"}, {"b1_t2_v1", "b1_t2_v2"}, {"b1_t3_v1", "b1_t3_v2"}});
        baggage.put("bag2", new Object[][] {{"b2_t1_v1", "b2_t1_v2"}, {"b2_t2_v1", "b2_t2_v2"}, {"b2_t3_v1", "b2_t3_v2"}});
        baggage.put("bag3", new Object[][] {{"b3_t1_v1", "b3_t1_v2"}, {"b3_t2_v1", "b3_t2_v2"}, {"b3_t3_v1", "b3_t3_v2"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .unpack("bag2", "e", "f")
                                                  .unpack("bag3", "g", "h")
                                                  .emit("test1", "a")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        advice.advise("vva", "vvc");
        advice.advise("vva", "vve");
        int expected = 3 * 3 * 3 * 3;
        for (int i = 0; i < expected; i++) {
            results.expectTuple("vva");
        }
        results.check();
        assertTrue("Expect "+expected+" output tuple emitted", results.emitted.size() == expected);     
    }
    
    /** Unpack multiple tuples, but only some of them satisfy predicate, ensure correct output */
    public void testFilterUnpackedMulti() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"v1", "v2"}, {"v3", "v4"}, {"v5", "v6"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .where("\"{}\"==\"v1\"", "c")
                                                  .emit("test1", "a", "b", "c", "d")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        results.expectTuple("vva", "vvb", "v1", "v2");
        results.check();
        assertTrue("Expect 1 output tuple emitted", results.emitted.size() == 1);    
        
    }
    
    /** Apply a filter to values unpacked from different bags 
     * @throws InvalidAdviceException */
    public void testFilterMultipleBags() throws InvalidAdviceException {
        // Set up the baggage
        BaggageAPIForTest baggage = new BaggageAPIForTest();
        baggage.put("bag1", new Object[][] {{"v1", "b1_v2"}, {"b1_v3", "v1"}, {"v2", "b1_v5"}});
        baggage.put("bag2", new Object[][] {{"b2_v1", "v2"}, {"b2_v3", "v1"}, {"b2_v5", "v3"}});
        
        EmitAPIForTest results = new EmitAPIForTest();
        Advice advice = AdviceTestUtils.newAdvice().observe("a", "b")
                                                  .unpack("bag1", "c", "d")
                                                  .unpack("bag2", "e", "f")
                                                  .where("\"{}\"==\"{}\"", "c", "f")
                                                  .emit("test1", "a", "b", "c", "d", "e", "f")
                                                  .build(baggage, results);
        
        assertTrue("Expect nothing emitted yet", results.emitted.size() == 0);
        advice.advise("vva", "vvb");
        results.expectTuple("vva", "vvb", "v2", "b1_v5", "b2_v1", "v2");
        results.expectTuple("vva", "vvb", "v1", "b1_v2", "b2_v3", "v1");
        results.check();
        assertTrue("Expect 2 output tuple emitted", results.emitted.size() == 2);           
    }
}
