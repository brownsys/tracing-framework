package edu.brown.cs.systems.retro.throttling.schedulers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import edu.brown.cs.systems.retro.throttling.mclock.MClock;
import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;

public class MClockTester {

    public static void main(String[] args) throws InterruptedException {
        MClockTester t = new MClockTester(1, 1);

        t.startThreads(1, 100, 1, 500000000);
        t.startThreads(2, 100, 1, 500000000);
        // t.startThreads(3, 20, 1, 500000000);
        // t.startThreads(4, 40, 1, 500000000);

        // t.setReservation(1, 1);
        // t.setReservation(2, 5);
        t.setLimit(1, 10);
        // t.setWeight(1, 9);
        // t.setWeight(2, 1);

        for (int i = 0; i < 10; i++) {
            Thread.currentThread().sleep(1000);
            System.out.println(t);
        }

        t.setResourceCapacity(100);

        while (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().sleep(1000);
            System.out.println(t);
        }

        t.shutdown();
    }

    private void shutdown() {
        for (TenantTester t : tenants.values())
            t.shutdown();
    }

    private static final Random rnd = new Random();

    private static long sampleExponential(long mean) {
        double x = rnd.nextDouble();
        long e = Math.round(-Math.log(1 - x) * mean); // (1-x) because we want
                                                      // (0,1] not [0,1)
        e = Math.min(mean * 20, e);
        return e;
    }

    private final MClock mclock;
    private final RateLimiter resource;

    private Map<Integer, TenantTester> tenants = new HashMap<Integer, TenantTester>();

    public MClockTester(int resourceCapacity, int mclockConcurrency) {
        this.mclock = new MClock(mclockConcurrency);
        this.resource = new RateLimiter(resourceCapacity);
    }

    public void startThreads(int tenantId, int numthreads, int opsize, long nanosBetweenRequests) {
        TenantTester tester = tenants.get(tenantId);
        if (tester == null) {
            tester = new TenantTester(tenantId);
            tenants.put(tenantId, tester);
        }

        tester.startThreads(numthreads, opsize, nanosBetweenRequests);
    }

    public void setResourceCapacity(double capacity) {
        resource.setRate(capacity);
    }

    public void setReservation(int tenantId, double reservation) {
        mclock.setReservation(tenantId, reservation);
    }

    public void setWeight(int tenantId, double weight) {
        mclock.setWeight(tenantId, weight);
    }

    public void setLimit(int tenantId, double limit) {
        mclock.setLimit(tenantId, limit);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        Integer[] tenantIds = tenants.keySet().toArray(new Integer[0]);
        Arrays.sort(tenantIds);
        for (Integer tenantId : tenantIds) {
            b.append(tenants.get(tenantId));
            b.append("\n");
        }
        return b.toString();
    }

    private class TenantTester {

        private Collection<TenantThread> threads = new HashSet<TenantThread>();
        private AtomicInteger consumed = new AtomicInteger();
        private final int tenantid;

        public TenantTester(int tenantid) {
            this.tenantid = tenantid;
        }

        public void shutdown() {
            for (TenantThread t : threads)
                t.interrupt();
        }

        public void startThreads(int numToStart, int opsize, long nanosBetweenRequests) {
            for (int i = 0; i < numToStart; i++) {
                TenantThread thread = new TenantThread(opsize, nanosBetweenRequests);
                thread.start();
                threads.add(thread);
            }
        }

        @Override
        public String toString() {
            return String.format("t-%d numthreads=%d consumed=%d", tenantid, threads.size(), consumed.getAndSet(0));
        }

        private class TenantThread extends Thread {

            private final int opsize;
            private final long nanos;

            public TenantThread(int opsize, long nanos) {
                this.opsize = opsize;
                this.nanos = nanos;
            }

            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        long tosleep = sampleExponential(nanos);
                        if (tosleep > 0)
                            Thread.sleep(nanos / 1000000, (int) (nanos % 1000000));
                        mclock.schedule(tenantid, opsize);
                        resource.acquire(opsize);
                        mclock.complete();
                        consumed.addAndGet(opsize);
                    }
                } catch (InterruptedException e) {
                    // End thread
                }
            }
        }

    }

}