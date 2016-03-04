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
        V v = wrapped.get();
        Baggage.join(instrumented.getSavedBaggage());
        return v;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        V v = wrapped.get(timeout, unit);
        Baggage.join(instrumented.getSavedBaggage());
        return v;
    }

}
