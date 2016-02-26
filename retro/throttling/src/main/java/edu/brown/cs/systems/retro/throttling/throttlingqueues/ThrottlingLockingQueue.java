package edu.brown.cs.systems.retro.throttling.throttlingqueues;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.NotImplementedException;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.ThrottlingPointAggregator;
import edu.brown.cs.systems.retro.throttling.ClassChecker;
import edu.brown.cs.systems.retro.throttling.ThrottlingQueue;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} of <tt>Delayed</tt>
 * elements, in which an element can only be taken when its delay has expired.
 * The <em>head</em> of the queue is that <tt>Delayed</tt> element whose delay
 * expired furthest in the past. If no delay has expired there is no head and
 * <tt>poll</tt> will return <tt>null</tt>. Expiration occurs when an element's
 * <tt>getDelay(TimeUnit.NANOSECONDS)</tt> method returns a value less than or
 * equal to zero. Even though unexpired elements cannot be removed using
 * <tt>take</tt> or <tt>poll</tt>, they are otherwise treated as normal
 * elements. For example, the <tt>size</tt> method returns the count of both
 * expired and unexpired elements. This queue does not permit null elements.
 *
 * <p>
 * This class and its iterator implement all of the <em>optional</em> methods of
 * the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>
 * This class is a member of the <a href="{@docRoot}
 * /../technotes/guides/collections/index.html"> Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E>
 *            the type of elements held in this collection
 */

public class ThrottlingLockingQueue<E> extends AbstractQueue<E> implements ThrottlingQueue<E> {

    private final ClassChecker hadoop_ipc_class_checker_hack = new ClassChecker();

    public volatile int size = 0;
    private final long window;
    private final ThrottlingPointAggregator taggregator;

    public ThrottlingLockingQueue(String queueid) {
        this(queueid, 1000000000); // default 1 second history (1 billion
                                   // nanoseconds)
    }

    /**
     * @param historySize
     *            up to historySizeNanos nanoseconds grace period
     */
    public ThrottlingLockingQueue(String queueid, long historySizeNanos) {
        this.window = historySizeNanos;
        this.taggregator = LocalResources.getThrottlingPointAggregator(queueid);
    }

    private long now() {
        return System.nanoTime();
    }

    private class TenantQueue {

        public final int tenantId;

        public volatile long delay_ns = 0;
        public volatile long next = 0;

        public final Queue<Item> elements = new ArrayDeque<Item>();

        public class Item implements Comparable<Item> {
            public final E element;
            public final long enqueue;
            public long available;
            public final TenantQueue q = TenantQueue.this;

            public Item(E element) {
                this.element = element;
                this.enqueue = System.nanoTime();
            }

            @Override
            public int compareTo(Item other) {
                return Long.compare(this.enqueue, other.enqueue);
            }

            @Override
            public boolean equals(Object other) {
                return other == null ? false : other.equals(element);
            }
        }

        public TenantQueue(int tenantId) {
            this.tenantId = tenantId;
        }

        public boolean offer(E e, long t) {
            elements.add(new Item(e));
            size++;
            if (elements.size() == 1)
                next = Math.max(t - window, next);
            return true;
        }

        public E peek() {
            Item peeked = elements.peek();
            return peeked == null ? null : peeked.element;
        }

        public Item poll(long t) {
            Item polled = elements.poll();
            if (polled == null)
                return null;
            size--;
            polled.available = next; // record when the element was actually
                                     // available for deq
            next = Math.max(t - window, next + delay_ns);
            return polled;
        }

        public void clear() {
            size -= elements.size();
            elements.clear();
        }

        public void setRate(double rate_per_second) {
            long new_delay_ns = (long) Math.floor(1000 * 1000 * 1000 / rate_per_second);
            next = next - delay_ns + new_delay_ns;
            delay_ns = new_delay_ns;
        }

        public void clearRate() {
            next -= delay_ns;
            this.delay_ns = 0;
        }

        /**
         * returns when the queue will next be available to pull from, or if it
         * can be pulled from now, when the available element was enqueued
         */
        public long available(long t) {
            return (elements.size() == 0) ? Long.MAX_VALUE : ((next <= t) ? elements.peek().enqueue : next);
        }

        public Object[] toArray() {
            return elements.toArray();
        }

        public boolean remove(Object o) {
            return elements.remove(o);
        }

    }

    private transient final ReentrantLock lock = new ReentrantLock();
    private final Map<Integer, TenantQueue> qs = new HashMap<Integer, TenantQueue>();

    public int tenant() {
        return Retro.getTenant();
    }

    private TenantQueue queue(int tenantId) {
        TenantQueue q = qs.get(tenantId);
        if (q == null)
            qs.put(tenantId, q = new TenantQueue(tenantId));
        return q;
    }

    private TenantQueue nextQueue(long now) {
        long available = Long.MAX_VALUE;
        TenantQueue next = null;

        for (TenantQueue q : qs.values()) {
            long qavailable = q.available(now);
            if (qavailable < available) {
                available = qavailable;
                next = q;
            }
        }

        return next;
    }

