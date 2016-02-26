package edu.brown.cs.systems.pivottracing.agent;

import java.lang.instrument.UnmodifiableClassException;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicInstrumentation;
import edu.brown.cs.systems.dynamicinstrumentation.DynamicManager;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Builder;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIImplForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.EmitAPIForTest;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.TracepointsTestUtils;
import edu.brown.cs.systems.pivottracing.dynamicinstrumentation.MethodRewriteModification;
import javassist.CannotCompileException;
import junit.framework.TestCase;

public class TestMethodRewriteModification extends TestCase {
    
    @Override
    public void tearDown() throws CannotCompileException, UnmodifiableClassException {
        DynamicInstrumentation.get().clear().install();
    }

    private static class PTAgentForTest {

        public final EmitAPIForTest results = new EmitAPIForTest();
        public final BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();
        public final DynamicManager dynamic = DynamicInstrumentation.get();
        public final PTAgent agent = new PTAgent(baggage, results, dynamic);

        public PTAgentForTest() {
            PrivilegedProxy.Register(agent);
            dynamic.clear();
        }
    }

    private static class AdviceImplForTest implements Advice {

        final List<List<Object>> advised = Lists.newArrayList();

        @Override
        public void advise(Object... values) {
            advised.add(Lists.newArrayList(values));
        }

        public void expectSize(int expectedSize) {
            TestCase.assertEquals(expectedSize, advised.size());
        }

        public void expect(int index, Object... values) {
            TestCase.assertEquals(Lists.newArrayList(values), advised.get(index));
        }

        public void expect(List<Object[]> tuples) {
            List<List<Object>> expected = Lists.newArrayList();
            for (Object[] tuple : tuples) {
                expected.add(Lists.newArrayList(tuple));
            }
            expect2(expected);
        }

        public void expect2(List<List<Object>> tuples) {
            TestCase.assertEquals(tuples, advised);
        }

    }

    public void method(String msg) {}

    public void varargsMethod(String first, String... multiargs) {}

    public void collectionMethod(List<String> strings, String second) {}

    public void mixedMethod(String first, String[] second, List<String> third, String fourth) {

    }

    public void primitive(int a) {}

