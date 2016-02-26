package edu.brown.cs.systems.retro.throttling.mclock;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;

/** Implementation of MClock scheduler */
public class MClock {
    private static final boolean DEBUG = ConfigFactory.load().getBoolean("retro.throttling.debug.mclock");

    /** Reuse DiskRequest objects for the purposes of caching */
    private ThreadLocal<DiskRequest> current_request = new ThreadLocal<DiskRequest>() {
        @Override
        protected DiskRequest initialValue() {
            return new DiskRequest();
        }
    };

    private final TenantSet tenants = new TenantSet();

    /** By default, no reservation */
    public static final double DEFAULT_RESERVATION = 0;

    /** By default, a weight of 1 */
    public static final double DEFAULT_WEIGHT = 1;

    /** By default, no limit */
    public static final double DEFAULT_LIMIT = Double.MAX_VALUE;

    /** The number of concurrent requests allowed through this scheduler */
    private volatile int concurrency;

    /** Manages concurrent access */
    private final SlotManager slots;

    /**
     * When there is room for a request to be scheduled, this lock must be held
     * (for now)
     */
    private final Lock schedulingLock = new ReentrantLock();

    /**
     * @param concurrency
     *            specifies the number of concurrent requests that can execute
     */
    public MClock(int concurrency) {
        this.concurrency = concurrency;
        this.slots = new SlotManager(concurrency);
    }

    /**
     * Schedule a request for the specified tenant, that has the specified
     * estimated cost. The thread will wait until the scheduler deems it OK to
     * proceed. The thread can be interrupted while waiting.
     * 
     * @param tenantId
     *            the ID of the tenant to schedule the request for.
     * @param costEstimate
     *            an estimate of the cost of the request
     * @return the amount of time in nanoseconds spent queueing (as opposed to
     *         being rate limited)
     * @throws InterruptedException
     *             if the thread is interrupted while waiting.
     */
    public long schedule(int tenantId, double costEstimate) throws InterruptedException {
        // Get the tenant
        Tenant tenant = tenants.get(tenantId);
        DiskRequest request = current_request.get();
        if (request.isActive()) {
            // this request has been scheduled and is mid execution. continue
            request.reenter();
            if (DEBUG)
                print("reenter", "thread has reentered scheduler, reentrance_count=%d", request.entrance_count);
            return 0;
        } else {
            request.reset(tenant, costEstimate);
            tenant.addNewRequest(request);
        }

        // See how many requests are queued
        if (slots.addRequest()) {
            if (DEBUG)
                print("enqueue", "can schedule immediately");
            triggerNextRequest();
        } else {
            if (DEBUG)
                print("enqueue", "cannot schedule immediately");
        }

        // Wait for somebody to trigger us, if we haven't already been triggered
        long beginWait = System.nanoTime();
        boolean waited = request.await();

        if (DEBUG)
            print("execute", "executing request");

        if (waited) {
            return System.nanoTime() - beginWait;
        } else {
            return 0;
        }
    }

    public void complete() {
        // Check if this has multiple entrance
        DiskRequest request = current_request.get().exit();
        if (request.isActive()) {
            if (DEBUG)
                print("reexit", "thread has exited scheduler, reentrance_count=%d", request.entrance_count);
            return;
        }

        // Not multiple entrance so complete
        if (DEBUG)
            print("execute", "execution complete");
        if (slots.addSlot()) {
            if (DEBUG)
                print("complete", "scheduling a waiting request");
            triggerNextRequest();
        } else {
            if (DEBUG)
                print("complete", "nobody to schedule");
        }
    }

    public synchronized void setConcurrency(int newConcurrency) {
        if (newConcurrency < 1)
            throw new IllegalArgumentException("Cannot set MClock to concurrency of " + newConcurrency);
        if (DEBUG)
            print("UPDATE", "setting mclock concurrency to %d", newConcurrency);

        long toSchedule = slots.setMaxSlots(newConcurrency);
        for (int i = 0; i < toSchedule; i++) {
            if (DEBUG)
                print("UPDATE", "triggering additional request");
            triggerNextRequest();
        }

        this.concurrency = newConcurrency;

        if (DEBUG)
            print("UPDATE", "new concurrency set");
    }

    public int getConcurrency() {
        return concurrency;
    }

