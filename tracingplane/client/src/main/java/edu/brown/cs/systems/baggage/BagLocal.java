package edu.brown.cs.systems.baggage;

import java.util.Set;

import com.google.protobuf.ByteString;

public abstract class BagLocal<T> {
    
    public final ByteString namespace;
    public final ByteString key;
    
    public BagLocal(ByteString namespace, ByteString key) {
        this.namespace = namespace;
        this.key = key;
    }
    
    public abstract T get();
    
    public abstract void set(T value);
    
    protected boolean hasValue() {
        return BaggageContents.contains(namespace, key);
    }
    
    protected void set(ByteString value) {
        BaggageContents.replace(namespace, key, value);
    }
    
    protected void set(Iterable<ByteString> values) {
        BaggageContents.replace(namespace, key, values);
    }
    
    public abstract ByteString serialize(T value);
    
    public abstract T deserialize(ByteString bytes);
    
    public T resolve(Set<ByteString> serializedVersions) {
        return serializedVersions.isEmpty() ? null : deserialize(serializedVersions.iterator().next());
    }

}
