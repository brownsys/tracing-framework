package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import java.util.List;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI;
import junit.framework.TestCase;

public class EmitAPIForTest implements EmitAPI {
    
    public final List<Object[]> emitted = Lists.newArrayList();
    public final List<Object[]> expected = Lists.newArrayList();
    public final List<Emit> created = Lists.newArrayList();
    public final List<Emit> destroyed = Lists.newArrayList();

    @Override
    public Emit create(EmitSpec spec) throws InvalidAdviceException {
        Emit emit = new Emit() {
            public void emit(List<Object[]> tuples) {
                emitted.addAll(tuples);
            }
        };
        created.add(emit);
        return emit;
    }
    
    public void expectTuple(List<? extends Object> tuple) {
        expected.add(tuple.toArray());
    }
    
    public void expectTuple(Object... tuple) {
        expected.add(tuple);
    }
    
    public void check() {
        TestCase.assertEquals(expected.size(), emitted.size());
        int tupleSize = -1;
        for (int i = 0; i < emitted.size(); i++) {
            Object[] emit = emitted.get(i);
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

    @Override
    public void destroy(Emit emit) {
        destroyed.add(emit);
    }

}
