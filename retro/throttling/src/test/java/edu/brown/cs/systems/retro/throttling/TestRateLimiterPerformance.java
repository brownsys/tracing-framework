package edu.brown.cs.systems.retro.throttling;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.ratelimiters.LockingRateLimiter;
import edu.brown.cs.systems.retro.throttling.ratelimiters.LongRateLimiter;
import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;
import edu.brown.cs.systems.retro.throttling.ratelimiters.WrappedGoogleRateLimiter;

public class TestRateLimiterPerformance extends TestCase {

    private interface Limiter {
        public void setRate(double rate);

        public void acquire() throws InterruptedException;
    }

    private static class GuavaRateLimiter implements Limiter {
        final com.google.common.util.concurrent.RateLimiter r = com.google.common.util.concurrent.RateLimiter.create(Double.MAX_VALUE);

        public void setRate(double rate) {
            r.setRate(rate);
        }

        public void acquire() {
            r.acquire();
        }
    }

    private static class WrappedGuavaRateLimiter implements Limiter {
        final WrappedGoogleRateLimiter r = new WrappedGoogleRateLimiter(Double.MAX_VALUE);

        public void setRate(double rate) {
            r.setRate(rate);
        }

        public void acquire() throws InterruptedException {
            r.acquire();
        }
    }

    private static class DoubleLimiterMax implements Limiter {
        final RateLimiter r = new RateLimiter(Double.MAX_VALUE);

        public void setRate(double rate) {
            r.setRate(rate);
        }

        public void acquire() throws InterruptedException {
            r.acquire();
        }
    }

    private static class DoubleLimiter implements Limiter {
        final RateLimiter r = new RateLimiter(Double.MAX_VALUE - 1);

        public void setRate(double rate) {
            if (rate == Double.MAX_VALUE)
                rate -= 1;
            r.setRate(rate);
        }

        public void acquire() throws InterruptedException {
            r.acquire();
        }
    }

    private static class LongLimiter implements Limiter {
        final LongRateLimiter r = new LongRateLimiter(Double.MAX_VALUE);

        public void setRate(double rate) {
            r.setRate(rate);
        }

        public void acquire() {
            r.acquire();
        }
    }

    private static class LockingLimiter implements Limiter {
        final LockingRateLimiter r = new LockingRateLimiter(Double.MAX_VALUE);

        public void setRate(double rate) {
            r.setRate(rate);
        }

        public void acquire() {
            r.acquire();
        }
    }

    private void throughputTest(final Limiter r, final int numthreads, final long durationNanos) {
        // Set to max allowed throughput
        r.setRate(Double.MAX_VALUE);

        final CountDownLatch ready = new CountDownLatch(numthreads);
        final CountDownLatch experimentStart = new CountDownLatch(1);
        final CountDownLatch experimentComplete = new CountDownLatch(numthreads);

        final AtomicLong count = new AtomicLong();

        // The threads all try to acquire a permit. As soon as the first permit
        // is acquired, the rate is set to max value. Ideally, the rate limiter
        // should immediately flush.
        for (int i = 0; i < numthreads; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    ready.countDown();
                    long myCount = 0;
                    try {
                        experimentStart.await();
                        long begin = System.nanoTime();
                        while ((System.nanoTime() - begin) < durationNanos) {
                            r.acquire();
                            myCount++;
                        }
                    } catch (InterruptedException e) {
                        experimentComplete.countDown();
                    } finally {
                        count.getAndAdd(myCount);
                        experimentComplete.countDown();
                    }
                }
            };
            t.start();
        }

        try {
            ready.await();
            experimentStart.countDown();
            experimentComplete.await();
            System.out.printf("%s duration=%.3f count=%.2f million\n", r.getClass().getSimpleName(), (durationNanos / 1000000000.0), count.get() / 1000000.0);
        } catch (InterruptedException e) {
            System.out.println("Experiment interrupted");
        } finally {
            experimentStart.countDown();
        }
    }

    @Test
    public void testRateLimiterThroughput() {

        int numthreads = 10;
        double durationSecs = 1;
        long durationNanos = (long) (durationSecs * 1000000000.0);

        throughputTest(new GuavaRateLimiter(), numthreads, durationNanos);
        throughputTest(new WrappedGuavaRateLimiter(), numthreads, durationNanos);
        throughputTest(new DoubleLimiterMax(), numthreads, durationNanos);
        throughputTest(new DoubleLimiter(), numthreads, durationNanos);
        throughputTest(new LongLimiter(), numthreads, durationNanos);
        throughputTest(new LockingLimiter(), numthreads, durationNanos);

    }

}
