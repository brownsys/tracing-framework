package edu.brown.cs.systems.retro.throttling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;

public class TestRateLimiter extends TestCase {

    @Test
    public void testRateLimiterRateChange() {

        for (int numthreads = 1; numthreads < 100; numthreads *= 2) {
            double rate1 = 100;
            double rate2 = Double.MAX_VALUE;

            final RateLimiter r = new RateLimiter(rate1);

            final AtomicLong threadscomplete = new AtomicLong();

            final CountDownLatch ready = new CountDownLatch(numthreads);
            final CountDownLatch go = new CountDownLatch(1);

            Collection<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < numthreads; i++) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            ready.countDown();
                            go.await();
                            r.acquire(100000);
                            threadscomplete.getAndIncrement();
                        } catch (InterruptedException e) {
                            System.out.println("interrupted");
                        }
                    }
                };
                t.start();
                threads.add(t);
            }

            try {
                ready.await();
                r.acquire(1);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            go.countDown();

            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            long currentlyComplete = threadscomplete.get();
            assertEquals("expect 0 threads complete, actual=" + currentlyComplete, 0, currentlyComplete);

            // set to very fast rate
            r.setRate(rate2);

            // give a little bit of time to complete
            long end = System.nanoTime() + 100000000; // max 100 ms waiting
            for (Thread t : threads) {
                try {
                    long time = System.nanoTime();
                    if (time < end) {
                        t.join(((end - time) / 1000000), (int) ((end - time) % 1000000));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long finallyComplete = threadscomplete.get();
            assertEquals("expect 0 threads complete, actual=" + finallyComplete, numthreads, finallyComplete);
        }

    }

    @Test
    public void testRateLimiterInterrupt() {

        for (int numthreads = 1; numthreads < 100; numthreads *= 2) {
            double permitsPerSecond = 100;
            final RateLimiter r = new RateLimiter(permitsPerSecond);

            final AtomicLong threadsinterrupted = new AtomicLong();

            final CountDownLatch ready = new CountDownLatch(numthreads);
            final CountDownLatch go = new CountDownLatch(1);

            Collection<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < numthreads; i++) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            ready.countDown();
                            go.await();
                            while (!Thread.currentThread().isInterrupted()) {
                                r.acquire(10000);
                            }
                            if (Thread.currentThread().isInterrupted())
                                threadsinterrupted.getAndIncrement();
                        } catch (InterruptedException e) {
                            threadsinterrupted.getAndIncrement();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
                threads.add(t);
            }

            try {
                ready.await();
                r.acquire(1);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            go.countDown();

            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            for (Thread t : threads) {
                t.interrupt();
            }

            long end = System.nanoTime() + 1000000000; // max 1 second waiting
            for (Thread t : threads) {
                try {
                    long time = System.nanoTime();
                    if (time < end) {
                        t.join(((end - time) / 1000000), (int) ((end - time) % 1000000));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            assertEquals("not all threads interrupted successfully", numthreads, threadsinterrupted.get());
        }

    }

    @Test
    public void testRateLimiterConcurrency() throws InterruptedException {

        int[] totest = new int[] { 1, 4, 32, 128 };

        for (int numthreads : totest) {
            double permitsPerSecond = 21929;
            final AtomicLong permitsAcquired = new AtomicLong(0);
            final long start = System.nanoTime();
            final long duration = 500; // 500 milliseconds
            final RateLimiter r = new RateLimiter(permitsPerSecond);

            final CountDownLatch ready = new CountDownLatch(numthreads);
            final CountDownLatch go = new CountDownLatch(1);
            final AtomicBoolean done = new AtomicBoolean(false);

            Collection<Thread> threads = new ArrayList<Thread>();
            for (int j = 0; j < numthreads; j++) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        int count = 0;
                        ready.countDown();
                        try {
                            go.await();
                            while (!done.get()) {
                                r.acquire();
                                count++;
                            }
                            permitsAcquired.addAndGet(count);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
                threads.add(t);
            }

            ready.await();
            go.countDown();
            Thread.sleep(duration);
            done.set(true);

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    return;
                }
            }

            double desired = permitsPerSecond * duration / 1000.0;
            assertTrue(
                    String.format("numthreads is %d, rate is %.2f, duration is %d, actual was %d", numthreads, permitsPerSecond, duration,
                            permitsAcquired.get()), permitsAcquired.get() > desired * 0.9);
            assertTrue(
                    String.format("numthreads is %d, rate is %.2f, duration is %d, actual was %d", numthreads, permitsPerSecond, duration,
                            permitsAcquired.get()), permitsAcquired.get() < desired / 0.9);

        }
    }

    @Test
    public void testRateLimiter() throws InterruptedException {
        int[] totest = new int[] { 1, 2, 4, 8, 16, 64, 128, 512 };
        for (int rate : totest) {
            doRateLimiterTest(rate);
        }
    }

    private void doRateLimiterTest(double permitsPerSecond) throws InterruptedException {
        RateLimiter r = new RateLimiter(permitsPerSecond);

        // acquire some to start with
        long start = System.nanoTime();
        long duration = 1000 * 1000000L;
        while (System.nanoTime() < start + duration) {
            r.acquire();
        }

        // now acquire some and count
        start = System.nanoTime();
        long acquired = 0;
        while (System.nanoTime() < start + duration) {
            r.acquire();
            acquired++;
        }

        double acquisitionRate = acquired * 1000000000L / duration;

        String msg = String.format("permitsPerSecond=%.0f, actual=%.0f", permitsPerSecond, acquisitionRate);
        assertTrue(msg, acquisitionRate + 1 >= permitsPerSecond);
        assertTrue(msg, acquisitionRate - 1 <= permitsPerSecond);
    }

}
