package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;

public class BaggageAPIDisabled implements BaggageAPI {

    public static final Pack disabledPack = new Pack() {
        public void pack(List<Object[]> tuples) {}
    };

    public static final Unpack disabledUnpack = new Unpack() {
        private final Object[][] noTuples = new Object[0][];
        public Object[][] unpack() {
            return noTuples;
        }
    };

    public Pack create(final PackSpec spec) throws InvalidAdviceException {
        return disabledPack;
    }

    public Unpack create(UnpackSpec spec) throws InvalidAdviceException {
        return disabledUnpack;
    }

}
