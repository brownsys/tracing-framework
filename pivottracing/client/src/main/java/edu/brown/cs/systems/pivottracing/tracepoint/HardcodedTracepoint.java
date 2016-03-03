package edu.brown.cs.systems.pivottracing.tracepoint;

import java.util.List;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;

public class HardcodedTracepoint implements Tracepoint {
    
    private final String name, id;
    private final List<String> exports;
    
    private HardcodedTracepoint(String name, String id, String... exports) {
        this.name = name;
        this.id = id;
        this.exports = Lists.newArrayList(exports);
    }
    
    public static HardcodedTracepoint get(String id, String... exports) {
        return new HardcodedTracepoint(id, id, exports);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean exports(String varName) {
        return exports.contains(varName);
    }

    @Override
    public List<TracepointSpec> getTracepointSpecsForAdvice(AdviceSpec advice) {
        TracepointSpec.Builder spec = TracepointSpec.newBuilder();
        spec.getHardcodedTracepointBuilder().setId(id).addAllExport(exports);
        return Lists.newArrayList(spec.build());
    }

}
