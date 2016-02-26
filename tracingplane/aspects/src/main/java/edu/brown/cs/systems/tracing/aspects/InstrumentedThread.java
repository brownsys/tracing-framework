package edu.brown.cs.systems.tracing.aspects;

import java.lang.reflect.Field;

/**
 * Propagates metadata to new threads. This class does NOT get the
 * XTraceRunnable interface extension
 */
public class InstrumentedThread extends Thread {

    private final InstrumentedRunnable t2;

    public InstrumentedThread(Runnable t1, InstrumentedRunnable t2) {
        super(t1);
        this.t2 = t2;
    }

    public InstrumentedThread(Runnable t1, InstrumentedRunnable t2, String name) {
        super(t1, name);
        this.t2 = t2;
    }

    public InstrumentedThread(ThreadGroup group, Runnable t1, InstrumentedRunnable t2) {
        super(group, t1);
        this.t2 = t2;
    }

    public InstrumentedThread(ThreadGroup group, Runnable t1, InstrumentedRunnable t2, String name) {
        super(group, t1, name);
        this.t2 = t2;
    }

    public InstrumentedThread(ThreadGroup group, Runnable t1, InstrumentedRunnable t2, String name, long stackSize) {
        super(group, t1, name, stackSize);
        this.t2 = t2;
    }

    /**
     * Joins the specified thread, which could be a thread wrapper, could be a
     * regular thread wrapping an xtrace runnable, and could be an xtrace
     * runnable itself
     */
    public static void join(Thread t) {
        if (t instanceof InstrumentedThread) {
            ((InstrumentedThread) t).t2.joinRunendContext();
        } else if (t instanceof InstrumentedRunnable) {
            ((InstrumentedRunnable) t).joinRunendContext();
        } else {
            try {
                Field runnableField = Thread.class.getDeclaredField("target");
                runnableField.setAccessible(true);
                Runnable target = (Runnable) runnableField.get(t);
                if (target != null && target instanceof InstrumentedRunnable) {
                    ((InstrumentedRunnable) target).joinRunendContext();
                }
            } catch (Exception e) {
                // swallow exceptions
            }
        }
    }

}
