package edu.brown.cs.systems.pivottracing.tracepoint;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;

public class MultiTracepoint implements Tracepoint {
    
    public final String name;
    public final Collection<Tracepoint> others = Sets.newHashSet();
    
    public MultiTracepoint(String name, Tracepoint... others) {
        this(name, Lists.newArrayList(others));
    }
    
    public MultiTracepoint(String name, Collection<Tracepoint> others) {
        this.name = name;
        this.others.addAll(others);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean exports(String varName) {
        for (Tracepoint t : others) {
            if (!t.exports(varName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<TracepointSpec> getTracepointSpecsForAdvice(AdviceSpec advice) {
        List<TracepointSpec> specs = Lists.newArrayList();
        for (Tracepoint t : others) {
            specs.addAll(t.getTracepointSpecsForAdvice(advice));
        }
        return specs;
    }

}
