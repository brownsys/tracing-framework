package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/**
 * Returns a InstrumentedThread whenever a new thread is created. Allows
 * rejoining of XTraceMetadata.
 * 
 * Captures most thread creation, but obviously not from external libraries
 */
public aspect Threads {

    Object around(Runnable target): args(target) && call(Thread.new(Runnable+)) {
        if (target instanceof BaggageAdded) {
            return new WrappedThread(target, (BaggageAdded) target);
        } else {
            return proceed(target);
        }
    }

    Object around(Runnable target, String name): args(target, name) && call(Thread.new(Runnable+, String+)) {
        if (target instanceof BaggageAdded) {
            return new WrappedThread(target, (BaggageAdded) target, name);
        } else {
            return proceed(target, name);
        }
    }

    Object around(ThreadGroup group, Runnable target): args(group, target) && call(Thread.new(ThreadGroup+, Runnable+)) {
        if (target instanceof BaggageAdded) {
            return new WrappedThread(group, target, (BaggageAdded) target);
        } else {
            return proceed(group, target);
        }
    }

    Object around(ThreadGroup group, Runnable target, String name): args(group, target, name) && call(Thread.new(ThreadGroup+, Runnable+, String+)) {
        if (target instanceof BaggageAdded) {
            return new WrappedThread(group, target, (BaggageAdded) target, name);
        } else {
            return proceed(group, target, name);
        }
    }

    Object around(ThreadGroup group, Runnable target, String name, long stackSize): args(group, target, name, stackSize) && call(Thread.new(ThreadGroup+, Runnable+, String+, long)) {
        if (target instanceof BaggageAdded) {
            return new WrappedThread(group, target, (BaggageAdded) target, name, stackSize);
        } else {
            return proceed(group, target, name, stackSize);
        }
    }

    void around(Thread t): target(t) && call(void Thread+.join(..)) {
        proceed(t);
        try {
            XTraceReport.entering(thisJoinPointStaticPart);
            WrappedThread.join(t);
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

}