    /**
     * We have claimed a request slot using should_schedule, now we wait our
     * turn to grab a request and trigger it
     */
    private void triggerNextRequest() {
        schedulingLock.lock();
        try {
            double time = getTime();

            double lowestReservation = Double.MAX_VALUE;
            Tenant lowestReservationTenant = null;

            double lowestWeight = Double.MAX_VALUE;
            Tenant lowestWeightTenant = null;

            for (Tenant tenant : tenants) {
                /* get this tenant's next request if they have one */
                DiskRequest next = tenant.peek();
                if (next == null) {
                    continue;
                }

                /* does this request have the lowest valid reservation? */
                double reservation = next.reservation();
                if (reservation <= time && reservation < lowestReservation) {
                    lowestReservation = reservation;
                    lowestReservationTenant = tenant;
                }

                /*
                 * does this request have the lowest weight? only do this is we
                 * haven't found a lowest reservation yet
                 */
                if (lowestReservationTenant == null) {
                    double weight = next.weight();
                    if (weight < lowestWeight) {
                        lowestWeight = weight;
                        lowestWeightTenant = tenant;
                    }
                }
            }

            /* actually take the request from the tenant */
            DiskRequest request;
            if (lowestReservationTenant != null) {
                request = lowestReservationTenant.takeReserved(time);
            } else {
                request = lowestWeightTenant.takeWeight(time);
            }

            if (DEBUG)
                print("trigger", "%s", request);

            request.trigger();

        } finally {
            schedulingLock.unlock();
        }
    }

    private static double getTime() {
        return System.nanoTime() / 1000000000.0;
    }

    public void setReservation(int tenantId, double reservation) {
        tenants.get(tenantId).setReservation(reservation);
    }

    public void setWeight(int tenantId, double weight) {
        tenants.get(tenantId).setWeight(weight);
    }

    public void setLimit(int tenantId, double limit) {
        tenants.get(tenantId).setLimit(limit);
    }

    public void resetReservation(int tenantId) {
        tenants.get(tenantId).setReservation(DEFAULT_RESERVATION);
    }

    public void resetWeight(int tenantId) {
        tenants.get(tenantId).setWeight(DEFAULT_WEIGHT);
    }

    public void resetLimit(int tenantId) {
        tenants.get(tenantId).setLimit(DEFAULT_LIMIT);
    }

    public void configure(int tenantId, double reservation, double weight, double limit) {
        Tenant t = tenants.get(tenantId);
        t.setReservation(reservation);
        t.setWeight(weight);
        t.setLimit(limit);
    }

    public Collection<Integer> tenants() {
        return tenants.tenants();
    }

    private class TenantSet implements Iterable<Tenant> {

        private final ConcurrentHashMap<Integer, Tenant> tenants = new ConcurrentHashMap<Integer, Tenant>();

        public Tenant get(int tenantId) {
            if (!tenants.containsKey(tenantId)) {
                tenants.putIfAbsent(tenantId, new Tenant(tenantId));
            }
            return tenants.get(tenantId);
        }

        @Override
        public Iterator<Tenant> iterator() {
            return tenants.values().iterator();
        }

        public Collection<Integer> tenants() {
            return new HashSet<Integer>(tenants.keySet());
        }

    }

    private class Tenant {
        public final int id;

        // The configuration settings for this tenant
        private volatile double WEIGHT = DEFAULT_WEIGHT;
        private volatile double RESERVATION = DEFAULT_RESERVATION;
        private volatile double LIMIT = DEFAULT_LIMIT;

        // Used to rate limit the tenant if necessary
        private final RateLimiter limiter = new RateLimiter(LIMIT);

        // The queue of pending requests
        private ConcurrentLinkedQueue<DiskRequest> pending = new ConcurrentLinkedQueue<DiskRequest>();

        // The values of the most recent disk request
        private volatile double weight;
        private volatile double reservation;
        private volatile double time;

        public Tenant(int tenantId) {
            this.id = tenantId;
        }

        public void setReservation(double reservation) {
            this.RESERVATION = reservation;
        }

        public void setWeight(double weight) {
            this.WEIGHT = weight;
        }

        public void setLimit(double limit) {
            this.LIMIT = limit;
            limiter.setRate(this.LIMIT);
        }

        public DiskRequest peek() {
            return pending.peek();
        }

        public DiskRequest takeReserved(double time) {
            DiskRequest next = this.pending.poll();
            this.reservation = next.reservation();
            this.time = time;
            return next;
        }

