package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

public aspect ExecutorServices {
    
    Future around(Callable task): args(task) && call(Future ExecutorService+.submit(Callable+)) {
        if (task instanceof BaggageAdded) {
            return new WrappedFuture(proceed(task), (BaggageAdded) task);
        } else {
            return proceed(task);
        }
    }
    
    Future around(Runnable task): args(task) && call(Future ExecutorService+.submit(Runnable+,..)) {
        if (task instanceof BaggageAdded) {
            return new WrappedFuture(proceed(task), (BaggageAdded) task);
        } else {
            return proceed(task);
        }
    }

}