    /** Simple method rewrite */
    @Test
    public void testMethodRewriteModification()
            throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "method");

        // Invoke method - should do nothing before rewrite
        method("hi");
        advice.expectSize(0);

        // Rewrite method
        MethodRewriteModification mod = new MethodRewriteModification(t1, lookupId);
        test.agent.dynamic.clear().add(mod).install();
        advice.expectSize(0);

        String[] testStrings = { "hello", "a", "bbbbb", "c", "bbbbb", "xyz", "#(@F", "hello" };
        int count = 0;
        for (String testString : testStrings) {
            advice.expectSize(count++);
            method(testString);
            advice.expectSize(count);
            for (int i = 0; i < count; i++) {
                advice.expect(i, testStrings[i]);
            }
        }
    }

    /** Simple method rewrite with primitive args, make sure they are boxed */
    @Test
    public void testPrimitiveRewriteModification()
            throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "primitive");

        // Invoke method - should do nothing before rewrite
        method("hi");
        advice.expectSize(0);

        // Rewrite method
        MethodRewriteModification mod = new MethodRewriteModification(t1, lookupId);
        test.agent.dynamic.add(mod).install();;
        advice.expectSize(0);

        int count = 0;
        for (int i = 0; i < 100; i++) {
            advice.expectSize(count++);
            primitive(i * 2);
            advice.expectSize(count);
            for (int j = 0; j < count; j++) {
                advice.expect(j, j * 2);
            }
        }
    }

    /** Rewrite a method with array args, configure advice invocation as array */
    @Test
    public void testArrayRewriteModification()
            throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "varargsMethod");

        // Invoke method - should do nothing before rewrite
        varargsMethod("hi");
        advice.expectSize(0);

        // Rewrite method
        MethodRewriteModification mod = new MethodRewriteModification(t1, lookupId);

        test.agent.dynamic.clear().add(mod).install();
        advice.expectSize(0);

        // Invoke a few times
        varargsMethod("beeee", "hello?");
        advice.expectSize(1);
        advice.expect(0, "beeee", "hello?");

        varargsMethod("one", "two", "three");
        advice.expectSize(3);
        advice.expect(0, "beeee", "hello?");
        advice.expect(1, "one", "two");
        advice.expect(2, "one", "three");

        varargsMethod("bad");
        advice.expectSize(3);
        advice.expect(0, "beeee", "hello?");
        advice.expect(1, "one", "two");
        advice.expect(2, "one", "three");

        varargsMethod("one", "one", "one");
        advice.expectSize(5);
        advice.expect(0, "beeee", "hello?");
        advice.expect(1, "one", "two");
        advice.expect(2, "one", "three");
        advice.expect(3, "one", "one");
        advice.expect(4, "one", "one");

        // Reset the class
        test.agent.dynamic.reset(getClass().getName()).install();

        varargsMethod("one", "two", "three");
        varargsMethod("beeee", "hello?");
        varargsMethod("no", "way", "mate");
        advice.expectSize(5);
        advice.expect(0, "beeee", "hello?");
        advice.expect(1, "one", "two");
        advice.expect(2, "one", "three");
        advice.expect(3, "one", "one");
        advice.expect(4, "one", "one");
    }

    /** Rewrite a method with collection args, configure advice invocation as collection */
    @Test
    public void testCollectionRewriteModification()
            throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "collectionMethod");

        // Invoke method - should do nothing before rewrite
        collectionMethod(Lists.newArrayList("beeee", "hi"), "aaa");
        advice.expectSize(0);

        // Instrument method
        MethodRewriteModification mod = new MethodRewriteModification(t1, lookupId);

        test.agent.dynamic.clear().add(mod).install();
        advice.expectSize(0);

        collectionMethod(Lists.newArrayList("beeee"), "hello?");
        advice.expectSize(1);
        advice.expect(0, "beeee", "hello?");

        collectionMethod(Lists.<String> newArrayList(), "three");
        advice.expectSize(1);
        advice.expect(0, "beeee", "hello?");

        collectionMethod(Lists.newArrayList("one", "two"), "three");
        advice.expectSize(3);
        advice.expect(1, "one", "three");
        advice.expect(2, "two", "three");
    }

    /** Test instrumenting advice with multiple array / collection args */
    @Test
    public void testArrayRewriteModificationMulti()
            throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "mixedMethod");

        // Invoke method - should do nothing before rewrite
        mixedMethod("blah", new String[] { "s1", "s2", "s3" }, Lists.newArrayList("yummy", "food"), "byeee");
        advice.expectSize(0);

        // Instrument method
        MethodRewriteModification mod = new MethodRewriteModification(t1, lookupId);

        test.agent.dynamic.clear().add(mod).install();
        advice.expectSize(0);

        mixedMethod("blah", new String[] { "s1", "s2", "s3" }, Lists.newArrayList("yummy", "food"), "byeee");
        advice.expectSize(6);
        advice.expect(0, "blah", "s1", "yummy", "byeee");
        advice.expect(1, "blah", "s2", "yummy", "byeee");
        advice.expect(2, "blah", "s3", "yummy", "byeee");
        advice.expect(3, "blah", "s1", "food", "byeee");
        advice.expect(4, "blah", "s2", "food", "byeee");
        advice.expect(5, "blah", "s3", "food", "byeee");

        mixedMethod("blah", new String[] {}, Lists.newArrayList("yummy", "food"), "byeee");
        advice.expectSize(6);
        advice.expect(0, "blah", "s1", "yummy", "byeee");
        advice.expect(1, "blah", "s2", "yummy", "byeee");
        advice.expect(2, "blah", "s3", "yummy", "byeee");
        advice.expect(3, "blah", "s1", "food", "byeee");
        advice.expect(4, "blah", "s2", "food", "byeee");
        advice.expect(5, "blah", "s3", "food", "byeee");

        mixedMethod("blah", new String[] { "s1", "s2", "s3" }, Lists.<String> newArrayList(), "byeee");
        advice.expectSize(6);
        advice.expect(0, "blah", "s1", "yummy", "byeee");
        advice.expect(1, "blah", "s2", "yummy", "byeee");
        advice.expect(2, "blah", "s3", "yummy", "byeee");
        advice.expect(3, "blah", "s1", "food", "byeee");
        advice.expect(4, "blah", "s2", "food", "byeee");
        advice.expect(5, "blah", "s3", "food", "byeee");

        mixedMethod("blah", new String[] { "s4" }, Lists.newArrayList("meee"), "yay");
        advice.expectSize(7);
        advice.expect(0, "blah", "s1", "yummy", "byeee");
        advice.expect(1, "blah", "s2", "yummy", "byeee");
        advice.expect(2, "blah", "s3", "yummy", "byeee");
        advice.expect(3, "blah", "s1", "food", "byeee");
        advice.expect(4, "blah", "s2", "food", "byeee");
        advice.expect(5, "blah", "s3", "food", "byeee");
        advice.expect(6, "blah", "s4", "meee", "yay");
    }

    @Test
    public void testBadTracepoint() throws ClassNotFoundException, UnmodifiableClassException, CannotCompileException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "method");
        MethodTracepointSpec tbad = MethodTracepointSpec.newBuilder(t1).setMethodName("badmethod").build();

        // Invoke method - should do nothing before rewrite
        method("hi");
        advice.expectSize(0);

        // Rewrite method
        MethodRewriteModification mod = new MethodRewriteModification(tbad, lookupId);

        test.agent.dynamic.clear().add(mod).install();
        advice.expectSize(0);

        method("hi");
        advice.expectSize(0);
    }

    @Test
    public void testBadTracepoint2() throws ClassNotFoundException, UnmodifiableClassException {
        // Create and register dummy advice
        PTAgentForTest test = new PTAgentForTest();
        AdviceImplForTest advice = new AdviceImplForTest();
        int lookupId = test.agent.adviceManager.register(advice);

        // Method under test
        MethodTracepointSpec t1 = TracepointsTestUtils.getMethodSpec(getClass(), "method");
        Builder tbad = MethodTracepointSpec.newBuilder(t1);
        tbad.addAdviceArgBuilder().setLiteral("definitely a bad literal");

        // Invoke method - should do nothing before rewrite
        method("hi");
        advice.expectSize(0);

        // Rewrite method
        MethodRewriteModification mod = new MethodRewriteModification(tbad.build(), lookupId);
        boolean expectedExceptionThrown = false;
        try {
            test.agent.dynamic.clear().add(mod).install();
        } catch (CannotCompileException e) {
            expectedExceptionThrown = true;
        } finally {
            if (!expectedExceptionThrown) {
                fail();
            }
        }
        advice.expectSize(0);

        method("hi");
        advice.expectSize(0);
    }

}
