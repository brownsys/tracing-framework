package edu.brown.cs.systems.retro.wrappers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.RWLockResource;

public class ReentrantReadWriteLockWrapper extends ReentrantReadWriteLock {

    private final String lockid;
    private final RWLockResource resource;
    private final ReadLockUtilization utilization = new ReadLockUtilization();

    private final ThreadLocal<Long> request = new ThreadLocal<Long>();
    private final ThreadLocal<Long> acquire = new ThreadLocal<Long>();
    private final ThreadLocal<Long> release = new ThreadLocal<Long>();

    public final ReadLock readLock;
    public final WriteLock writeLock;

    public ReentrantReadWriteLockWrapper(JoinPoint.StaticPart jp) {
        this(false, jp);
    }

    public ReentrantReadWriteLockWrapper(boolean fair, JoinPoint.StaticPart jp) {
        super(fair);
        this.readLock = new WrappedReadLock(this);
        this.writeLock = new WrappedWriteLock(this);
        this.lockid = "ReentrantReadWriteLock-" + jp.getSourceLocation().toString() + "-" + System.identityHashCode(this);
        this.resource = new RWLockResource(this.lockid);
    }

    @Override
    public ReentrantReadWriteLock.ReadLock readLock() {
        return this.readLock;
    }

    @Override
    public ReentrantReadWriteLock.WriteLock writeLock() {
        return this.writeLock;
    }

    private boolean requesting(RWLockResource.Lock l) {
        if (getReadHoldCount() == 0 && getWriteHoldCount() == 0) {
            long request_hrt = System.nanoTime();
            request.set(request_hrt);
            l.request(request_hrt, JoinPointTracking.Caller.get(null));
            return true;
        }
        return false;
    }

    private void acquired(boolean wasFirstRequest, RWLockResource.Lock l) {
        if (wasFirstRequest) {
            long acquire_hrt = System.nanoTime();
            acquire.set(acquire_hrt);
            l.acquire(request.get(), acquire_hrt, JoinPointTracking.Caller.get(null));
        }
    }

    private void released(RWLockResource.Lock l) {
        if (getReadHoldCount() == 0 && getWriteHoldCount() == 0) {
            long release_hrt = System.nanoTime();
            release.set(release_hrt);
            l.release(request.get(), acquire.get(), release_hrt, JoinPointTracking.Caller.get(null));
        }
    }

    private class ReadLockUtilization {

        private volatile long first_acquire_hrt;
        private final AtomicInteger counter = new AtomicInteger();

        public void readlockacquire() {
            int previous_count = counter.getAndIncrement();
            if (previous_count == 0) {
                first_acquire_hrt = System.nanoTime();
                resource.SharedRead.acquire();
            }
        }

        public void readlockrelease() {
            long acquired_hrt = first_acquire_hrt;
            int current_count = counter.decrementAndGet();
            if (current_count == 0) {
                long released_hrt = System.nanoTime();
                resource.SharedRead.release(acquired_hrt, released_hrt);
            }
        }

    }

    public static class WrappedReadLock extends ReadLock {
        private final ReentrantReadWriteLockWrapper lock;

        protected WrappedReadLock(ReentrantReadWriteLockWrapper lock) {
            super(lock);
            this.lock = lock;
        }

        @Override
        public void lock() {
            boolean was_first_request = lock.requesting(lock.resource.Read);
            super.lock();
            lock.utilization.readlockacquire();
            lock.acquired(was_first_request, lock.resource.Read);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            boolean was_first_request = lock.requesting(lock.resource.Read);
            try {
                super.lockInterruptibly();
                lock.utilization.readlockacquire();
                lock.acquired(was_first_request, lock.resource.Read);
            } catch (InterruptedException e) {
                lock.acquired(was_first_request, lock.resource.Read);
                lock.released(lock.resource.Read);
                throw e;
            }
        }

        @Override
        public boolean tryLock() {
            boolean was_first_request = lock.requesting(lock.resource.Read);
            boolean success = super.tryLock();
            if (success) {
                lock.utilization.readlockacquire();
                lock.acquired(was_first_request, lock.resource.Read);
            } else {
                lock.acquired(was_first_request, lock.resource.Read);
                lock.released(lock.resource.Read);
            }
            return success;
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            boolean was_first_request = lock.requesting(lock.resource.Read);
            try {
                boolean success = super.tryLock(timeout, unit);
                if (success) {
                    lock.utilization.readlockacquire();
                    lock.acquired(was_first_request, lock.resource.Read);
                } else {
                    lock.acquired(was_first_request, lock.resource.Read);
                    lock.released(lock.resource.Read);
                }
                return success;
            } catch (InterruptedException e) {
                lock.acquired(was_first_request, lock.resource.Read);
                lock.released(lock.resource.Read);
                throw e;
            }
        }

        @Override
        public void unlock() {
            super.unlock();
            lock.utilization.readlockrelease();
            lock.released(lock.resource.Read);
        }

    }

    public static class WrappedWriteLock extends WriteLock {
        private ReentrantReadWriteLockWrapper lock;

        protected WrappedWriteLock(ReentrantReadWriteLockWrapper lock) {
            super(lock);
            this.lock = lock;
        }

        @Override
        public void lock() {
            boolean was_first_request = lock.requesting(lock.resource.Write);
            super.lock();
            lock.acquired(was_first_request, lock.resource.Write);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            boolean was_first_request = lock.requesting(lock.resource.Write);
            try {
                super.lockInterruptibly();
                lock.acquired(was_first_request, lock.resource.Write);
            } catch (InterruptedException e) {
                lock.acquired(was_first_request, lock.resource.Write);
                lock.released(lock.resource.Write);
                throw e;
            }
        }

        @Override
        public boolean tryLock() {
            boolean was_first_request = lock.requesting(lock.resource.Write);
            boolean success = super.tryLock();
            lock.acquired(was_first_request, lock.resource.Write);
            if (!success)
                lock.released(lock.resource.Write);
            return success;
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            boolean was_first_request = lock.requesting(lock.resource.Write);
            try {
                boolean success = super.tryLock(timeout, unit);
                lock.acquired(was_first_request, lock.resource.Write);
                if (!success)
                    lock.released(lock.resource.Write);
                return success;
            } catch (InterruptedException e) {
                lock.acquired(was_first_request, lock.resource.Write);
                lock.released(lock.resource.Write);
                throw e;
            }
        }

        @Override
        public void unlock() {
            super.unlock();
            lock.released(lock.resource.Write);
        }
    }

}
