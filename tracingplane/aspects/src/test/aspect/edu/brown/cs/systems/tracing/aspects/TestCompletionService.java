package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import edu.brown.cs.systems.tracing.aspects.RunnablesCallablesThreads.InstrumentedExecution;
import junit.framework.TestCase;

public class TestCompletionService extends TestCase {
    
    public static class WithInstrumentation implements Callable<String> {
        @Override
        public String call() throws Exception {
            return "HelloWorld";
        }
    }
    
    @BaggageInheritanceDisabled
    public static class WithoutInstrumentation implements Callable<String> {
        @Override
        public String call() throws Exception {
            return "GoodbyeCruelWorld";
        }
    }

    @Test
    public void testCompletionServiceSubmit() throws InterruptedException {
        Executor x = Executors.newSingleThreadExecutor();
        CompletionService<String> s = new ExecutorCompletionService<String>(x);
        
        Callable<String> c1 = new WithInstrumentation();
        Callable<String> c2 = new WithoutInstrumentation();
        
        assertTrue(c1 instanceof InstrumentedExecution);
        assertFalse(c2 instanceof InstrumentedExecution);
        
        Future<String> f1 = s.submit(c1);
        
        assertTrue(f1 instanceof WrappedFuture);
        assertEquals(((InstrumentedExecution) c1).observeExecutionRunContext(), ((WrappedFuture) f1).instrumented);
        
        Future<String> f3 = s.submit(c2);
        
        assertFalse(f3 instanceof WrappedFuture);
        
    }

    @Test
    public void testCompletionServiceTake() throws InterruptedException {
        Executor x = Executors.newSingleThreadExecutor();
        CompletionService<String> s = new ExecutorCompletionService<String>(x);
        
        Callable<String> c1 = new WithInstrumentation();
        Callable<String> c2 = new WithoutInstrumentation();
        
        assertTrue(c1 instanceof InstrumentedExecution);
        assertFalse(c2 instanceof InstrumentedExecution);
        
        Future<String> f1 = s.submit(c1);
        
        assertTrue(f1 instanceof WrappedFuture);
        assertEquals(((InstrumentedExecution) c1).observeExecutionRunContext(), ((WrappedFuture) f1).instrumented);
        
        Future<String> f2 = s.take();
        
        assertTrue(f2 instanceof WrappedFuture);
        assertTrue(f1 == f2);
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
        assertEquals(((InstrumentedExecution) c1).observeExecutionRunContext(), ((WrappedFuture) f2).instrumented);
        
        Future<String> f3 = s.submit(c2);
        
        assertFalse(f3 instanceof WrappedFuture);
        
        Future<String> f4 = s.take();
        
        assertFalse(f4 instanceof WrappedFuture);
        
    }
    
}
