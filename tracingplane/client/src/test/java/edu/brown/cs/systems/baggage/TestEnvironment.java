package edu.brown.cs.systems.baggage;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Sets;

import junit.framework.TestCase;

public class TestEnvironment extends TestCase {

    @Test
    public void testGetEnvironment() {
        assertNotNull(Baggage.stop().environment());
        assertTrue(Baggage.stop().environment().isEmpty());
        
        BaggageContents.add("Namespace", "Key", "Value");
        Map<String, String> env = Baggage.stop().environment();
        
        assertFalse(env.isEmpty());
        assertTrue(env.containsKey(BaggageUtils.BAGGAGE_ENVIRONMENT_VARIABLE));
        
        BaggageUtils.checkEnvironment(env);
        assertEquals(Sets.newHashSet("Value"), BaggageContents.getStrings("Namespace", "Key"));
    }
    
}
