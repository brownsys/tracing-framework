package edu.brown.cs.systems.tracing.aspects;

import org.junit.Test;

import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import junit.framework.TestCase;

/** Tests to see whether wrapper classes are correctly created */
public class TestInstrumentedClasses extends TestCase {

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

        assertTrue(t4 instanceof InstrumentedThread);
        assertTrue(t5 instanceof InstrumentedThread);
        assertTrue(t6 instanceof InstrumentedThread);
        assertTrue(t7 instanceof InstrumentedThread);
        assertTrue(t8 instanceof InstrumentedThread);
        assertTrue(t9 instanceof InstrumentedThread);

    }

}
