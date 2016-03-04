package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.brown.cs.systems.baggage.Baggage;

public class WrappedFuture<V> implements Future<V> {

    private final Future<V> wrapped;
    private final BaggageAdded instrumented;

    public WrappedFuture(Future<V> wrapped, BaggageAdded instrumented) {
        this.wrapped = wrapped;
        this.instrumented = instrumented;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrapped.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return wrapped.isCancelled();
    }

    public boolean isDone() {
        return wrapped.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return UnadvisedProxy.futureGet(wrapped);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return UnadvisedProxy.futureGet(wrapped, timeout, unit);
    }

    /** Joins the specified future, which could be a future wrapper */
    public static void join(Future f) {
        if (f instanceof WrappedFuture) {
            Baggage.join(((WrappedFuture) f).instrumented.getSavedBaggage());
        } else if (f instanceof BaggageAdded) {
            Baggage.join(((BaggageAdded) f).getSavedBaggage());
        }
    }

}
