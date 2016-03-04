package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.brown.cs.systems.baggage.DetachedBaggage;

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
        return wrapped.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapped.get(timeout, unit);
    }

    /** Try to get the baggage saved with this future */
    public static DetachedBaggage getSavedBaggage(Future f) {
        if (f instanceof WrappedFuture) {
            return ((WrappedFuture) f).instrumented.getSavedBaggage();
        } else if (f instanceof BaggageAdded) {
            return ((BaggageAdded) f).getSavedBaggage();
        } else {
            return null;
        }
    }

}
