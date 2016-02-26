package edu.brown.cs.systems.retro.throttling.ratelimiters;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Small extension to google rate limiter using a fair lock It ensures that when
 * the rate is updated, at most one request observes the old rate. With google's
 * rate limiter, requests observe the rate from when they were enqueued, so
 * there is lag.
 * 
 * @author a-jomace
 *
 */
public class WrappedGoogleRateLimiter {

    // Fair lock - provides ordering
    private Lock lock = new ReentrantLock(true);
    private final com.google.common.util.concurrent.RateLimiter r;

    public WrappedGoogleRateLimiter() {
        this(Double.MAX_VALUE);
    }

    public WrappedGoogleRateLimiter(double permitsPerSecond) {
        this.r = com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond);
    }

    public void setRate(double permitsPerSecond) {
        this.r.setRate(permitsPerSecond);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int permits) {
        if (!lock.tryLock())
            return false;
        try {
            return this.r.tryAcquire(permits);
        } finally {
            lock.unlock();
        }
    }

    public double acquire() {
        return acquire(1);
    }

    public double acquire(int permits) {
        lock.lock();
        try {
            return this.r.acquire(permits);
        } finally {
            lock.unlock();
        }
    }

}
