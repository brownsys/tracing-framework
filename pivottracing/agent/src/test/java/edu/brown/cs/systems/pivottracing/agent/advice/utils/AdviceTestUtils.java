package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec.Builder;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.AdviceImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI;

/** Utility class for creating advice for tests */
public class AdviceTestUtils {
    
    public static AdviceSpecBuilder newAdvice() {
        return new AdviceSpecBuilder();
    }
    
    public static class AdviceSpecBuilder {
        private Builder b = AdviceSpec.newBuilder();
        public AdviceSpecBuilder observe(String... toObserve) {
            return observe(Lists.newArrayList(toObserve));
        }
        public AdviceSpecBuilder observe(List<String> toObserve) {
            b.getObserveBuilder().addAllVar(toObserve);
            return this;
        }
        public AdviceSpecBuilder unpack(String bag, String... which) {
            UnpackSpec.Builder u = b.addUnpackBuilder();
            u.setBagId(ByteString.copyFromUtf8(bag));
            u.getTupleSpecBuilder().addAllVar(Lists.newArrayList(which));
            return this;
        }
        public AdviceSpecBuilder unpack(String bag, Filter f, String... which) {
            UnpackSpec.Builder u = b.addUnpackBuilder();
            u.setBagId(ByteString.copyFromUtf8(bag));
            u.getFilterSpecBuilder().setFilter(f).addAllVar(Lists.newArrayList(which));
            return this;
        }
        public AdviceSpecBuilder unpack(String bag, String[] groupBy, String[] aggregate, Agg[] how) {
            UnpackSpec.Builder u = b.addUnpackBuilder();
            u.setBagId(ByteString.copyFromUtf8(bag));
            GroupBySpec.Builder gb = u.getGroupBySpecBuilder().addAllGroupBy(Lists.newArrayList(groupBy));
            for (int i = 0; i < aggregate.length && i < how.length; i++) {
                gb.addAggregateBuilder().setHow(how[i]).setName(aggregate[i]);
            }
            return this;
        }
        public AdviceSpecBuilder where(String predicate, String... args) {
            b.addWhereBuilder().setPredicate(predicate).addAllReplacementVariables(Lists.newArrayList(args));
            return this;
        }
        public AdviceSpecBuilder let(String var, String expression, String... args) {
            b.addLetBuilder().setVar(var).setExpression(expression).addAllReplacementVariables(Lists.newArrayList(args));
            return this;
        }
        public AdviceSpecBuilder pack(String bag, String... which) {
            PackSpec.Builder p = b.getPackBuilder().setBagId(ByteString.copyFromUtf8(bag));
            p.getTupleSpecBuilder().addAllVar(Lists.newArrayList(which));
            return this;
        }
        public AdviceSpecBuilder pack(String bag, Filter f, String... which) {
            return pack(bag, f, Lists.newArrayList(which));
        }
        public AdviceSpecBuilder pack(String bag, String first, String second, Agg aggsecond) {
            PackSpec.Builder p = b.getPackBuilder().setBagId(ByteString.copyFromUtf8(bag));
            p.getGroupBySpecBuilder().addGroupBy(first).addAggregateBuilder().setName(second).setHow(aggsecond);
            return this;
        }
        public AdviceSpecBuilder pack(String bag, Filter f, List<String> which) {
            PackSpec.Builder p = b.getPackBuilder().setBagId(ByteString.copyFromUtf8(bag));
            p.getFilterSpecBuilder().setFilter(f).addAllVar(which);
            return this;
        }
        public AdviceSpecBuilder emit(String name, String... which) {
            return emit(name, Lists.newArrayList(which));
        }
        public AdviceSpecBuilder emit(String name, String first, String second, Agg aggsecond) {
            EmitSpec.Builder e = b.getEmitBuilder().setOutputId(ByteString.copyFromUtf8(name));
            e.getGroupBySpecBuilder().addGroupBy(first).addAggregateBuilder().setName(second).setHow(aggsecond);
            return this;
        }
        public AdviceSpecBuilder emit(String name, List<String> which) {
            b.getEmitBuilder().setOutputId(ByteString.copyFromUtf8(name)).getTupleSpecBuilder().addAllVar(which);
            return this;
        }
        public AdviceSpecBuilder emit(String name, String[] groupBy, String[] aggregate, Agg[] how) {
            EmitSpec.Builder e = b.getEmitBuilder();
            e.setOutputId(ByteString.copyFromUtf8(name));
            GroupBySpec.Builder gb = e.getGroupBySpecBuilder().addAllGroupBy(Lists.newArrayList(groupBy));
            for (int i = 0; i < aggregate.length && i < how.length; i++) {
                gb.addAggregateBuilder().setHow(how[i]).setName(aggregate[i]);
            }
            return this;
        }
        public AdviceSpec spec() {
            return b.build();
        }
        public Advice build(BaggageAPI baggage, EmitAPI sender) throws InvalidAdviceException {
            return new AdviceImpl(b.build(), baggage, sender);
        }
    }
    
    public static class MethodTracepoints {
        private MethodTracepointSpec.Builder b = MethodTracepointSpec.newBuilder();
        public static MethodTracepoints forClass(String className) {
            MethodTracepoints x = new MethodTracepoints();
            x.b.setClassName(className);
            return x;
        }
        public static MethodTracepoints forClass(Class<?> cls) {
            return forClass(cls.getName());
        }
        public MethodTracepoints method(String methodName) {
            b.setMethodName(methodName);
            return this;
        }
        public MethodTracepoints args(Object... args) {
            for (Object arg : args) {
                if (arg instanceof Class<?>) {
                    Class<?> cls = (Class<?>) arg;
                    b.addParamClass(cls.getName());
                } else if (arg instanceof String) {
                    b.addParamClass((String) arg);
                } else {
                    b.addParamClass(arg.getClass().getName());
                }
            }
            return this;
        }
        public MethodTracepoints export(String... exports) {
            for (String export : exports) {
                b.addAdviceArgBuilder().setLiteral(export);
            }
            return this;
        }
        public MethodTracepoints export(String literal, String f, String typ) {
            b.addAdviceArgBuilder().getMultiBuilder().setLiteral(literal).setPostProcess(f).setType(typ);
            return this;
        }
        public MethodTracepointSpec buildSpec() {
            return b.build();
        }
        public TracepointSpec build() {
            return TracepointSpec.newBuilder().setMethodTracepoint(b).build();
        }
    }

}