    private E peekNext(long now) {
        TenantQueue nextQueue = nextQueue(now);
        return nextQueue == null ? null : nextQueue.peek();
    }

    private boolean hasNext() {
        return size > 0;
    }

    private E done(TenantQueue.Item item, long t) {
        if (item == null)
            return null;

        Retro.setTenant(item.q.tenantId);

        taggregator.throttled(item.q.tenantId, 0L); // TODO track latency
        if (hadoop_ipc_class_checker_hack.isHadoopIPCCall(item.element))
            hadoop_ipc_class_checker_hack.setCallEnqueue(item.element, Math.max(item.enqueue, item.available));
        if (hadoop_ipc_class_checker_hack.isHBaseCallRunner(item.element))
            hadoop_ipc_class_checker_hack.setCallRunnerEnqueue(item.element, Math.max(item.enqueue, item.available));

        return item.element;
    }

    /**
     * Thread designated to wait for the element at the head of the queue. This
     * variant of the Leader-Follower pattern
     * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to minimize
     * unnecessary timed waiting. When a thread becomes the leader, it waits
     * only for the next delay to elapse, but other threads await indefinitely.
     * The leader thread must signal some other thread before returning from
     * take() or poll(...), unless some other thread becomes leader in the
     * interim. Whenever the head of the queue is replaced with an element with
     * an earlier expiration time, the leader field is invalidated by being
     * reset to null, and some waiting thread, but not necessarily the current
     * leader, is signalled. So waiting threads must be prepared to acquire and
     * lose leadership while waiting.
     */
    private Thread leader = null;

