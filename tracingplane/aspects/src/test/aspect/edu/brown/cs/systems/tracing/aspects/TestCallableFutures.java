package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.tracing.aspects.RunnablesCallablesThreads.InstrumentedExecution;
import edu.brown.cs.systems.tracing.aspects.TestThreads.CountingBaggageHandler;
import junit.framework.TestCase;

public class TestCallableFutures extends TestCase {
    
    @Override
    public void setUp() {
        Baggage.discard();
    }
    
    @Override
    public void tearDown() {
        Baggage.discard();
    }
    
    public class C1 implements Callable<String> {
        public String call() throws Exception {
            BaggageContents.add("d", "e", "f");
            return "hi";
        }
    }
    
    @Test
    public void testCallable() throws InterruptedException, ExecutionException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        C1 c1 = new C1();
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        ExecutorService x = Executors.newFixedThreadPool(1);
        Future<String> f = x.submit(c1);
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        assertEquals(1, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        C1 c2 = new C1();
        f = x.submit(c2);
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(0, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        f = x.submit(new Callable<String>() {
            public String call() throws Exception {
                return "bye";
            }
        });
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("bye", f.get());
        assertTrue(BaggageContents.isEmpty());
        
        x.shutdown();
        h.discard();
    }
    
    public class R1 implements Runnable {
        public void run() {
            BaggageContents.add("d", "e", "f");
        }
    }
    
    @Test
    public void testRunnable() throws InterruptedException, ExecutionException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        R1 r1 = new R1();
        
        assertTrue(r1 instanceof InstrumentedExecution);
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        ExecutorService x = Executors.newFixedThreadPool(1);
        Future<?> f = x.submit(r1);
        
        assertTrue(f instanceof WrappedFuture);
        
        assertNull(f.get());
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        assertEquals(1, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        R1 r2 = new R1();
        f = x.submit(r2);
        
        assertTrue(f instanceof WrappedFuture);

        assertNull(f.get());
        assertEquals(0, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        f = x.submit(new Runnable() {
            public void run() {}
        });
        
        assertTrue(f instanceof WrappedFuture);

        assertNull(f.get());
        assertTrue(BaggageContents.isEmpty());

        x.shutdown();
        h.discard();
    }
    
    @Test
    public void testRunnableWithResult() throws InterruptedException, ExecutionException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        R1 r1 = new R1();
        
        assertTrue(r1 instanceof InstrumentedExecution);
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        ExecutorService x = Executors.newFixedThreadPool(1);
        Future<String> f = x.submit(r1, "hi");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        assertEquals(1, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        R1 r2 = new R1();
        f = x.submit(r2, "hi");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(0, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        f = x.submit(new Runnable() {
            public void run() {}
        }, "bye");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("bye", f.get());
        assertTrue(BaggageContents.isEmpty());
        
        x.shutdown();
        h.discard();
    }
    
    @Test
    public void testCallableCompletionService() throws InterruptedException, ExecutionException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        C1 c1 = new C1();
        
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);
        
        ExecutorService p = Executors.newFixedThreadPool(1);
        CompletionService<String> x = new ExecutorCompletionService<String>(p);
        Future<String> f = x.submit(c1);
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        assertEquals(1, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        C1 c2 = new C1();
        f = x.submit(c2);
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(0, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        f = x.submit(new Callable<String>() {
            public String call() throws Exception {
                return "bye";
            }
        });
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("bye", f.get());
        assertTrue(BaggageContents.isEmpty());
        
        p.shutdown();
        h.discard();
    }
    
    @Test
    public void testRunnableWithResultCompletionService() throws InterruptedException, ExecutionException {
        BaggageContents.add("a", "b", "c");
        
        CountingBaggageHandler h = new CountingBaggageHandler();
        assertEquals(0, h.splitCount);
        assertEquals(0, h.joinCount);
        
        R1 r1 = new R1();
        
        assertTrue(r1 instanceof InstrumentedExecution);
        assertEquals(1, h.splitCount);
        assertEquals(0, h.joinCount);

        ExecutorService p = Executors.newFixedThreadPool(1);
        CompletionService<String> x = new ExecutorCompletionService<String>(p);
        Future<String> f = x.submit(r1, "hi");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(1, h.splitCount);
        assertEquals(1, h.joinCount);
        assertEquals(1, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        R1 r2 = new R1();
        f = x.submit(r2, "hi");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("hi", f.get());
        assertEquals(0, BaggageContents.get("a", "b").size());
        assertEquals(1, BaggageContents.get("d", "e").size());
        
        Baggage.discard();
        
        f = x.submit(new Runnable() {
            public void run() {}
        }, "bye");
        
        assertTrue(f instanceof WrappedFuture);
        
        assertEquals("bye", f.get());
        assertTrue(BaggageContents.isEmpty());
        
        p.shutdown();        
        h.discard();
    }

}
