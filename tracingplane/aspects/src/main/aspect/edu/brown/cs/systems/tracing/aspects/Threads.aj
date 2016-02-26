package edu.brown.cs.systems.tracing.aspects;

/**
 * Returns a InstrumentedThread whenever a new thread is created. Allows
 * rejoining of XTraceMetadata.
 * 
 * Captures most thread creation, but obviously not from external libraries
 */
public aspect Threads {

    Object around(Runnable target): args(target) && call(Thread.new(Runnable+)) {
        if (target instanceof InstrumentedRunnable) {
            return new InstrumentedThread(target, (InstrumentedRunnable) target);
        } else {
            return proceed(target);
        }
    }

    Object around(Runnable target, String name): args(target, name) && call(Thread.new(Runnable+, String+)) {
        if (target instanceof InstrumentedRunnable) {
            return new InstrumentedThread(target, (InstrumentedRunnable) target, name);
        } else {
            return proceed(target, name);
        }
    }

    Object around(ThreadGroup group, Runnable target): args(group, target) && call(Thread.new(ThreadGroup+, Runnable+)) {
        if (target instanceof InstrumentedRunnable) {
            return new InstrumentedThread(group, target, (InstrumentedRunnable) target);
        } else {
            return proceed(group, target);
        }
    }

    Object around(ThreadGroup group, Runnable target, String name): args(group, target, name) && call(Thread.new(ThreadGroup+, Runnable+, String+)) {
        if (target instanceof InstrumentedRunnable) {
            return new InstrumentedThread(group, target, (InstrumentedRunnable) target, name);
        } else {
            return proceed(group, target, name);
        }
    }

    Object around(ThreadGroup group, Runnable target, String name, long stackSize): args(group, target, name, stackSize) && call(Thread.new(ThreadGroup+, Runnable+, String+, long)) {
        if (target instanceof InstrumentedRunnable) {
            return new InstrumentedThread(group, target, (InstrumentedRunnable) target, name, stackSize);
        } else {
            return proceed(group, target, name, stackSize);
        }
    }

    void around(Thread t): target(t) && call(void Thread+.join(..)) {
        proceed(t);
        InstrumentedThread.join(t);
    }

}
