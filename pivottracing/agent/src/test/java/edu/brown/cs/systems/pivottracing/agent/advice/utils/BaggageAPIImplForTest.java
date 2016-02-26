package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Namespace;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPIImpl;

public class BaggageAPIImplForTest extends BaggageAPIImpl {

    public final BaggageNamespaceForTest ACTIVE;
    public final BaggageNamespaceForTest ARCHIVE;

    /** API for test */
    public BaggageAPIImplForTest() {
        super(new BaggageNamespaceForTest(), new BaggageNamespaceForTest());
        this.ACTIVE = (BaggageNamespaceForTest) super.ACTIVE;
        this.ARCHIVE = (BaggageNamespaceForTest) super.ARCHIVE;
    }

    public static class BaggageNamespaceForTest implements Namespace<ByteString, ByteString> {

        public static BaggageNamespaceForTest create() {
            return new BaggageNamespaceForTest();
        }

        public static BaggageAPI createAPI(BaggageNamespaceForTest active, BaggageNamespaceForTest archive) {
            return new BaggageAPIImpl(active, archive);
        }

        public SetMultimap<ByteString, ByteString> map = HashMultimap.create();

        @Override
        public Set<ByteString> get(ByteString key) {
            return map.get(key);
        }

        @Override
        public void add(ByteString key, ByteString value) {
            map.put(key, value);
        }

        @Override
        public void replace(ByteString key, ByteString value) {
            map.removeAll(key);
            map.put(key, value);
        }

        @Override
        public void replace(ByteString key, Iterable<? extends ByteString> values) {
            map.removeAll(key);
            map.putAll(key, values);
        }

        @Override
        public void remove(ByteString key) {
            map.removeAll(key);
        }

        @Override
        public boolean has(ByteString key) {
            return map.containsKey(key);
        }

        @Override
        public Set<ByteString> keys() {
            return map.keySet();
        }

    }
}
