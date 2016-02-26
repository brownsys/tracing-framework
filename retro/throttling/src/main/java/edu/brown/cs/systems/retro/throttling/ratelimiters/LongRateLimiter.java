package edu.brown.cs.systems.retro.throttling.ratelimiters;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * RateLimiter implementation that reacts immediately to updates in rate
 * 
 * The RateLimiter is designed so that if it's under-utilized and no
 * rate-limiting occurs, then it is highly concurrent (using read-locks and
 * atomic values).
 * 
 * Only if it's over-utilized and rate-limiting must occur will write-locks be
 * utilized, which is OK since rate limiting is occurring...
 */
public class LongRateLimiter {

    private final ReentrantReadWriteLock rateLock = new ReentrantReadWriteLock();
    private final Lock readRate = rateLock.readLock();
    private final Lock writeRate = rateLock.writeLock();
    private final Condition rateChanged = writeRate.newCondition();

    /** The allowed backlog size in nanoseconds */
    private volatile long backlogSize;

    /** The permit generation rate in nanos per permit */
    private volatile long nanosPerPermit;

    /** Rate limiting clock */
    private final AtomicLong clock = new AtomicLong();

    public LongRateLimiter() {
        this(Double.MAX_VALUE);
    }

    public LongRateLimiter(double permitsPerSecond) {
        this(permitsPerSecond, 1);
    }

    public LongRateLimiter(double permitsPerSecond, double backlogSizeSeconds) {
        this.nanosPerPermit = (long) (1000000000.0 / permitsPerSecond);
        this.backlogSize = (long) (backlogSizeSeconds * 1000000000.0);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(long numPermits) {
        return doTryAcquire(sanitize(numPermits));
    }

    public double acquire() {
        return acquire(1);
    }

    public double acquire(long numPermits) {
        numPermits = sanitize(numPermits);

        // take a timestamp
        long t = System.nanoTime();

        // update the clock and find out how many permits we must wait for
        double permitsToAwait = doAcquire(numPermits, t);

        return waitFor(permitsToAwait, t);
    }

    private long sanitize(long permits) {
        if (permits <= 0)
            throw new IllegalArgumentException("Attempted to acquired " + permits + " permits");
        if (Double.isNaN(permits))
            throw new IllegalArgumentException("Attempted to acquired NaN permits");
        else if (Double.isInfinite(permits))
            throw new IllegalArgumentException("Attempted to acquired infinite permits");
        return permits;
    }

    /**
     * Updates the clock based on the number of permits requested
     * 
     * @param permitsToAcquire
     *            the number of permits being requested
     * @param currentTime
     *            the current time in nanoseconds
     * @return the number of permits to await before the acquisition is complete
     */
    private double doAcquire(long permitsToAcquire, long currentTime) {
        readRate.lock(); // not sure if should release and reacquire lock each
                         // time round the loop
        try {
            while (true) {
                long nanosPerPermit = this.nanosPerPermit;
                long clockDelta = permitsToAcquire * nanosPerPermit;
                long lowestAllowedClock = currentTime - backlogSize;

                // look at the current clock and determine the next clock
                long currentClock = clock.get();
                long nextClock = currentClock + clockDelta;
                if (nextClock < lowestAllowedClock)
                    nextClock = lowestAllowedClock;

                // try to update the clock. if successful, return permits to
                // await
                if (clock.compareAndSet(currentClock, nextClock)) {
                    if (nextClock > currentTime)
                        return (nextClock - currentTime) / (double) nanosPerPermit;
                    else
                        return 0.0;
                }
            }
        } finally {
            readRate.unlock();
        }
    }

    /**
     * Tries to acquire the specified number of permits, but only if they are
     * immediately available
     * 
     * @param permitsToAcquire
     *            the number of permits to acquire
     * @return true if the permits were successfully acquired
     */
    private boolean doTryAcquire(long permitsToAcquire) {
        long currentTime = System.nanoTime();
        readRate.lock(); // not sure if should release and reacquire lock each
                         // time round the loop
        try {
            while (true) {
                // pull out the current rate values
                long nanosPerPermit = this.nanosPerPermit;
                long clockDelta = permitsToAcquire * nanosPerPermit;
                long lowestAllowedClock = currentTime - backlogSize;

                // look at the current clock and determine the next clock
                long currentClock = clock.get();
                long nextClock = currentClock + clockDelta;
                if (nextClock < lowestAllowedClock)
                    nextClock = lowestAllowedClock;

                // immediately return false if the permits cannot be acquired
                if (nextClock > currentTime) {
                    return false;
                }

                // permits can be immediately acquired: try to update the clock
                if (clock.compareAndSet(currentClock, nextClock)) {
                    return true;
                }
            }
        } finally {
            readRate.unlock();
        }
    }

    /**
     * Update to the specified permits per second. Immediately affects any
     * pending acquires.
     * 
     * @param newPermitsPerSecond
     *            the new rate to release permits
     */
    public void setRate(double newPermitsPerSecond) {
        long nanosPerPermit = (long) (1000000000.0 / newPermitsPerSecond);

        writeRate.lock();
        try {
            // Adjust the clock based on its current backlog
            long current = clock.get();
            long now = System.nanoTime();
            long backlog = current - now;
            double scalefactor = nanosPerPermit / (double) this.nanosPerPermit;
            current = (long) (now + backlog * scalefactor);
            current = Math.max(current, now - backlogSize);
            clock.set(current);

            // Save the new rate
            this.nanosPerPermit = nanosPerPermit;

            // Signal to all pending requests that the rate has changed
            rateChanged.signalAll();
        } finally {
            writeRate.unlock();
        }
    }

    /**
     * Waits until the specified number of permits have PASSED
     * 
     * @param t
     *            the time that we started waiting
     * @param permits
     *            the number of permits to wait for
     * @return the total amount of time spend waiting
     * @throws InterruptedException
     *             if we are interrupted while waiting
     */
    private double waitFor(double permits, long t) {
        long begin = t;

        /* Acquire the writeRate lock so that we can wait on the condition */
        writeRate.lock();
        try {
            while (permits > 0) {
                long nanosPerPermit = this.nanosPerPermit;
                long done = t + (long) (permits * nanosPerPermit);
                long now = System.nanoTime();
                if (done > now) {
                    rateChanged.awaitNanos(done - now);
                    now = System.nanoTime();
                }
                double permitsElapsed = (now - t) / (double) nanosPerPermit;

                // Update the permits to the number remaining, and t to the
                // current time
                permits -= permitsElapsed;
                t = now;
            }
        } catch (InterruptedException e) {
            // ignore and return immediately
        } finally {
            writeRate.unlock();
        }

        return t - begin;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println((long) (1000000000.0 / 1));

        final long epoch = System.nanoTime();
        final LongRateLimiter r = new LongRateLimiter(0.001);

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
