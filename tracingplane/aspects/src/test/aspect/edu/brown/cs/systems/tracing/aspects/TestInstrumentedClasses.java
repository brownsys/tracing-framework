package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import junit.framework.TestCase;

/** Tests to see whether wrapper classes are correctly created */
public class TestInstrumentedClasses extends TestCase {
    
    private static class TestCallable<T> implements Callable<T> {
        public T call() {
            return null;
        }
    }
    
    private static class TestCallable2<T> extends TestCallable<T> {
    }
    
    private static class TestCallable3<T> extends TestCallable<T> {
        public T call() {
            return null;
        }
    }
    
    @BaggageInheritanceDisabled
    private static class TestNonTraceCallable<T> implements Callable<T> {
        public T call(){
            return null;
        }
    }
    
    private static class TestNonTraceCallable2<T> extends TestNonTraceCallable<T> {
    }
    
    private static class TestNonTraceCallable3<T> extends TestNonTraceCallable<T> {
        public T call() {
            return null;
        }
    }

    private static class TestRunnable implements Runnable {
        public void run() {
        }
    }

    private static class TestRunnable2 extends TestRunnable {
    }

    private static class TestThread extends Thread {
    }

    private static class TestThread2 extends TestThread {
    }
    
    @BaggageInheritanceDisabled
    private static class TestNonTraceRunnable implements Runnable {
        public void run() {
        }
    }

    private static class TestNonTraceRunnable2 extends TestNonTraceRunnable{}
    
    @BaggageInheritanceDisabled
    private static class TestNonTraceRunnable3 extends TestNonTraceRunnable{}

    @Test
    public void testRunnables() {
        Runnable r1 = new Runnable() {
            public void run() {
            }
        };

        Runnable r2 = new TestRunnable();
        Runnable r3 = new TestRunnable2();
        
        Runnable r4 = new TestNonTraceRunnable();
        Runnable r5 = new TestNonTraceRunnable2();
        Runnable r6 = new TestNonTraceRunnable3();

        Thread t1 = new Thread() {
        };
        Thread t2 = new TestThread();
        Thread t3 = new TestThread2();

        Thread t4 = new Thread(r1);
        Thread t5 = new Thread(r2);
        Thread t6 = new Thread(r3);
        Thread t7 = new Thread(t1);
        Thread t8 = new Thread(t2);
        Thread t9 = new Thread(t3);

        assertTrue(r1 instanceof BaggageAdded);
        assertTrue(r2 instanceof BaggageAdded);
        assertTrue(r3 instanceof BaggageAdded);
        assertTrue(t1 instanceof BaggageAdded);
        assertTrue(t2 instanceof BaggageAdded);
        assertTrue(t3 instanceof BaggageAdded);
        assertFalse(r4 instanceof BaggageAdded);
        assertFalse(r5 instanceof BaggageAdded);
        assertFalse(r6 instanceof BaggageAdded);

        assertTrue(t4 instanceof WrappedThread);
        assertTrue(t5 instanceof WrappedThread);
        assertTrue(t6 instanceof WrappedThread);
        assertTrue(t7 instanceof WrappedThread);
        assertTrue(t8 instanceof WrappedThread);
        assertTrue(t9 instanceof WrappedThread);

        Callable<Object> c1 = new TestCallable<>();
        Callable<Object> c2 = new TestCallable2<>();
        Callable<Object> c3 = new TestCallable3<>();
        Callable<Object> c4 = new TestNonTraceCallable<>();
        Callable<Object> c5 = new TestNonTraceCallable2<>();
        Callable<Object> c6 = new TestNonTraceCallable3<>();

        assertTrue(c1 instanceof BaggageAdded);
        assertTrue(c2 instanceof BaggageAdded);
        assertTrue(c3 instanceof BaggageAdded);
        assertFalse(c4 instanceof BaggageAdded);
        assertFalse(c5 instanceof BaggageAdded);
        assertFalse(c6 instanceof BaggageAdded);
        
        ExecutorService s = Executors.newFixedThreadPool(1);

        Future<Object> f1 = s.submit(c1);
        Future<Object> f2 = s.submit(c2);
        Future<Object> f3 = s.submit(c3);
        Future<Object> f4 = s.submit(c4);
        Future<Object> f5 = s.submit(c5);
        Future<Object> f6 = s.submit(c6);
        Future<?> f7 = s.submit(r1);
        Future<?> f8 = s.submit(r2);
        Future<?> f9 = s.submit(r3);
        Future<?> f10 = s.submit(r4);
        Future<?> f11 = s.submit(r5);
        Future<?> f12 = s.submit(r6);

        assertTrue(f1 instanceof WrappedFuture);
        assertTrue(f2 instanceof WrappedFuture);
        assertTrue(f3 instanceof WrappedFuture);
        assertFalse(f4 instanceof WrappedFuture);
        assertFalse(f5 instanceof WrappedFuture);
        assertFalse(f6 instanceof WrappedFuture);
        assertTrue(f7 instanceof WrappedFuture);
        assertTrue(f8 instanceof WrappedFuture);
        assertTrue(f9 instanceof WrappedFuture);
        assertFalse(f10 instanceof WrappedFuture);
        assertFalse(f11 instanceof WrappedFuture);
        assertFalse(f12 instanceof WrappedFuture);

    }

}
