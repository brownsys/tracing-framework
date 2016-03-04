package edu.brown.cs.systems.tracing.aspects;

import java.lang.reflect.Field;

import edu.brown.cs.systems.baggage.Baggage;

/**
 * Propagates metadata to new threads. This class does NOT get the
 * XTraceRunnable interface extension
 */
public class WrappedThread extends Thread {

    private final BaggageAdded t2;

    public WrappedThread(Runnable t1, BaggageAdded t2) {
        super(t1);
        this.t2 = t2;
    }

    public WrappedThread(Runnable t1, BaggageAdded t2, String name) {
        super(t1, name);
        this.t2 = t2;
    }

    public WrappedThread(ThreadGroup group, Runnable t1, BaggageAdded t2) {
        super(group, t1);
        this.t2 = t2;
    }

    public WrappedThread(ThreadGroup group, Runnable t1, BaggageAdded t2, String name) {
        super(group, t1, name);
        this.t2 = t2;
    }

    public WrappedThread(ThreadGroup group, Runnable t1, BaggageAdded t2, String name, long stackSize) {
        super(group, t1, name, stackSize);
        this.t2 = t2;
    }

    /**
     * Joins the specified thread, which could be a thread wrapper, could be a
     * regular thread wrapping an xtrace runnable, and could be an xtrace
     * runnable itself
     */
    public static void join(Thread t) {
        if (t instanceof WrappedThread) {
            Baggage.join(((WrappedThread) t).t2.getSavedBaggage());
        } else if (t instanceof BaggageAdded) {
            Baggage.join(((BaggageAdded) t).getSavedBaggage());
        } else {
            try {
                Field runnableField = Thread.class.getDeclaredField("target");
                runnableField.setAccessible(true);
                Runnable target = (Runnable) runnableField.get(t);
                if (target != null && target instanceof BaggageAdded) {
                    Baggage.join(((BaggageAdded) target).getSavedBaggage());
                }
            } catch (Exception e) {
                // swallow exceptions
            }
        }
    }

}
