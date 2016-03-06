package edu.brown.cs.systems.tracing.aspects;

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

/** Tests to make sure that threads that wrap runnables have correct baggage added */
public class TestThreads extends TestCase {
    
    @Override
    public void setUp() {
        Baggage.discard();
    }
    
    @Override
    public void tearDown() {
        Baggage.discard();
    }
    
    @Test
    public void testRunnable() {
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        
        Thread t = new Thread(r);
        
        assertTrue(r instanceof InstrumentedExecution);
        assertTrue(t instanceof InstrumentedExecution);
        assertTrue(t instanceof WrappedThread);
        assertNotNull(((InstrumentedExecution) r).observeExecutionRunContext());
        assertNotNull(((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(((InstrumentedExecution) r).observeExecutionRunContext(), ((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(DetachedBaggage.EMPTY, ((InstrumentedExecution) r).observeExecutionRunContext().baggage);
        assertEquals(DetachedBaggage.EMPTY, ((InstrumentedExecution) t).observeExecutionRunContext().baggage);
    }
    
    @Test
    public void testThread() {
        Thread t = new Thread(){
            @Override
            public void run() {
                
            }
        };

        assertTrue(t instanceof InstrumentedExecution);
        assertFalse(t instanceof WrappedThread);
        assertNotNull(((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(DetachedBaggage.EMPTY, ((InstrumentedExecution) t).observeExecutionRunContext().baggage);
    }
    
    @Test
    public void testThreadAndRunnable() {
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        Thread t = new Thread(r){
        };

        assertTrue(r instanceof InstrumentedExecution);
        assertTrue(t instanceof InstrumentedExecution);
        assertFalse(t instanceof WrappedThread);
        assertNotNull(((InstrumentedExecution) r).observeExecutionRunContext());
        assertNotNull(((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(((InstrumentedExecution) r).observeExecutionRunContext(), ((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(DetachedBaggage.EMPTY, ((InstrumentedExecution) r).observeExecutionRunContext().baggage);
        assertEquals(DetachedBaggage.EMPTY, ((InstrumentedExecution) t).observeExecutionRunContext().baggage);
    }
    
    @Test
    public void testThreadAndRunnableContents() {
        BaggageContents.add("a", "b", "c");
        
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        Thread t = new Thread(r){
        };

        assertTrue(r instanceof InstrumentedExecution);
        assertTrue(t instanceof InstrumentedExecution);
        assertFalse(t instanceof WrappedThread);
        assertNotNull(((InstrumentedExecution) r).observeExecutionRunContext());
        assertNotNull(((InstrumentedExecution) t).observeExecutionRunContext());
        assertNotSame(DetachedBaggage.EMPTY, ((InstrumentedExecution) r).observeExecutionRunContext().baggage);
        assertNotSame(DetachedBaggage.EMPTY, ((InstrumentedExecution) t).observeExecutionRunContext().baggage);
        assertEquals(((InstrumentedExecution) r).observeExecutionRunContext(), ((InstrumentedExecution) t).observeExecutionRunContext());
        assertEquals(((InstrumentedExecution) r).observeExecutionRunContext().baggage, ((InstrumentedExecution) t).observeExecutionRunContext().baggage);
    }
    
    @Test
    public void testThreadAndRunnableForkCount() {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        Thread t = new Thread(r){
        };
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
        
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        
        h.discard();
    }
    
    @Test
    public void testThreadAndRunnableForkCount2() {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        Thread t = new Thread(r);
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
        
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        
        h.discard();
    }
    
    public class T1 extends Thread {
        public T1(ThreadGroup grp, Runnable r) {
            super(grp, r);
        }
    }
    
    @Test
    public void testThreadSubclasses() {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Runnable r = new Runnable() {
            public void run() {}
        };
        ThreadGroup grp = new ThreadGroup("testgrp");
        Thread t = new T1(grp, r);
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
        
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        
        h.discard();
    }
    
    @Test
    public void testThreadSubclasses2() throws InterruptedException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Runnable r = new Runnable() {
            public void run() {}
        };
        ThreadGroup grp = new ThreadGroup("testgrp");
        Thread t = new T1(grp, r);
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        t.start();
        
        Baggage.discard();
        
        t.join();
        
        assertFalse(BaggageContents.isEmpty());
        assertEquals(1, BaggageContents.get("a", "b").size());
        
        h.discard();
    }
    
    public class R1 implements Runnable {
        public void run() {
            BaggageContents.add("r1", "a", "b");
        }
    }
    
    public class R2 extends R1 {
        public void run() {
            BaggageContents.add("r2", "a", "b");
            super.run();
            BaggageContents.add("r2", "c", "d");
        }
    }
    
    public class R3 extends R2 {
        public void run() {
            BaggageContents.add("r3", "a", "b");
            super.run();
            BaggageContents.add("r3", "c", "d");
        }
    }
    
    @Test
    public void testRunnableSubclasses() {
        BaggageContents.add("test", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        R3 r3 = new R3();
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Baggage.discard();
        BaggageContents.add("test", "e", "f");
        
        r3.run();
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);

        assertEquals(1, BaggageContents.keys(ByteString.copyFromUtf8("test")).size());
        assertEquals(1, BaggageContents.get("test", "e").size());
        assertEquals(ByteString.copyFromUtf8("f"), BaggageContents.get("test", "e").iterator().next());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r1")).size());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r2")).size());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r3")).size());
        
        assertTrue(r3 instanceof InstrumentedExecution);
        ((InstrumentedExecution) r3).getOrCreateExecutionRunContext().JoinSavedBaggageIfPossible(null);

        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        
        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("test")).size());
        assertEquals(1, BaggageContents.get("test", "e").size());
        assertEquals(1, BaggageContents.get("test", "b").size());
        assertEquals(ByteString.copyFromUtf8("f"), BaggageContents.get("test", "e").iterator().next());
        assertEquals(ByteString.copyFromUtf8("c"), BaggageContents.get("test", "b").iterator().next());
        assertEquals(1, BaggageContents.keys(ByteString.copyFromUtf8("r1")).size());
        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("r2")).size());
        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("r3")).size());
        
        h.discard();
    }
    
    @Test
    public void testRunnableSubclassesWithThread() throws InterruptedException {
        BaggageContents.add("test", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Thread t = new Thread(new R3());
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        Baggage.discard();
        BaggageContents.add("test", "e", "f");
        
        t.start();
        while (t.isAlive()) {
            // Spin wait
        }
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);

        assertEquals(1, BaggageContents.keys(ByteString.copyFromUtf8("test")).size());
        assertEquals(1, BaggageContents.get("test", "e").size());
        assertEquals(ByteString.copyFromUtf8("f"), BaggageContents.get("test", "e").iterator().next());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r1")).size());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r2")).size());
        assertEquals(0, BaggageContents.keys(ByteString.copyFromUtf8("r3")).size());
        
        t.join();

        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);

        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("test")).size());
        assertEquals(1, BaggageContents.get("test", "e").size());
        assertEquals(1, BaggageContents.get("test", "b").size());
        assertEquals(ByteString.copyFromUtf8("f"), BaggageContents.get("test", "e").iterator().next());
        assertEquals(ByteString.copyFromUtf8("c"), BaggageContents.get("test", "b").iterator().next());
        assertEquals(1, BaggageContents.keys(ByteString.copyFromUtf8("r1")).size());
        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("r2")).size());
        assertEquals(2, BaggageContents.keys(ByteString.copyFromUtf8("r3")).size());
        
        h.discard();
    }
    

    
    public static class CountingBaggageHandler implements BaggageHandler {
        public int splitCount = 0;
        public int joinCount = 0;
        public CountingBaggageHandler() {
            Handlers.registerBaggageHandler(this);
        }
        public void preSplit(BaggageImpl current) {
            splitCount++;
        }
        public void postSplit(BaggageImpl left, BaggageImpl right) {}
        public void preJoin(BaggageImpl left, BaggageImpl right) {}
        public void postJoin(BaggageImpl current) {
            joinCount++;
        }
        public void preSerialize(BaggageImpl baggage) {}
        public void postDeserialize(BaggageImpl baggage) {}
        public void discard() {
            Handlers.unregisterBaggageHandler(this);
        }
    }

}