        public DiskRequest takeWeight(double time) {
            DiskRequest next = this.pending.poll();
            this.weight = next.weight();
            this.time = time;
            return next;
        }

        private boolean isActive(double time) {
            return !pending.isEmpty() || time - this.time < 2;
        }

        private synchronized void setStartingWeight(double time) {
            if (isActive(time))
                // somebody beat us to it, so we're good
                return;

            boolean success = false;
            double minPendingWeight = Double.MAX_VALUE;
            for (Tenant tenant : tenants) {
                DiskRequest tnext = tenant.peek();
                if (tnext != null) {
                    double weight = tnext.weight();
                    if (weight < minPendingWeight) {
                        success = true;
                        minPendingWeight = weight;
                    }
                }
            }

            this.weight = success ? minPendingWeight : 0;
        }

        /**
         * throws InterruptedException if we are being rate limited and this
         * thread isinterrupted
         */
        public DiskRequest addNewRequest(DiskRequest request) throws InterruptedException {
            // Reset the starting weight if necessary.
            if (!isActive(request.time))
                setStartingWeight(request.time);

            // Wait for the limit if necessary
            limiter.acquire(request.estimatedCost);

            pending.add(request);
            return request;
        }

    }

    private class DiskRequest {

        private Tenant tenant;
        private double estimatedCost;
        private double time;
        private int entrance_count;

        private final Semaphore semaphore = new Semaphore(0);

        public DiskRequest reset(Tenant tenant, double estimatedCost) {
            this.tenant = tenant;
            this.estimatedCost = estimatedCost;
            this.time = getTime();
            this.entrance_count = 1;
            return this;
        }

        public void reenter() {
            entrance_count++;
        }

        public DiskRequest exit() {
            entrance_count--;
            return this;
        }

        public boolean isActive() {
            return entrance_count > 0;
        }

        public double reservation() {
            return Math.max(tenant.reservation + estimatedCost / tenant.RESERVATION, time - 1);
        }

        public double weight() {
            return Math.max(tenant.weight + estimatedCost / tenant.WEIGHT, time);
        }

        public void trigger() {
            semaphore.release();
        }

        /**
         * @returns true if a wait was required (eg, we aren't able to
         *          immediately proceed)
         * @throws InterruptedException
         *             if this thread is interrupted while waiting
         */
        public boolean await() throws InterruptedException {
            if (semaphore.tryAcquire()) {
                if (DEBUG)
                    print("enqueue", "no wait required");
                return false;
            }
            if (DEBUG)
                print("enqueue", "waiting");
            semaphore.acquire();
            return true;
        }

        public String requestID() {
            return Integer.toHexString(System.identityHashCode(this));
        }

        @Override
        public String toString() {
            return requestID() + " estimatedCost=" + estimatedCost + " res=" + tenant.reservation + "   wgt=" + tenant.weight;
        }
    }

    private String trimpad(Object o, int width) {
        String s = o.toString();
        if (s.length() > width)
            s = s.substring(0, width);
        while (s.length() < width)
            s = s + " ";
        return s;
    }

    private void print(String phase, String fmt, Object... fmtobjs) {
        try {
            String s1 = trimpad(getTime(), 10);
            String s2 = trimpad(Thread.currentThread().getId(), 4);
            String s3 = "";
            String s4 = "";
            if (current_request.get().tenant != null) {
                s3 = trimpad(current_request.get().tenant.id, 4);
                s4 = trimpad(current_request.get().requestID(), 10);
            }
            String s5 = trimpad(phase, 10);
            String msg = String.format(fmt, fmtobjs);

            System.out.printf("%s %s %s %s %s %s\n", s1, s2, s3, s4, s5, msg);
        } catch (Exception e) {
            // swallow
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("MClock-");
        b.append(concurrency);
        b.append(" (tenant)=[res, wgt, lim]");
        for (Tenant t : tenants) {
            b.append(" (");
            b.append(t.id);
            b.append(")=[");
            if (t.RESERVATION == 0)
                b.append("none");
            else
                b.append(String.format("%.4f", t.RESERVATION));
            b.append(", ");
            b.append(String.format("%.2f", t.WEIGHT));
            b.append(", ");
            if (t.LIMIT == Double.MAX_VALUE)
                b.append("MAX");
            else
                b.append(String.format("%.4f", t.LIMIT));
            b.append("]");
        }
        return b.toString();
    }

}
