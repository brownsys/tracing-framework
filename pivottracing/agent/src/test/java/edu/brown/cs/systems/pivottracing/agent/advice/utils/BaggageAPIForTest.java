package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import junit.framework.TestCase;

public class BaggageAPIForTest implements BaggageAPI {

    private Map<ByteString, Object[][]> bags = Maps.newHashMap();    
    
    public final List<Object[]> packed = Lists.newArrayList();
    public final List<Object[]> expected = Lists.newArrayList();
    
    public BaggageAPIForTest put(String bag, Object[][] tuples) {
        bags.put(ByteString.copyFromUtf8(bag), tuples);
        return this;
    }

    @Override
    public Pack create(PackSpec spec) throws InvalidAdviceException {
        return new Pack() {
            public void pack(List<Object[]> tuples) {
                packed.addAll(tuples);
            }
        };
    }

    @Override
    public Unpack create(final UnpackSpec spec) throws InvalidAdviceException {
        return new Unpack() {
            public Object[][] unpack() {
                return bags.get(spec.getBagId());
            }
        };
    }
    
    
    public void expect(List<? extends Object> tuple) {
        expected.add(tuple.toArray());
    }
    
    public void expect(Object... tuple) {
        expected.add(tuple);
    }
    
    public void check() {
        TestCase.assertEquals(expected.size(), packed.size());
        int tupleSize = -1;
        for (int i = 0; i < packed.size(); i++) {
            Object[] emit = packed.get(i);
            Object[] expect = expected.get(i);
            TestCase.assertEquals(expect.length, emit.length);
            for (int j = 0; j < expect.length; j++) {
                TestCase.assertEquals(expect[j], emit[j]);
            }
            if (tupleSize == -1) {
                tupleSize = emit.length;
            } else {
                TestCase.assertEquals(tupleSize, emit.length);
            }
        }
    }
}
