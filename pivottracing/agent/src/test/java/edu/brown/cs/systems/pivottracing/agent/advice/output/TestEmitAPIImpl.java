package edu.brown.cs.systems.pivottracing.agent.advice.output;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPIImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI.Emit;
import junit.framework.TestCase;

public class TestEmitAPIImpl extends TestCase {

    @Test
    public void testInvalidEmit() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).build();

        EmitAPIImpl impl = new EmitAPIImpl(1000, "topic", true);
        try {
            Emit created = impl.create(emitspec1);
            fail();
        } catch (InvalidAdviceException e) {

        }
    }

    @Test
    public void testCreateRemove() throws InvalidAdviceException {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitAPIImpl impl = new EmitAPIImpl(1000, "topic", true);
        
        assertEquals(0, impl.emits.size());
        Emit created = impl.create(emitspec1);
        assertEquals(1, impl.emits.size());
        assertTrue(impl.emits.contains(created));
        
        impl.destroy(created);
        assertEquals(0, impl.emits.size());
        assertFalse(impl.emits.contains(created));
    }

}
