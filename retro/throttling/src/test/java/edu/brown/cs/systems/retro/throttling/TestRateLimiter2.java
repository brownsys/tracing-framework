package edu.brown.cs.systems.retro.throttling;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;

/** More rate limiter tests */
public class TestRateLimiter2 {

    private static class RateLimitedThreadGroup implements Runnable {
        // rate limiter under test
        private final RateLimiter r;

        private final int numthreads;
        private final double permitsPerAcquisition;

        // controls
        private final CountDownLatch ready;
        private final CountDownLatch go;
        private final AtomicBoolean done;

        // counters
        public final AtomicLong interrupted = new AtomicLong(0);
        public final AtomicLong complete = new AtomicLong(0);
        public final AtomicLong started = new AtomicLong(0);

        // threads
        private Collection<Thread> threads = new ArrayList<Thread>();

        public RateLimitedThreadGroup(RateLimiter r, int numthreads, double permitsPerAcquisition) {
            this.r = r;
            this.numthreads = numthreads;
            this.permitsPerAcquisition = permitsPerAcquisition;
            this.ready = new CountDownLatch(numthreads);
            this.go = new CountDownLatch(1);
            this.done = new AtomicBoolean(false);

            for (int i = 0; i < numthreads; i++) {
                threads.add(new Thread(this));
            }
        }

        @Override
        public void run() {
            try {
                started.getAndIncrement();
                ready.countDown();
                go.await();
                while (!done.get()) {
                    r.acquire(permitsPerAcquisition);
                    Thread.sleep(10);
                }
                complete.getAndIncrement();
            } catch (InterruptedException e) {
                interrupted.getAndIncrement();
            }
        }

        /** Starts threads and gets them ready for the firing pistol */
        public void initialize() throws InterruptedException {
            check(0, 0, 0);

            for (Thread t : threads) {
                t.start();
            }
            ready.await();

            check(numthreads, 0, 0);
        }

        /** Fires the pistol! go threads go */
        public void start() {
            check(numthreads, 0, 0);

            // Trigger thread begin
            go.countDown();
        }

        /** Trigger the threads that we're done */
        public void end() {
            check(numthreads, 0, 0);

            // Trigger thread completion
            done.set(true);
        }

        /** Wait for threads to all finish */
        public void awaitCompletion(long timeoutSecs) throws InterruptedException {
            long end = System.nanoTime() + timeoutSecs * 1000 * 1000000L; // max
                                                                          // 10
                                                                          // s
                                                                          // waiting
            for (Thread t : threads) {
                long time = System.nanoTime();
                if (time < end) {
                    t.join(((end - time) / 1000000), (int) ((end - time) % 1000000));
                }
            }

            check(numthreads, numthreads, 0);
        }

        /** Wait for threads to finish, expecting instead to time out waiting */
        public void awaitTimeout(long awaitTimeout, long awaitJoin) throws InterruptedException {
            long end = System.nanoTime() + awaitTimeout * 1000 * 1000000L;
            for (Thread t : threads) {
                long time = System.nanoTime();
                if (time < end) {
                    t.join(((end - time) / 1000000), (int) ((end - time) % 1000000));
                }
            }

            check(numthreads, 0, 0);

            for (Thread t : threads) {
                t.interrupt();
            }

            end = System.nanoTime() + awaitJoin * 1000 * 1000000L;
            for (Thread t : threads) {
                long time = System.nanoTime();
                if (time < end) {
                    t.join(((end - time) / 1000000), (int) ((end - time) % 1000000));
                }
            }

            check(numthreads, 0, numthreads);
        }

        public void check(int expectStarted, int expectCompleted, int expectInterrupted) {
            // Check started
            long actualStarted = started.get();
            assertEquals("Unexpected number of threads started", expectStarted, actualStarted);

            // Check completed
            long actualComplete = complete.get();
            assertEquals("Unexpected number of threads completed", expectCompleted, actualComplete);

            // Check no interrupts
            long actualInterrupts = interrupted.get();
            assertEquals("Unexpected number of threads interrupted", expectInterrupted, actualInterrupts);
        }
    }

    @Test
    public void testInfiniteRate() throws InterruptedException {
        int numthreads = 1000;
        double initialRate = 1;

        final RateLimiter r = new RateLimiter(initialRate);

        RateLimitedThreadGroup g = new RateLimitedThreadGroup(r, numthreads, initialRate);

        g.initialize();
        g.start();

        Thread.sleep(100);

        r.setRate(Double.MAX_VALUE);

        Thread.sleep(100);

        g.end();

        g.awaitCompletion(10);
    }

    @Test
    public void testNotInfiniteRate() throws InterruptedException {
        int numthreads = 1000;

        double initialRate = 1;
        double permitsPerAcquisition = 1000;

        final RateLimiter r = new RateLimiter(initialRate);

        RateLimitedThreadGroup g = new RateLimitedThreadGroup(r, numthreads, permitsPerAcquisition);

        g.initialize();
        g.start();

        Thread.sleep(100);

        g.end();

        g.awaitTimeout(1, 10);
    }

    @Test
    public void testRandomRateChanges() throws InterruptedException {

        int numthreads = 1000;

        int numperiods = 100;
        int periodduration = 10;

        double initialRate = 1;
        double permitsPerAcquisition = 1;

        final RateLimiter r = new RateLimiter(initialRate);

        RateLimitedThreadGroup g = new RateLimitedThreadGroup(r, numthreads, permitsPerAcquisition);

        g.initialize();
        g.start();

        Random rnd = new Random();
        for (int i = 0; i < numperiods; i++) {
            r.setRate(rnd.nextDouble());
            int tosleep = rnd.nextInt(periodduration);
            if (tosleep > 0)
                Thread.sleep(tosleep);
        }

        r.setRate(Double.MAX_VALUE);

        g.end();

        g.awaitCompletion(10);

    }

    @Test
    public void testBigRateChanges() throws InterruptedException {

        int numthreads = 1000;

        int numperiods = 100;
        int periodduration = 10;

        double initialRate = 1;
        double permitsPerAcquisition = 1;

        final RateLimiter r = new RateLimiter(initialRate);

        RateLimitedThreadGroup g = new RateLimitedThreadGroup(r, numthreads, permitsPerAcquisition);

        g.initialize();
        g.start();

        Random rnd = new Random();
        for (int i = 0; i < numperiods; i++) {
            r.setRate(Double.MAX_VALUE);
            int tosleep = rnd.nextInt(periodduration);
            if (tosleep > 0)
                Thread.sleep(tosleep);
            r.setRate(Double.MIN_VALUE);
            tosleep = rnd.nextInt(periodduration);
            if (tosleep > 0)
                Thread.sleep(tosleep);
            r.setRate(0);
            tosleep = rnd.nextInt(periodduration);
            if (tosleep > 0)
                Thread.sleep(tosleep);
        }

        r.setRate(Double.MAX_VALUE);

        g.end();

        g.awaitCompletion(10);

    }

}
