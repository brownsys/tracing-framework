package edu.brown.cs.systems.retro.throttling.ratelimiters;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LockingRateLimiter {

    // Fair lock - provides ordering
    private volatile double rate;
    private volatile double backlog;
    private volatile double clock;

    private Lock rateLock = new ReentrantLock();
    private Condition rateChanged = rateLock.newCondition();

    /** Fair lock for acquiring permits */
    private Lock permitsLock = new ReentrantLock(true);

    public LockingRateLimiter() {
        this(Double.MAX_VALUE);
    }

    public LockingRateLimiter(double permitsPerSecond) {
        this.rate = permitsPerSecond;
    }

    public void setRate(double permitsPerSecond) {
        rateLock.lock();
        try {
            double now = System.nanoTime() / 1000000000.0;
            double pending = clock - now;
            double newPending = pending * rate / permitsPerSecond;
            clock = Math.max(now + newPending, now - backlog);
            rate = permitsPerSecond;
            rateChanged.signalAll();
        } finally {
            rateLock.unlock();
        }
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int permits) {
        if (permitsLock.tryLock()) {
            rateLock.lock();
            try {
                long nowNanos = System.nanoTime();
                double nowSecs = nowNanos / 1000000000.0;
                double nextClock = Math.max(clock + permits / rate, nowSecs - backlog);
                if (nextClock <= nowSecs) {
                    clock = nextClock;
                    return true;
                }
            } finally {
                rateLock.unlock();
            }
        }
        return false;
    }

    public double acquire() {
        return acquire(1);
    }

    public double acquire(int permits) {
        permitsLock.lock();
        try {
            rateLock.lock();
            try {
                long nowNanos = System.nanoTime();
                double nowSecs = nowNanos / 1000000000.0;
                clock = Math.max(clock + permits / rate, nowSecs - backlog);
                if (clock > nowSecs) {
                    double permitsToAwait = (clock - nowSecs) * rate;
                    long nanosWaited = waitFor(permitsToAwait, nowNanos);
                    return nanosWaited / 1000000000.0;
                }
                return 0;
            } finally {
                rateLock.unlock();
            }
        } finally {
            permitsLock.unlock();
        }
    }

    /** MUST HOLD RATE LOCK */
    private long waitFor(double permitsToAwait, long t) {
        long begin = t;

        /* Acquire the writeRate lock so that we can wait on the condition */
        try {
            while (permitsToAwait > 0) {
                double currentRate = this.rate;
                double nanosPerPermit = 1000000000.0 / currentRate;
                long done = t + ((long) (permitsToAwait * nanosPerPermit));
                long now = System.nanoTime();
                if (done > now) {
                    rateChanged.awaitNanos(done - now);
                    now = System.nanoTime();
                }
                double permitsElapsed = ((now - t) / 1000000000.0) * currentRate;

                // Update the permits to the number remaining, and t to the
                // current time
                permitsToAwait -= permitsElapsed;
                t = now;
            }
        } catch (InterruptedException e) {
            // do nothing, just return immediately
        }

        return t - begin;
    }

    public static void main(String[] args) throws InterruptedException {
        final long epoch = System.nanoTime();
        final LockingRateLimiter r = new LockingRateLimiter(0.001);

        for (int i = 0; i < 50; i++) {
            Thread acquirer = new Thread() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        r.acquire();
                        System.out.printf("%d: %.2f acquiring\n", Thread.currentThread().getId(), (System.nanoTime() - epoch) / 1000000000.0);
                    }
                }
            };
            acquirer.start();
        }

        Thread.sleep(1100);
        System.out.printf("%.2f: setting to 2\n", (System.nanoTime() - epoch) / 1000000000.0);
        r.setRate(2);
        Thread.sleep(5100);
        System.out.printf("%.2f: setting to 0.1\n", (System.nanoTime() - epoch) / 1000000000.0);
        r.setRate(0.1);
        Thread.sleep(25100);
        System.out.printf("%.2f: setting to 10\n", (System.nanoTime() - epoch) / 1000000000.0);
        r.setRate(10);

    }

}
