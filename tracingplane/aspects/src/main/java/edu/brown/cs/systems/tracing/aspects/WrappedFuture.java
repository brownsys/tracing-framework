package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;

public class WrappedFuture<V> implements ListenableFuture<V> {

    public final Future<V> wrapped;
    public final RunContext instrumented;

    public WrappedFuture(Future<V> wrapped, RunContext instrumented) {
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
    
    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof WrappedFuture) {
            return ((WrappedFuture) other).wrapped.equals(wrapped);
        } else {
            return wrapped.equals(other);
        }
    }
    
    @Override
    public void addListener(final Runnable r, Executor e) {
        ((ListenableFuture<V>) wrapped).addListener(new Runnable() {
            public void run() {
                instrumented.BeginExecution(null);
                r.run();
                instrumented.EndExecution(null);
            }
        }, e);
    }

}
