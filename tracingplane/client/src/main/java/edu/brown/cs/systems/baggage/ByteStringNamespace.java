package edu.brown.cs.systems.baggage;

import java.util.Set;

import com.google.protobuf.ByteString;

public class ByteStringNamespace implements Namespace<ByteString, ByteString> {

    private final ByteString namespace;

    public ByteStringNamespace(String namespace) {
        this(ByteString.copyFromUtf8(namespace));
    }

    public ByteStringNamespace(ByteString namespace) {
        this.namespace = namespace;
    }

    @Override
    public Set<ByteString> get(ByteString key) {
        return BaggageContents.get(namespace, key);
    }

    @Override
    public void add(ByteString key, ByteString value) {
        BaggageContents.add(namespace, key, value);
    }

    @Override
    public void replace(ByteString key, ByteString value) {
        BaggageContents.replace(namespace, key, value);
    }

    @Override
    public void replace(ByteString key, Iterable<? extends ByteString> values) {
        BaggageContents.replace(namespace, key, values);
    }

    @Override
    public void remove(ByteString key) {
        BaggageContents.remove(namespace, key);
    }

    @Override
    public boolean has(ByteString key) {
        return BaggageContents.contains(namespace, key);
    }

    @Override
    public Set<ByteString> keys() {
        return BaggageContents.keys(namespace);
    }

}
