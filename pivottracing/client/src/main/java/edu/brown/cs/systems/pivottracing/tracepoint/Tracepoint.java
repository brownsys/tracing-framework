package edu.brown.cs.systems.pivottracing.tracepoint;

import java.util.List;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;

public interface Tracepoint {
    
    public String getName();
    public boolean exports(String varName);
    public List<TracepointSpec> getTracepointSpecsForAdvice(AdviceSpec advice);

}
