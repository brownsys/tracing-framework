package edu.brown.cs.systems.baggage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageMessages.BaggageMessage;
import edu.brown.cs.systems.baggage.BaggageMessages.BaggageMessage.BagData;
import edu.brown.cs.systems.baggage.BaggageMessages.BaggageMessage.NamespaceData;

/** Simple implementation of Baggage using protocol buffers, hash multimap, and byte strings */
public class BaggageImpl {

    /** Contents of the baggage */
    final Map<ByteString, SetMultimap<ByteString, ByteString>> contents;

    BaggageImpl() {
        this.contents = Maps.newHashMap();
    }
    
    BaggageImpl(Map<ByteString, SetMultimap<ByteString, ByteString>> contents) {
        this.contents = contents;
    }

    /** Construct a BaggageMessage protobuf message and serialize it to a byte array. If this baggage is empty, an empty
     * byte array will be returned */
    public byte[] toByteArray() {
        BaggageMessage message = buildMessage();
        return message == null ? ArrayUtils.EMPTY_BYTE_ARRAY : message.toByteArray();
    }

    /** Construct a BaggageMessage protobuf message and serialize it to a byte array. If this baggage is empty, an empty
     * byte array will be returned */
    public ByteString toByteString() {
        BaggageMessage message = buildMessage();
        return message == null ? ByteString.EMPTY : message.toByteString();
    }

    /** Deserialize a baggage instance from a serialized representation. If the provided bytes are invalid, null, or
     * empty, this method will return null
     * 
     * @param byteRepr Serialized bytes, possibly null
     * @return A baggage instance if the provided bytes were valid, null otherwise */
    public static BaggageImpl deserialize(byte[] byteRepr) {
        return create(ProtobufUtils.parse(byteRepr));
    }

    /** Deserialize a baggage instance from a serialized representation. If the provided bytes are invalid, null, or
     * empty, this method will return null
     * 
     * @param byteRepr Serialized bytes, possibly null
     * @return A baggage instance if the provided bytes were valid, null otherwise */
    public static BaggageImpl deserialize(ByteString byteRepr) {
        return create(ProtobufUtils.parse(byteRepr));
    }

    /** Construct a baggage instance from the provided BaggageMessage. If the provided message is invalid, null, or
     * empty, this method will return null
     * 
     * @param message A baggagemessage protobuf
     * @return A baggage instance if the provided message is valid and has contents, null otherwise */
    static BaggageImpl create(BaggageMessage message) {
        // Check the message is not null and has some namespaces
        if (message == null || message.getNamespaceCount() == 0) {
            return null;
        }

        // Construct the baggage from the received message
        Map<ByteString, SetMultimap<ByteString, ByteString>> data = Maps.newHashMapWithExpectedSize(message.getNamespaceCount());
        for (NamespaceData namespaceData : message.getNamespaceList()) {
            SetMultimap<ByteString, ByteString> namespace = HashMultimap.create(namespaceData.getBagCount(), 1);
            for (BagData bag : namespaceData.getBagList()) {
                namespace.putAll(bag.getKey(), bag.getValueList());
            }
            if (!namespace.isEmpty()) {
                data.put(namespaceData.getKey(), namespace);
            }
        }

        // If there was no data after all, return null
        if (data.isEmpty()) {
            return null;
        }

        BaggageImpl impl = new BaggageImpl(data);
        Handlers.postDeserialize(impl);
        return impl;
    }

    /** Constructs a {@link BaggageMessage} protobuf message with the contents of this baggage. If this baggage is
     * empty, returns null */
    BaggageMessage buildMessage() {
        // Call baggage handlers
        Handlers.preSerialize(this);
        
        // Construct message
        BaggageMessage.Builder b = BaggageMessage.newBuilder();
        for (ByteString namespace : contents.keySet()) {
            SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);

            // Namespace should not exist if it has no data
            if (namespaceData.isEmpty()) {
                continue;
            }

            // Construct the namespace data message
            NamespaceData.Builder nb = b.addNamespaceBuilder().setKey(namespace);
            for (ByteString key : namespaceData.keySet()) {
                nb.addBagBuilder().setKey(key).addAllValue(namespaceData.get(key));
            }
        }

