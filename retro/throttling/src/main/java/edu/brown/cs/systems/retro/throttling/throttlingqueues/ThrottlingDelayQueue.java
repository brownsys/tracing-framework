package edu.brown.cs.systems.retro.throttling.throttlingqueues;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.NotImplementedException;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.ThrottlingPointAggregator;
import edu.brown.cs.systems.retro.throttling.ClassChecker;
import edu.brown.cs.systems.retro.throttling.ThrottlingQueue;

/**
 * This queue modifies FIFO queue semantics and I don't like it.
 * 
 * @author a-jomace
 *
 * @param <T>
 */
public class ThrottlingDelayQueue<T> implements ThrottlingQueue<T> {

    private final ClassChecker hadoop_ipc_class_checker_hack = new ClassChecker();

    private final BlockingQueue<TenantThrottler> throttlers = new DelayQueue<TenantThrottler>();
    private final ConcurrentHashMap<Integer, TenantThrottler> tenantThrottlers = new ConcurrentHashMap<Integer, TenantThrottler>();
    private final ThrottlingPointAggregator taggregator;

    private final long historySize;

    public ThrottlingDelayQueue(String queueid) {
        this(queueid, 1000000000); // default 1 second history (1 billion
                                   // nanoseconds)
    }

    /**
     * @param historySize
     *            up to historySizeNanos nanoseconds grace period
     */
    public ThrottlingDelayQueue(String queueid, long historySizeNanos) {
        this.historySize = historySizeNanos;
        this.taggregator = LocalResources.getThrottlingPointAggregator(queueid);
    }

    private TenantThrottler getThrottler(int tenantId) {
        TenantThrottler throttler = tenantThrottlers.get(tenantId);
        if (throttler == null) {
            synchronized (tenantThrottlers) {
                throttler = tenantThrottlers.get(tenantId);
                if (throttler == null) {
                    throttler = new TenantThrottler(tenantId);
                    tenantThrottlers.put(tenantId, throttler);
                }
            }
        }
        return throttler;
    }

    public void setRate(int tenantId, double ratePerSecond) {
        getThrottler(tenantId).setRate(ratePerSecond);
    }

    @Override
    public boolean add(T e) {
        return add(Retro.getTenant(), e);
    }

    public boolean add(int tenantId, T e) {
        return getThrottler(tenantId).add(e);
    }

    @Override
    public boolean contains(Object o) {
        return contains(Retro.getTenant(), o);
    }

    public boolean contains(int tenantId, Object o) {
        return getThrottler(tenantId).pending.contains(o);
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        return drainTo(Retro.getTenant(), c);
    }

    public int drainTo(int tenantId, Collection<? super T> c) {
        return getThrottler(tenantId).drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        return drainTo(Retro.getTenant(), c, maxElements);
    }

    public int drainTo(int tenantId, Collection<? super T> c, int maxElements) {
        return getThrottler(tenantId).drainTo(c, maxElements);
    }

    @Override
    public boolean offer(T e) {
        return add(e);
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
        return add(e);
    }

