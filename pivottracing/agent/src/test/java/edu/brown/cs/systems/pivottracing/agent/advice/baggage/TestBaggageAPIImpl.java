package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIImplForTest;
import junit.framework.TestCase;

public class TestBaggageAPIImpl extends TestCase {
    
    @Test
    public void testInvalidAdvice() {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        try {
            Pack pack = baggage.create(PackSpec.newBuilder().setBagId(ByteString.copyFromUtf8("test")).build());
            fail();
        } catch (InvalidAdviceException e) {
        }

        try {
            Unpack unpack = baggage.create(UnpackSpec.newBuilder().setBagId(ByteString.copyFromUtf8("test")).build());
            fail();
        } catch (InvalidAdviceException e) {
        }
    }

}
