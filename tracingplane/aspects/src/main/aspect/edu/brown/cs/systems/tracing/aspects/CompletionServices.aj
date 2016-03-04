package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

public aspect CompletionServices {
    
    Future around(Callable task): args(task) && call(Future CompletionService+.submit(Callable+)) {
        if (task instanceof BaggageAdded) {
            return new WrappedFuture(proceed(task), (BaggageAdded) task);
        } else {
            return proceed(task);
        }
    }
    
    Future around(Runnable task): args(task) && call(Future CompletionService+.submit(Runnable+, Object+)) {
        if (task instanceof BaggageAdded) {
            return new WrappedFuture(proceed(task), (BaggageAdded) task);
        } else {
            return proceed(task);
        }
    }

}
