package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Methods here are not advised */
public class UnadvisedProxy {
    
    static <V> V futureGet(Future<V> future) throws InterruptedException, ExecutionException {
        return future.get();
    }
    
    static <V> V futureGet(Future<V> future, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

}
