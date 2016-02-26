package edu.brown.cs.systems.retro.throttling.mclock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * For a scheduler with requests being enqueued and slots for executing
 * requests, manages the slots
 */
public class SlotManager {

    /**
     * The number of requests pending. Varies from [-totalslots, ... ) When the
     * number of requests pending is 1, it indicates a slot is not available to
     * service the request
     */
    private final AtomicLong queuesize = new AtomicLong();

    /** For adjusting the total slots */
    private final AtomicLong slotdeficit = new AtomicLong();

    /** The maximum number of slots allowed */
    private long maxSlots;

    public SlotManager(long maxSlots) {
        queuesize.set(-maxSlots);
        this.maxSlots = maxSlots;
    }

    /**
     * Indicate that a request is available to be scheduled.
     * 
     * @return true if there is a request available to be scheduled and a slot
     *         is also available. the slot and request will be removed from the
     *         manager if so.
     */
    public boolean addRequest() {
        return queuesize.getAndIncrement() < 0;
    }

    /**
     * Indicate that a slot is available for a request.
     * 
     * @return true if there is a request available to be scheduled and a slot
     *         is also available. the slot and request will be removed from the
     *         manager if so.
     */
    public boolean addSlot() {
        return addSlots(1) == 1;
    }

    /**
     * @param maxSlots
     *            the maximum slots that can be scheduled at any given time
     * @return the the number of requests that can now be scheduled
     */
    public synchronized long setMaxSlots(long maxSlots) {
        long toschedule = addSlots(maxSlots - this.maxSlots);
        this.maxSlots = maxSlots;
        return toschedule;
    }

    private long addSlots(long delta) {
        // do nothing if no change
        if (delta == 0) {
            return 0;
        }

        // if this is negative, add to the deficit
        if (delta < 0) {
            slotdeficit.addAndGet(-delta);
            return 0;
        }

        // pay the deficit
        long deficit;
        while ((deficit = slotdeficit.get()) > 0) {
            if (deficit < delta) {
                if (slotdeficit.compareAndSet(deficit, 0)) {
                    delta = delta - deficit;
                    continue;
                }
            } else {
                if (slotdeficit.compareAndSet(deficit, deficit - delta)) {
                    return 0;
                }
            }
        }

        // schedule remaining
        long enqueued, newenqueued;
        do {
            enqueued = queuesize.get();
            newenqueued = enqueued - delta;
            if (newenqueued > enqueued) {
                // long underflow
                enqueued = Long.MIN_VALUE;
            }
        } while (!queuesize.compareAndSet(enqueued, newenqueued));

        // return the number of requests that can be scheduled
        if (enqueued > delta)
            return delta;
        else if (enqueued > 0)
            return enqueued;
        else
            return 0;
    }

}
