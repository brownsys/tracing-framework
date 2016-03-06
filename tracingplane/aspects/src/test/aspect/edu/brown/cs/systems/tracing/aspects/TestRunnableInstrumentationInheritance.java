package edu.brown.cs.systems.tracing.aspects;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.baggage.BaggageImpl;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.baggage.Handlers;
import edu.brown.cs.systems.baggage.Handlers.BaggageHandler;
import edu.brown.cs.systems.tracing.aspects.RunnablesCallablesThreads.InstrumentedExecution;
import junit.framework.TestCase;

public class TestRunnableInstrumentationInheritance extends TestCase {
    
    public static class CountingBaggageHandler implements BaggageHandler {
        public int splitCount = 0;
        public CountingBaggageHandler() {
            Handlers.registerBaggageHandler(this);
        }
        public void preSplit(BaggageImpl current) {
            splitCount++;
        }
        public void postSplit(BaggageImpl left, BaggageImpl right) {}
        public void preJoin(BaggageImpl left, BaggageImpl right) {}
        public void postJoin(BaggageImpl current) {}
        public void preSerialize(BaggageImpl baggage) {}
        public void postDeserialize(BaggageImpl baggage) {}
        public void discard() {
            Handlers.unregisterBaggageHandler(this);
        }
    }

    public DetachedBaggage baggage(Object o) {
        assertTrue(o instanceof InstrumentedExecution);
        return ((InstrumentedExecution) o).observeExecutionRunContext().baggage;
    }
    
    public void expect(DetachedBaggage b, String namespace, String key, String value) {
        assertNotNull(b);

        Field implField;
        try {
            implField = DetachedBaggage.class.getDeclaredField("impl");
            implField.setAccessible(true);
            BaggageImpl impl = (BaggageImpl) implField.get(b);
            assertNotNull(impl);
            Set<ByteString> contents = impl.get(ByteString.copyFromUtf8(namespace), ByteString.copyFromUtf8(key));
            assertEquals(1, contents.size());
            assertEquals(ByteString.copyFromUtf8(value), contents.iterator().next());
        } catch (Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
    
    public void discardBaggage() {
        Baggage.discard();
        assertTrue(BaggageContents.isEmpty());
    }
    
    public void checkConstructorForked(Runnable r) {
        assertTrue(r instanceof InstrumentedExecution);
        DetachedBaggage b = baggage(r);
        assertNotNull(b);
        expect(b, "a", "b", "c");
    }
    
    public class RunnableWithConstructor implements Runnable {
        public DetachedBaggage stoppedBaggage = null;
        public RunnableWithConstructor() {}
        public void run() {
            stoppedBaggage = Baggage.stop();
            BaggageContents.add("d", "e", "f");
        }
    }
    
    // Checks that the convenience methods work
    @Test
    public void testSimple() {
        discardBaggage();
        CountingBaggageHandler h = new CountingBaggageHandler();
        BaggageContents.add("a", "b", "c");
        assertEquals(0, h.splitCount);
        RunnableWithConstructor r = new RunnableWithConstructor();
        assertEquals(1, h.splitCount);
        checkConstructorForked(r);
        
        discardBaggage();
        BaggageContents.add("m", "n", "p");
        r.run();
        
        expect(Baggage.stop(), "m", "n", "p");
        expect(r.stoppedBaggage, "a", "b", "c");
        expect(baggage(r), "d", "e", "f");
        assertEquals(1, h.splitCount);
        h.discard();
        
    }
    
    public class RunnableWithoutConstructor implements Runnable {
        public DetachedBaggage stoppedBaggage = null;
        public void run() {
            stoppedBaggage = Baggage.stop();
            BaggageContents.add("d", "e", "f");
        }
    }
    
    // Checks that the convenience methods work
    @Test
    public void testNoConstructor() {
        discardBaggage();
        BaggageContents.add("a", "b", "c");
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        RunnableWithoutConstructor r = new RunnableWithoutConstructor();
        assertEquals(1, h.splitCount);
        checkConstructorForked(r);
        
        discardBaggage();
        BaggageContents.add("m", "n", "p");
        r.run();
        
        expect(Baggage.stop(), "m", "n", "p");
        expect(r.stoppedBaggage, "a", "b", "c");
        expect(baggage(r), "d", "e", "f");
        assertEquals(1, h.splitCount);
        h.discard();
    }
    
    public class RunnableSubclass1 extends RunnableWithConstructor {
    }
    
    // Checks that the convenience methods work
    @Test
    public void testSubclass1Constructor() {
        discardBaggage();
        BaggageContents.add("a", "b", "c");
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        RunnableSubclass1 r = new RunnableSubclass1();
        assertEquals(1, h.splitCount); // If this is greater than 1, it means multiple subclasses saved a copy of bagage
        checkConstructorForked(r);
        
        discardBaggage();
        BaggageContents.add("m", "n", "p");
        r.run();
        
        expect(Baggage.stop(), "m", "n", "p");
        expect(r.stoppedBaggage, "a", "b", "c");
        expect(baggage(r), "d", "e", "f");
        assertEquals(1, h.splitCount);
        h.discard();
    }
    
    public class RunnableSubclass2 extends RunnableWithoutConstructor {
    }
    
    // Checks that the convenience methods work
    @Test
    public void testSubclass2Constructor() {
        discardBaggage();
        BaggageContents.add("a", "b", "c");
        RunnableSubclass2 r = new RunnableSubclass2();
        checkConstructorForked(r);
        
        discardBaggage();
        BaggageContents.add("m", "n", "p");
        r.run();
        
        expect(Baggage.stop(), "m", "n", "p");
        expect(r.stoppedBaggage, "a", "b", "c");
        expect(baggage(r), "d", "e", "f");
    }
    
}
