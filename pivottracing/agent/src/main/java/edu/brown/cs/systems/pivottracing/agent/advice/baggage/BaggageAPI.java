package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;

public interface BaggageAPI {
    
    public interface Pack {
        public void pack(List<Object[]> tuples);
    }
    
    public interface Unpack {
        public Object[][] unpack();
    }
    
    public Pack create(PackSpec spec) throws InvalidAdviceException;
    public Unpack create(UnpackSpec spec) throws InvalidAdviceException;
    
}
