package edu.brown.cs.systems.retro.throttling.ratelimiters;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * RateLimiter implementation that reacts immediately to updates in rate
 * 
 * The RateLimiter is designed so that if it's under-utilized and no
 * rate-limiting occurs, then it is highly concurrent (using read-locks and
 * atomic values).
 * 
 * Only if it's over-utilized and rate-limiting must occur will write-locks be
 * utilized, which is OK since rate limiting is occurring...
 * 
 * @author a-jomace
 */
public class RateLimiter {

    public static final double MAX_RATE = Double.MAX_VALUE;
    public static final double MIN_RATE = 1 / 1000000000.0;
    public static final double DEFAULT_RATE = MAX_RATE;

    private final ReentrantReadWriteLock rateLock = new ReentrantReadWriteLock();
    private final Lock readRate = rateLock.readLock();
    private final Lock writeRate = rateLock.writeLock();
    private final Condition rateChanged = writeRate.newCondition();

    /**
     * The allowed backlog size in seconds, eg. a 2 second backlog allows 2
     * seconds worth of permits to accrue
     */
    private volatile double backlogSize;

    /** The permit generation rate, in permits per second */
    private volatile double permitsPerSecond;

    /** Rate limiting clock */
    private final AtomicDouble clock;

    public RateLimiter() {
        this(DEFAULT_RATE);
    }

    public RateLimiter(double permitsPerSecond) {
        this(permitsPerSecond, 1);
    }

    public RateLimiter(double permitsPerSecond, double backlogSize) {
        this.permitsPerSecond = permitsPerSecond;
        this.backlogSize = backlogSize;
        this.clock = new AtomicDouble(System.nanoTime() / 1000000000.0);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(double numPermits) {
        // short circuit if max rate set
        if (permitsPerSecond == Double.MAX_VALUE)
            return true;

        return doTryAcquire(sanitize(numPermits));
    }

    public double acquire() throws InterruptedException {
        return acquire(1);
    }

    public double acquire(double numPermits) throws InterruptedException {
        // short circuit if max rate set
        if (permitsPerSecond == Double.MAX_VALUE)
            return 0;

        numPermits = sanitize(numPermits);

        // take a timestamp
        long tNanos = System.nanoTime();
        double tSecs = tNanos / 1000000000.0;

        // update the clock and find out how many permits we must wait for
        double permitsToAwait = doAcquire(numPermits, tSecs);

        return waitFor(permitsToAwait, tNanos) / 1000000000.0;
    }

    public double acquireUninterruptibly() {
        return acquireUninterruptibly(1);
    }

    public double acquireUninterruptibly(double numPermits) {
        long tNanos = System.nanoTime();
        try {
            return acquire(numPermits);
        } catch (InterruptedException e) {
            return System.nanoTime() - tNanos;
        }
    }

    private double sanitize(double permits) {
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
     *            the current time in seconds
     * @return the number of permits to await before the acquisition is complete
     */
    private double doAcquire(double permitsToAcquire, double currentTime) {
        while (true) {
            readRate.lock(); // not sure if should release and reacquire lock
                             // each time round the loop
            try {
                // pull out the current rate values
                double permitReleaseRate = this.permitsPerSecond;
                double clockDelta = permitsToAcquire / permitReleaseRate;
                double lowestAllowedClock = currentTime - backlogSize;

                // look at the current clock and determine the next clock
                double currentClock = clock.get();
                double nextClock = currentClock + clockDelta;
                if (nextClock < lowestAllowedClock)
                    nextClock = lowestAllowedClock;

                // try to update the clock. if successful, return permits to
                // await
                if (clock.compareAndSet(currentClock, nextClock)) {
                    if (nextClock > currentTime)
                        return permitReleaseRate * (nextClock - currentTime);
                    else
                        return 0;
                }
            } finally {
                readRate.unlock();
            }
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
    private boolean doTryAcquire(double permitsToAcquire) {
        double currentTime = System.nanoTime() / 1000000000.0;
        while (true) {
            readRate.lock();
            try {
                // pull out the current rate values
                double permitReleaseRate = this.permitsPerSecond;
                double clockDelta = permitsToAcquire / permitReleaseRate;
                double lowestAllowedClock = (currentTime - backlogSize) - clockDelta;

                // look at the current clock and determine the next clock
                double currentClock = clock.get();
                double nextClock = currentClock + clockDelta;
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
            } finally {
                readRate.unlock();
            }
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
        if (Double.isNaN(newPermitsPerSecond) || Double.isInfinite(newPermitsPerSecond) || newPermitsPerSecond < 0)
            newPermitsPerSecond = DEFAULT_RATE;
        if (newPermitsPerSecond < MIN_RATE)
            newPermitsPerSecond = MIN_RATE;
        if (newPermitsPerSecond > MAX_RATE)
            newPermitsPerSecond = MAX_RATE;
        writeRate.lock();
        try {
            // Adjust the clock based on its current backlog
            double current = clock.get();
            double now = System.nanoTime() / 1000000000.0;
            double backlog = current - now;
            double scalefactor = permitsPerSecond / newPermitsPerSecond;
            current = now + backlog * scalefactor;
            current = Math.max(current, now - backlogSize);
            clock.set(current);

            // Save the new rate
            permitsPerSecond = newPermitsPerSecond;

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
    private long waitFor(double permits, long t) throws InterruptedException {
        long begin = t;

        /* Acquire the writeRate lock so that we can wait on the condition */
        while (permits > 0 && !Thread.currentThread().isInterrupted()) {
            writeRate.lock();
            try {
                double currentRate = this.permitsPerSecond;

                // short-circuit if big rate
                if (currentRate == Double.MAX_VALUE)
                    return System.nanoTime() - begin;

                double nanosPerPermit = 1000000000.0 / currentRate;
                long done = t + ((long) (permits * nanosPerPermit));
                long now = System.nanoTime();

                if (done <= now) {
                    return now - begin;
                }

                rateChanged.awaitNanos(done - now);
                now = System.nanoTime();
                double permitsElapsed = ((now - t) / 1000000000.0) * currentRate;

                // Update the permits to the number remaining, and t to the
                // current time
                permits -= permitsElapsed;
                t = now;
            } finally {
                writeRate.unlock();
            }
        }

        return t - begin;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Double.MAX_VALUE);
        System.out.println(Double.MAX_VALUE * 2);
        System.out.println(1000000000 / Double.MAX_VALUE);
        System.out.println(1000000000 / (2 * Double.MAX_VALUE));

        final long epoch = System.nanoTime();
        final RateLimiter r = new RateLimiter(0.001);

        for (int i = 0; i < 50; i++) {
            Thread acquirer = new Thread() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            r.acquire();
                            System.out.printf("%d: %.2f acquiring\n", Thread.currentThread().getId(), (System.nanoTime() - epoch) / 1000000000.0);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
