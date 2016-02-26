package edu.brown.cs.systems.pivottracing.agent;

import org.junit.Test;

import junit.framework.TestCase;

public class TestPivotTracing extends TestCase {

    @Test
    public void testPivotTracingStaticAPI() {
        assertNull(PivotTracing.agent);        
        PivotTracing.initialize();
        
        PTAgent agent = PivotTracing.agent;
        assertNotNull(agent);

        PivotTracing.initialize();
        assertEquals(agent, PivotTracing.agent);
        PivotTracing.initialize();
        assertEquals(agent, PivotTracing.agent);
        PivotTracing.initialize();
        assertEquals(agent, PivotTracing.agent);

        assertEquals(agent, PivotTracing.agent());
        assertEquals(agent, PivotTracing.agent());
        assertEquals(agent, PivotTracing.agent());
        assertEquals(agent, PivotTracing.agent());
    }
}
