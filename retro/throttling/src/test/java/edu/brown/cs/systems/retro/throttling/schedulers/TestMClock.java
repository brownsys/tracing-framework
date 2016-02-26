package edu.brown.cs.systems.retro.throttling.schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.mclock.MClock;

public class TestMClock extends TestCase {

    @Test
    public void testInterrupt() throws InterruptedException {
        final MClock clock = new MClock(1);

        final AtomicBoolean interrupted = new AtomicBoolean(false);

        // Add a rate limited tenant who will block in the rate limiter
        clock.setLimit(1, 1);
        final AtomicBoolean rateLimitedTenantInterrupted = new AtomicBoolean(false);
        Thread rateLimited = new Thread() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        clock.schedule(1, 10);
                        clock.complete();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    rateLimitedTenantInterrupted.getAndSet(true);
                }
            }
        };

        // A non-rate limited tenant who will block trying to acquire
        final AtomicBoolean blockedTenantInterrupted = new AtomicBoolean(false);
        Thread blockedThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        clock.schedule(2, 1);
                        clock.complete();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    blockedTenantInterrupted.getAndSet(true);
                }
            }
        };

        // Start the threads
        rateLimited.start();
        blockedThread.start();

        // Wait a bit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            assertTrue("testReentrance interrupted", false);
            return;
        }

        // Acquire the clock in this thread
        clock.schedule(3, 1);

        // Interrupt the two tenants
        rateLimited.interrupt();
        blockedThread.interrupt();

        // Try to join the two tenants
        try {
            rateLimited.join(100);
            blockedThread.join(100);
        } catch (InterruptedException e1) {
            assertTrue("testReentrance interrupted", false);
            return;
        }

        // assert that they successfully interrupted
        assertTrue("rate limited thread did not abort mclock upon interruption", rateLimitedTenantInterrupted.get());
        assertTrue("non-ratelimited thread did not abort mclock upon interruption", blockedTenantInterrupted.get());
    }

    @Test
    public void testReentrance() {
        final MClock clock = new MClock(1);

        final AtomicBoolean complete = new AtomicBoolean(false);

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    clock.schedule(1, 5);
                    clock.schedule(1, 5);
                    complete.getAndSet(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        t.start();
        try {
            t.join(1000);
            t.interrupt();
        } catch (InterruptedException e) {
            return;
        }

        assertTrue("Thread was not able to reenter mclock", complete.get());
    }

}