    @Override
    public void put(T e) throws InterruptedException {
        add(e);
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /** Takes the next element from the throttler */
    private T takeFrom(TenantThrottler throttler) {
        if (throttler != null) {
            Retro.setTenant(throttler.tenantId);
            return throttler.take();
        } else {
            return null;
        }
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return takeFrom(throttlers.poll(timeout, unit));
    }

    @Override
    public boolean remove(Object o) {
        throw new NotImplementedException("");
    }

    @Override
    public T take() throws InterruptedException {
        return takeFrom(throttlers.take());
    }

    @Override
    public T element() {
        T peeked = peek();
        if (peeked == null) {
            throw new NoSuchElementException();
        }
        return peeked;
    }

    @Override
    public T peek() {
        T peeked = null;
        TenantThrottler peekfrom = null;
        while ((peekfrom = throttlers.peek()) != null && (peeked = peekfrom.pending.peek()) == null) {
            // we get here if somebody removed from the throttler while we were
            // peeking
            // its impossible to have a throttler in the throttlers queue that
            // also has an empty pending queue
        }
        return peeked;
    }

    @Override
    public T poll() {
        return takeFrom(throttlers.poll());
    }

    @Override
    public T remove() {
        T removed = poll();
        if (removed == null) {
            throw new NoSuchElementException();
        }
        return removed;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return addAll(Retro.getTenant(), c);
    }

    public boolean addAll(int tenantId, Collection<? extends T> c) {
        return getThrottler(tenantId).addAll(c);
    }

    @Override
    public void clear() {
        TenantThrottler toclear = null;
        while ((toclear = throttlers.poll()) != null) {
            toclear.clear();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new NotImplementedException("");
    }

    @Override
    public boolean isEmpty() {
        for (TenantThrottler throttler : tenantThrottlers.values())
            if (throttler.count.get() > 0)
                return false;
        return true;
    }

    @Override
    public Iterator<T> iterator() {
        throw new NotImplementedException("");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NotImplementedException("");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException("");
    }

    @Override
    public int size() {
        int size = 0;
        for (TenantThrottler throttler : tenantThrottlers.values()) {
            size += throttler.count.get();
        }
        return size;
    }

    @Override
    public Object[] toArray() {
        throw new NotImplementedException("");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new NotImplementedException("");
    }

    /**
     * A TenantThrottler is for a single tenant All requests enqueued by that
     * tenant are handled by the tenant's TenantThrottler Internally, it
     * maintains a FIFO queue
     */
    private class TenantThrottler implements Delayed {

        private final int tenantId;

        private volatile long delay = 0;
        private volatile long next = 0;
        private volatile long avail = 0;
        private final AtomicInteger count = new AtomicInteger();
        private final BlockingQueue<T> pending = new LinkedBlockingQueue<T>();

        public TenantThrottler(int tenantId) {
            this.tenantId = tenantId;
        }

        public boolean add(T element) {
            // First, put the element in the queue
            boolean result = pending.add(element);

            // Now, increment the count, enqueueing this throttler if we are the
            // first element
            int previous_count = count.getAndIncrement();
            if (previous_count == 0) {
                enqueue();
            }

            // Update the aggregator
            taggregator.throttling(tenantId);

            return result;
        }

        public boolean addAll(Collection<? extends T> c) {
            // First, put the elements in the queue
            boolean result = pending.addAll(c);

            // Now, increment the count, enqueueing this throttler if we are the
            // first element
            int previous_count = count.getAndAdd(c.size());
            if (previous_count == 0) {
                enqueue();
            }

            return result;

        }

        /**
         * This method can only be called when we know that there are pending
         * elements. It is an error if this method is called but there are no
         * pending elements
         */
        public T take() {
            // Retrieve the next element
            T element = pending.poll();

            // TODO: this is a huuuuuuuuuge hack but no time to fix up better.
            // will fix later.
            // Use 'avail' as the actual enqueue time
            if (hadoop_ipc_class_checker_hack.isHadoopIPCCall(element))
                hadoop_ipc_class_checker_hack.setCallEnqueue(element, avail);
            if (hadoop_ipc_class_checker_hack.isHBaseCallRunner(element))
                hadoop_ipc_class_checker_hack.setCallRunnerEnqueue(element, avail);

            // Decrement the count
            // If the new count is greater than zero, then we are responsible
            // for enqueueing the throttler
            int new_count = count.decrementAndGet();
            if (new_count != 0) {
                enqueue();
            }

            // Update the aggregator
            taggregator.throttled(tenantId, 0L); // TODO track latency

            return element;
        }

        public int drainTo(Collection<? super T> c) {
            return pending.drainTo(c, count.getAndSet(0));
        }

        public int drainTo(Collection<? super T> c, int maxElements) {
            int current_count = count.getAndSet(0);
            int toDrain = Math.min(current_count, maxElements);
            int toReturn = current_count - toDrain;
            if (toReturn > 0 && count.getAndAdd(toReturn) == 0) {
                enqueue();
            }
            return pending.drainTo(c, toDrain);
        }

        public void clear() {
            throttlers.remove(this);
            int toRemove = count.getAndSet(0);
            for (int i = 0; i < toRemove; i++) {
                pending.poll();
            }
        }

        public void setRate(double ratePerSecond) {
            this.delay = (long) Math.floor(1000000000.0 / ratePerSecond);
        }

        /**
         * This throttler has elements pending but is not enqueued, so enqueue
         * it
         */
        public void enqueue() {
            // Clamp the previous time to either the value of next, or the
            // current time minus the history size
            long current_time = System.nanoTime();
            long previous = Math.max(next, current_time - historySize);

            // Set the next time
            next = previous + delay;
            avail = Math.max(next, current_time); // record when the element was
                                                  // actually available for deq

            // Enqueue this throttler
            throttlers.add(this);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof ThrottlingDelayQueue.TenantThrottler) {
                long onext = ((ThrottlingDelayQueue.TenantThrottler) other).next;
                if (next < onext)
                    return -1;
                else if (next == onext)
                    return 0;
                else
                    return 1;
            } else {
                long odelay = other.getDelay(TimeUnit.NANOSECONDS);
                long tdelay = this.getDelay(TimeUnit.NANOSECONDS);
                if (tdelay < odelay)
                    return -1;
                else if (tdelay == odelay)
                    return 0;
                else
                    return 1;
            }
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(next - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

    }

    @Override
    public void update(ThrottlingPointSpecification spec) {
        // Pull out the tenant IDs for tenants that currently are rate limited
        HashSet<Integer> remainingTenants = new HashSet<Integer>(tenantThrottlers.keySet());

        // Update the limits as specified.
        for (int i = 0; i < spec.getTenantIDCount(); i++) {
            int tenantId = spec.getTenantID(i);
            setRate(tenantId, spec.getThrottlingRate(i));
            remainingTenants.remove(tenantId);
        }

        // If a tenant does not have a rate specified, then it is no longer rate
        // limited
        for (Integer tenantId : remainingTenants) {
            setRate(tenantId, Double.MAX_VALUE);
        }
    }

    @Override
    public void clearRates() {
        for (TenantThrottler throttler : tenantThrottlers.values()) {
            throttler.setRate(Double.MAX_VALUE);
        }
    }

}
