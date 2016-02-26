package edu.brown.cs.systems.dynamicinstrumentation;

import java.io.IOException;

import org.junit.Test;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import edu.brown.cs.systems.dynamicinstrumentation.JDWPAgent;
import junit.framework.TestCase;

public class TestJDWPAgent extends TestCase {
    
    @Test
    public void testNoAgentWithoutDebugmode() throws IOException, IllegalConnectorArgumentsException {
        JDWPAgent dynamic = JDWPAgent.get();
        assertNull(dynamic);
    }

}
