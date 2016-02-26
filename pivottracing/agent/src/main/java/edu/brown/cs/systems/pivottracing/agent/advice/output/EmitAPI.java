package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.List;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;

public interface EmitAPI {
    
    public interface Emit {
        public void emit(List<Object[]> tuples);
    }
    
    public Emit create(EmitSpec spec) throws InvalidAdviceException;
    public void destroy(Emit emit);
}
