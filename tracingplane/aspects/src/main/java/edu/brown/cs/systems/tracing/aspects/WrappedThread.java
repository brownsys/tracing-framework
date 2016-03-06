package edu.brown.cs.systems.tracing.aspects;

/**
 * Propagates metadata to new threads. This class gets the InstrumentedExecution interface
 */
public class WrappedThread extends Thread {

    public WrappedThread(Runnable t1) {
        super(t1);
    }

    public WrappedThread(Runnable t1, String name) {
        super(t1, name);
    }

    public WrappedThread(ThreadGroup group, Runnable t1) {
        super(group, t1);
    }

    public WrappedThread(ThreadGroup group, Runnable t1, String name) {
        super(group, t1, name);
    }

    public WrappedThread(ThreadGroup group, Runnable t1, String name, long stackSize) {
        super(group, t1, name, stackSize);
    }

}
