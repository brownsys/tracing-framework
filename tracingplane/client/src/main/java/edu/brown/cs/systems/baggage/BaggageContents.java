package edu.brown.cs.systems.baggage;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

/** Static API for manipulating the contents of the thread's current baggage */
public class BaggageContents {

    private BaggageContents() {}

    /** @return The current baggage, or a new one, set for the current thread, if it did not exist */
    private static BaggageImpl getOrCreate() {
        BaggageImpl impl = Baggage.current.get();
        if (impl == null) {
            impl = new BaggageImpl();
            Baggage.current.set(impl);
        }
        return impl;
    }
    
    /** Returns true if this baggage is empty, false otherwise */
    public static boolean isEmpty() {
        BaggageImpl impl = Baggage.current.get();
        return impl == null || impl.isEmpty();
    }

    /** Does the thread's current baggage have any values stored under the provided namespace/key combination.
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @return true if the thread's current baggage contains values for the key, false otherwise */
    public static boolean contains(ByteString namespace, ByteString key) {
        BaggageImpl impl = Baggage.current.get();
        return impl == null ? false : impl.contains(namespace, key);
    }

    /** Get the values for a specified key from the current baggage
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @return An unmodifiable set, possibly empty, containing the values of the specified key */
    public static Set<ByteString> get(ByteString namespace, ByteString key) {
        BaggageImpl impl = Baggage.current.get();
        return impl == null ? Collections.<ByteString> emptySet()
                : Collections.unmodifiableSet(impl.get(namespace, key));
    }

    /** Calls {@link get}, serializing the namespace and key to bytes using {@link ByteString.copyFromUtf8()} */
    public static Set<ByteString> get(String namespace, String key) {
        if (namespace == null || key == null) {
            return Collections.emptySet();
        }
        return get(ByteString.copyFromUtf8(namespace), ByteString.copyFromUtf8(key));
    }

    /** Convenience method to get kvs with strings instead of bytestrings. Uses ByteString.copyFromUtf8(string)
     * Interprets stored bytestrings as strings */
    public static Set<String> getStrings(String namespace, String key) {
        Set<String> strings = Sets.newHashSet();
        for (ByteString bytestring : get(namespace, key)) {
            strings.add(bytestring.toStringUtf8());
        }
        return strings;
    }

    /** Convenience method to put kvs using strings instead of bytestrings Uses ByteString.copyFromUtf8(string)
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @param value */
    public static void add(String namespace, String key, String value) {
        if (namespace != null && key != null && value != null) {
            add(ByteString.copyFromUtf8(namespace), ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value));
        }
    }

    /** Add a value to the current baggage
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @param value The value to add */
    public static void add(ByteString namespace, ByteString key, ByteString value) {
        getOrCreate().add(namespace, key, value);
    }

    /** Replace the value for a key in the current baggage
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @param value The value to set for the key, removing previous values */
    public static void replace(ByteString namespace, ByteString key, ByteString value) {
        getOrCreate().replace(namespace, key, value);
    }

    /** Replace values for a key in the current baggage
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @param values The values to set for the key, removing previous values */
    public static void replace(ByteString namespace, ByteString key, Iterable<? extends ByteString> values) {
        getOrCreate().replace(namespace, key, values);
    }

    /** Remove all values for the specified key from the current baggage
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up */
    public static void remove(ByteString namespace, ByteString key) {
        BaggageImpl impl = Baggage.current.get();
        if (impl != null) {
            impl.remove(namespace, key);
        }
    }

    /** Returns a view Set of distinct keys under the namespace from the current baggage
     * 
     * @param namespace The namespace to look up
     * @return all the distinct keys under the given namespace */
    public static Set<ByteString> keys(ByteString namespace) {
        BaggageImpl impl = Baggage.current.get();
        return impl == null ? Collections.<ByteString> emptySet() : impl.keys(namespace);
    }

    /** Remove all key-value pairs under a namespace
     * 
     * @param namespace the namespace to remove from the baggage */
    public static void removeAll(ByteString namespace) {
        BaggageImpl impl = Baggage.current.get();
        if (impl != null) {
            impl.removeAll(namespace);
        }
    }

    /** Remove all key-value pairs under a namespace. Interprets the provided namespace as utf8 bytes
     * 
     * @param namespace the namespace to remove from the baggage */
    public static void removeAll(String namespace) {
        if (namespace != null) {
            removeAll(ByteString.copyFromUtf8(namespace));
        }
    }

    /** An accessor to key-value pairs of a namespace; provided as a convenient alternative to methods that require the
     * namespace explicitly.  The returned Namespace object always access the thread's current baggage.
     * 
     * @param namespace The namespace to access
     * @return a Bag wrapper object for access to the namespace */
    public static Namespace<ByteString, ByteString> getNamespace(ByteString namespace) {
        return new ByteStringNamespace(namespace);
    }

    /** An accessor to key-value pairs of a namespace; provided as a convenient alternative to methods that require the
     * namespace explicitly.  The returned Namespace object always access the thread's current baggage.
     * 
     * @param namespace The namespace to access
     * @return a Bag wrapper object for access to the namespace */
    public static Namespace<ByteString, ByteString> getNamespace(String namespace) {
        return new ByteStringNamespace(namespace);
    }

}