    /**
     * Condition signalled when a newer element becomes available at the head of
     * the queue or a new thread may need to become leader.
     */
    private final Condition available = lock.newCondition();

    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e
     *            the element to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws NullPointerException
     *             if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e
     *            the element to add
     * @return <tt>true</tt>
     * @throws NullPointerException
     *             if the specified element is null
     */
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        int tenant = tenant();
        lock.lock();
        try {
            long t = now();
            queue(tenant).offer(e, t);
            if (peekNext(t) == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
            taggregator.throttling(tenant);
        }
    }

    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e
     *            the element to add
     * @throws NullPointerException
     *             {@inheritDoc}
     */
    public void put(E e) {
        offer(e);
    }

    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e
     *            the element to add
     * @param timeout
     *            This parameter is ignored as the method never blocks
     * @param unit
     *            This parameter is ignored as the method never blocks
     * @return <tt>true</tt>
     * @throws NullPointerException
     *             {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    /**
     * Retrieves and removes the head of this queue, or returns <tt>null</tt> if
     * this queue has no elements with an expired delay.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue has no
     *         elements with an expired delay
     */
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        long t = now();
        TenantQueue.Item item = null;
        try {
            TenantQueue q = nextQueue(t);
            if (q == null || q.next > t)
                return null;
            else {
                item = q.poll(t);
                return item == null ? null : item.element;
            }
        } finally {
            lock.unlock();
            done(item, t);
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until
     * an element with an expired delay is available on this queue.
     *
     * @return the head of this queue
     * @throws InterruptedException
     *             {@inheritDoc}
     */
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        long t = now();
        TenantQueue.Item item = null;
        try {
            for (;;) {
                TenantQueue q = nextQueue(t);
                if (q == null)
                    available.await();
                else {
                    long delay = q.next - t;
                    if (delay <= 0) {
                        item = q.poll(t);
                        return item == null ? null : item.element;
                    } else if (leader != null)
                        available.await();
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            available.awaitNanos(delay);
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
                t = System.nanoTime();
            }
        } finally {
            if (leader == null && hasNext())
                available.signal();
            lock.unlock();
            done(item, t);
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until
     * an element with an expired delay is available on this queue, or the
     * specified wait time expires.
     *
     * @return the head of this queue, or <tt>null</tt> if the specified waiting
     *         time elapses before an element with an expired delay becomes
     *         available
     * @throws InterruptedException
     *             {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        long t = now();
        TenantQueue.Item item = null;
        try {
            for (;;) {
                TenantQueue q = nextQueue(t);
                if (q == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = q.next - t;
                    if (delay <= 0) {
                        item = q.poll(t);
                        return item == null ? null : item.element;
                    }
                    if (nanos <= 0)
                        return null;
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
                t = System.nanoTime();
            }
        } finally {
            if (leader == null && hasNext())
                available.signal();
            lock.unlock();
            done(item, t);
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue, or returns
     * <tt>null</tt> if this queue is empty. Unlike <tt>poll</tt>, if no expired
     * elements are available in the queue, this method returns the element that
     * will expire next, if one exists.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return peekNext(now());
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws UnsupportedOperationException
     *             {@inheritDoc}
     * @throws ClassCastException
     *             {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        // if (c == null)
        // throw new NullPointerException();
        // if (c == this)
        // throw new IllegalArgumentException();
        // final ReentrantLock lock = this.lock;
        // lock.lock();
        // TenantQueue.Item item = null;
        // try {
        // int n = 0;
        // long t = now();
        // for (;;) {
        // TenantQueue q = nextQueue(t);
        // if (q == null || q.next > t)
        // break;
        // c.add(q.poll(t));
        // ++n;
        // }
        // return n;
        // } finally {
        // lock.unlock();
        // }
        throw new NotImplementedException("");
    }

    /**
     * @throws UnsupportedOperationException
     *             {@inheritDoc}
     * @throws ClassCastException
     *             {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        // if (c == null)
        // throw new NullPointerException();
        // if (c == this)
        // throw new IllegalArgumentException();
        // if (maxElements <= 0)
        // return 0;
        // final ReentrantLock lock = this.lock;
        // lock.lock();
        // TenantQueue.Item item = null;
        // try {
        // int n = 0;
        // long t = now();
        // while (n < maxElements) {
        // TenantQueue q = nextQueue(t);
        // if (q == null || q.next > t)
        // break;
        // c.add(q.poll(t));
        // ++n;
        // }
        // return n;
        // } finally {
        // lock.unlock();
        // }
        throw new NotImplementedException("");
    }

    /**
     * Atomically removes all of the elements from this delay queue. The queue
     * will be empty after this call returns. Elements with an unexpired delay
     * are not waited for; they are simply discarded from the queue.
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (TenantQueue q : qs.values()) {
                q.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Always returns <tt>Integer.MAX_VALUE</tt> because a <tt>DelayQueue</tt>
     * is not capacity constrained.
     *
     * @return <tt>Integer.MAX_VALUE</tt>
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns an array containing all of the elements in this queue. The
     * returned array elements are in no particular order.
     *
     * <p>
     * The returned array will be "safe" in that no references to it are
     * maintained by this queue. (In other words, this method must allocate a
     * new array). The caller is thus free to modify the returned array.
     *
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] arr = new Object[size];
            int i = 0;
            for (TenantQueue q : qs.values()) {
                Object[] qarr = q.toArray();
                System.arraycopy(qarr, 0, arr, i, qarr.length);
                i += qarr.length;
            }
            return arr;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array. The
     * returned array elements are in no particular order. If the queue fits in
     * the specified array, it is returned therein. Otherwise, a new array is
     * allocated with the runtime type of the specified array and the size of
     * this queue.
     *
     * <p>
     * If this queue fits in the specified array with room to spare (i.e., the
     * array has more elements than this queue), the element in the array
     * immediately following the end of the queue is set to <tt>null</tt>.
     *
     * <p>
     * Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows
     * precise control over the runtime type of the output array, and may, under
     * certain circumstances, be used to save allocation costs.
     *
     * <p>
     * The following code can be used to dump a delay queue into a newly
     * allocated array of <tt>Delayed</tt>:
     *
     * <pre>
     * Delayed[] a = q.toArray(new Delayed[0]);
     * </pre>
     *
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param a
     *            the array into which the elements of the queue are to be
     *            stored, if it is big enough; otherwise, a new array of the
     *            same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException
     *             if the runtime type of the specified array is not a supertype
     *             of the runtime type of every element in this queue
     * @throws NullPointerException
     *             if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (T[]) toArray(a);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a single instance of the specified element from this queue, if it
     * is present, whether or not it has expired.
     */
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (TenantQueue q : qs.values()) {
                if (q.remove(o))
                    return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over all the elements (both expired and unexpired) in
     * this queue. The iterator does not return the elements in any particular
     * order.
     *
     * <p>
     * The returned iterator is a "weakly consistent" iterator that will never
     * throw {@link java.util.ConcurrentModificationException
     * ConcurrentModificationException}, and guarantees to traverse elements as
     * they existed upon construction of the iterator, and may (but is not
     * guaranteed to) reflect any modifications subsequent to construction.
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor; // index of next element to return;
        int lastRet; // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E) array[cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            Object x = array[lastRet];
            lastRet = -1;
            lock.lock();
            try {
                ThrottlingLockingQueue.this.remove(x);
            } finally {
                lock.unlock();
            }
        }
    }

    public void setRate(int tenantId, double rate) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            queue(tenantId).setRate(rate);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(ThrottlingPointSpecification spec) {
        // Pull out the tenant IDs for tenants that currently are rate limited
        HashSet<Integer> remainingTenants = new HashSet<Integer>(qs.keySet());

        // Update the limits as specified.
        for (int i = 0; i < spec.getTenantIDCount(); i++) {
            int tenantId = spec.getTenantID(i);
            setRate(tenantId, spec.getThrottlingRate(i));
            remainingTenants.remove(tenantId);
        }

        // If a tenant does not have a rate specified, then it is no longer rate
        // limited
        for (Integer tenantId : remainingTenants) {
            clearRate(tenantId);
        }
    }

    public void clearRate(int tenantId) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            queue(tenantId).clearRate();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clearRates() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (TenantQueue q : qs.values()) {
                q.clearRate();
            }
        } finally {
            lock.unlock();
        }
    }

}