        // Return null if the baggage message is empty
        return b.getNamespaceCount() == 0 ? null : b.build();
    }

    /** Does this baggage contain anything for the specified key?
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up
     * @return true if the baggage has one or more value for this namespace and key */
    public boolean contains(ByteString namespace, ByteString key) {
        if (namespace != null && key != null) {
            SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
            return namespaceData != null && namespaceData.containsKey(key);
        }
        return false;
    }

    /** Does this baggage contain values under the specified namespace?
     * 
     * @param namespace The namespace to look up
     * @return true if the baggage has one or more values in this namespace, false otherwise */
    public boolean hasNamespace(ByteString namespace) {
        return namespace != null && contents.containsKey(namespace);
    }

    /** Get all values from the baggage for a given namespace and key
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to look up within the provided namespace
     * @return The set of all values mapped to that key */
    public Set<ByteString> get(ByteString namespace, ByteString key) {
        if (namespace != null || key != null) {
            SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
            if (namespaceData != null) {
                return namespaceData.get(key);
            }
        }
        return Collections.<ByteString> emptySet(); 
    }

    /** Remove a key from a specified namespace
     * 
     * @param namespace The namespace the key resides in
     * @param key The key to remove */
    public void remove(ByteString namespace, ByteString key) {
        if (namespace != null && key != null) {
            SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
            if (namespaceData != null) {
                namespaceData.removeAll(key);
                if (namespaceData.isEmpty()) {
                    contents.remove(namespace);
                }
            }
        }
    }

    /** Remove all values for a namespace from the baggage
     * 
     * @param namespace the namespace to remove from the baggage */
    public void removeAll(ByteString namespace) {
        if (namespace != null) {
            contents.remove(namespace);
        }
    }

    /** Set the value of a key under a namespace, removing any values previously associated with this key.
     * If the provided value is null, the previous contents are removed but the new value is ignored.
     * 
     * @param namespace The namespace the provided key resides in, 
     * @param key The key to replace values for
     * @param value The value to replace any existing values with */
    public void replace(ByteString namespace, ByteString key, ByteString value) {
        if (namespace != null && key != null) {
            if (value == null) {
                remove(namespace, key);
            } else {
                modifyKey(namespace, key).put(key, value);
            }
        }
    }

    /** Replace the values for a key with the new provided values. Null keys ignored, while null values treated as
     * removing the key
     * 
     * @param namespace The namespace the provided key resides in, 
     * @param key The key to replace values for
     * @param values These values replace existing values for the key */
    public void replace(ByteString namespace, ByteString key, Iterable<? extends ByteString> values) {
        if (namespace != null && key != null) {
            if (values == null) {
                remove(namespace, key);
            } else {
                SetMultimap<ByteString, ByteString> namespaceData = modifyKey(namespace, key);
                namespaceData.putAll(key, values);
                namespaceData.remove(key, null);
                if (namespaceData.isEmpty()) {
                    contents.remove(namespace);
                }
            }
        }
    }

    /** Add a value to a key within a specified namespace.  Nulls are ignored.
     * 
     * @param namespace The namespace the provided key resides in, 
     * @param key The key to add a value for
     * @param value The value to add */
    public void add(ByteString namespace, ByteString key, ByteString value) {
        if (namespace != null && key != null && value != null) {
            modifyNamespace(namespace).put(key, value);
        }
    }

    /** Returns a set view of the non-empty namespaces in this baggage */
    public Set<ByteString> namespaces() {
        return contents.keySet();
    }

    /** Returns a set view of the keys in the specified namespace that have 1 or more values assigned
     * 
     * @param namespace the namespace to look up
     * @return all the distinct keys under the given namespace with values assigned */
    public Set<ByteString> keys(ByteString namespace) {
        if (namespace != null) {
            SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
            if (namespaceData != null) {
                return namespaceData.keySet();
            }
        }
        return Collections.emptySet();
    }

    /** Move key-value pairs from one namespace to another.
     * 
     * @param fromNamespace The namespace to move values from
     * @param toNamespace The namespace to move values to 
     */
    public void moveEntries(ByteString fromNamespace, ByteString toNamespace) {
        if (fromNamespace == null || toNamespace == null || !contents.containsKey(fromNamespace)) {
            return; // Do nothing because fromNamespace is empty
        }
        if (!contents.containsKey(toNamespace)) {
            contents.put(toNamespace, contents.remove(fromNamespace));
        } else {
            contents.get(toNamespace).putAll(contents.remove(fromNamespace));
        }
    }

    /** Merge the contents of the other baggage into the contents of this baggage
     * 
     * @param other The other baggage to merge with */
    public void merge(BaggageImpl other) {
        if (other != null) {
            Handlers.preMerge(this, other);
            for (ByteString namespace : other.contents.keySet()) {
                SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
                if (namespaceData == null) {
                    contents.put(namespace, other.contents.get(namespace));
                } else {
                    namespaceData.putAll(other.contents.get(namespace));
                }
            }
            Handlers.postMerge(this);
        }
    }
    
    
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    /** Split this baggage into two by copying the contents over to a new instance
     * 
     * @return Another baggage impl with the same contents, copied */
    public BaggageImpl split() {
        Handlers.preSplit(this);
        Map<ByteString, SetMultimap<ByteString, ByteString>> copiedData = Maps.newHashMapWithExpectedSize(contents.size());
        for (ByteString namespace : contents.keySet()) {
            copiedData.put(namespace, HashMultimap.create(contents.get(namespace)));
        }
        BaggageImpl newImpl = new BaggageImpl(copiedData);
        Handlers.postSplit(this, newImpl);
        return newImpl;
    }

    /* Get the namespace or create one if it does not exist */
    private SetMultimap<ByteString, ByteString> modifyNamespace(ByteString namespace) {
        SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
        if (namespaceData == null) {
            namespaceData = HashMultimap.<ByteString, ByteString> create();
            contents.put(namespace, namespaceData);
        }
        return namespaceData;
    }
    
    /* Get the namespace or create one if it does not exist.  Removes existing values for a key */
    private SetMultimap<ByteString, ByteString> modifyKey(ByteString namespace, ByteString key) {
        SetMultimap<ByteString, ByteString> namespaceData = contents.get(namespace);
        if (namespaceData == null) {
            namespaceData = HashMultimap.<ByteString, ByteString> create();
            contents.put(namespace, namespaceData);
        } else {
            namespaceData.removeAll(key);
        }
        return namespaceData;
    }

}
