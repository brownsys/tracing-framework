package edu.brown.cs.systems.baggage;

import org.junit.Test;

import edu.brown.cs.systems.baggage.BaggageTestMessages.OptionalBaggageBytesMessage;
import junit.framework.TestCase;

public class TestOptionalBytes extends TestCase {
    
    @Test
    public void testOptional() {
        OptionalBaggageBytesMessage nobaggage = OptionalBaggageBytesMessage.newBuilder().build();
        
        Baggage.start(nobaggage.getBaggage());
        
        assertNull(Baggage.current.get());
    }

}
