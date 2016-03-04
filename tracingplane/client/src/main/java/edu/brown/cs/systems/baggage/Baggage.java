package edu.brown.cs.systems.baggage;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.DetachedBaggage.StringEncoding;

/** Provides the static API for propagating baggage through an application. When baggage is attached to a thread, it is
 * stored in a thread local variable, and persists until it is explicitly detached or discarded.
 * 
 * Baggage contents can be accessed with the {@link BaggageContents} static API. */
public class Baggage {

    /** Trace context of the current thread; can be null */
    static final ThreadLocal<BaggageImpl> current = new ThreadLocal<BaggageImpl>();

    /** This class is not instantiable */
    private Baggage() {}

    /** Discards any active baggage currently in this thread */
    public static void start() {
        current.remove();
    }

    /** Discards any active baggage currently in this thread */
    public static void discard() {
        current.remove();
    }

    /** Detaches and returns the current thread's baggage. The returned baggage can be stored for later, passed to a new
     * thread, serialized, and reattached to a thread later. After invoking this method, the current thread's baggage
     * will be empty.
     * 
     * @return a {@link DetachedBaggage} that contains the data previously stored in the current thread's baggage. */
    public static DetachedBaggage stop() {
        try {
            return DetachedBaggage.wrap(current.get());
        } finally {
            current.remove();
        }
    }

    /** Discards the thread's current baggage and replaces it with the provided baggage. If the provided baggage is
     * null, this call is equivalent to {@link Discard()} Invoking this method will remove the contents of the provided
     * baggage object.
     * 
     * @param baggage The baggage to set as the thread's current baggage. */
    public static void start(DetachedBaggage baggage) {
        BaggageImpl impl = null;
        if (baggage != null) {
            impl = baggage.impl;
            baggage.impl = null;
        }
        current.set(impl);
    }

    /** Swap the thread's current baggage, replacing it with the provided baggage. If the provided baggage is null, this
     * call is equivalent to {@link Discard()}. Returns the baggage that was previously the thread's current baggage.
     * @param baggage The baggage to set as the thread's current baggage
     * @return The previous current baggage */
    public static DetachedBaggage swap(DetachedBaggage baggage) {
        BaggageImpl prev = current.get();
        start(baggage);
        return DetachedBaggage.wrap(prev);
    }

    /** Discards the thread's current baggage. Deserializes the provided bytes. Attaches the deserialized baggage to
     * this thread. If the provided bytes aren't valid, this call has equivalent behavior to {@link Discard()}. It is
     * strongly advised not to attach the same bytes multiple times. Instead, call the {@link duplicateContents()}
     * method of {@link DetachedBaggage} to create the necessary copies.
     * 
     * @param baggageBytes The serialized baggage to set as the thread's current baggage. */
    public static void start(ByteString baggageBytes) {
        start(DetachedBaggage.deserialize(baggageBytes));
    }

    /** Discards the thread's current baggage. Deserializes the provided bytes. Attaches the deserialized baggage to
     * this thread. If the provided bytes aren't valid, this call has equivalent behavior to {@link Discard()}. It is
     * strongly advised not to attach the same bytes multiple times. Instead, call the {@link duplicateContents()}
     * method of {@link DetachedBaggage} to create the necessary copies.
     * 
     * @param baggageBytes The serialized baggage to set as the thread's current baggage. */
    public static void start(byte[] baggageBytes) {
        start(DetachedBaggage.deserialize(baggageBytes));
    }

    /** Discards the thread's current baggage. Decodes the provided string as base64 encoded bytes. Attaches the decoded
     * baggage to this thread. If the provided string isn't valid, this call has equivalent behavior to
     * {@link Discard()}. It is strongly advised not to attach the same bytes multiple times. Instead, call the
     * {@link duplicateContents()} method of {@link DetachedBaggage} to create the necessary copies.
     * 
     * @param baggage The encoded baggage to set as the thread's current baggage. */
    public static void start(String base64Baggage) {
        start(DetachedBaggage.decode(base64Baggage, StringEncoding.BASE64));
    }

    /** Creates and returns a duplicate copy of the current thread's baggage. The duplicated baggage will contain copies
     * of all data present in the thread's current baggage. After this call completes, the thread's current baggage will
     * remain attached and its contents will be unmodified.
     * 
     * @return A detached baggage containing duplicated values */
    public static DetachedBaggage fork() {
        return DetachedBaggage.wrap(current.get()).split();
    }

    /** Merge the contents of the provided baggage into the current thread's baggage. After this call completes, the
     * thread's current baggage will contain all previous contents plus the contents of the other baggage. The contents
     * of the other DetachedBaggage will be removed, so the DetachedBaggage should now be discarded.
     * 
     * @param otherBaggage A DetachedBaggage */
    public static void join(DetachedBaggage otherBaggage) {
        if (otherBaggage == null || otherBaggage.impl == null) {
            return;
        }
        BaggageImpl impl = current.get();
        if (impl == null) {
            current.set(otherBaggage.impl);
        } else {
            impl.merge(otherBaggage.impl);
        }
        otherBaggage.impl = null;
    }

    /** Deserialize the provided baggage and merge its contents into the current thread's baggage. */
    public static void join(byte[] baggageBytes) {
        join(DetachedBaggage.deserialize(baggageBytes));
    }

    /** Deserialize the provided baggage and merge its contents into the current thread's baggage. */
    public static void join(ByteString baggageBytes) {
        join(DetachedBaggage.deserialize(baggageBytes));
    }

    /** Decode the provided baggage and merge its contents into the current thread's baggage. */
    public static void join(String base64baggage) {
        join(DetachedBaggage.decode(base64baggage, StringEncoding.BASE64));
    }

}
