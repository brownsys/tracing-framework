package edu.brown.cs.systems.dynamicinstrumentation;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import javassist.util.HotSwapper;

/** Does class reloading, modification, and hotswapping using Javassist's HotSwapper, which uses JDWP to reload classes.
 * In order to use JDWP, Java must run with the following command line options */
public class JDWPAgent extends Agent {

    private static final Logger log = LoggerFactory.getLogger(JDWPAgent.class);

    private static JDWPAgent instance = null;
    
    private final HotSwapper hotswap;
    
    /** Try to get an instrumentation instance using JDWP */
    public static synchronized JDWPAgent get() throws IOException, IllegalConnectorArgumentsException {
        // There can be only one
        if (instance != null) {
            return instance;
        }
        
        // Get the JDWP port
        Properties props = sun.misc.VMSupport.getAgentProperties();
        String jdwp_address = props.getProperty("sun.jdwp.listenerAddress");
        
        // If no JDWP address, then the debug argument isn't included
        if (jdwp_address == null) {
            System.err.println("Could not acquire JDWP address to attach dynamic instrumentation. "
                    + "Ensure JVM runs with argument -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0");
            return null;
        }
        
        // Extract the port and return the instance;
        System.out.println("JDWPAgent started with address " + jdwp_address);
        String jdwp_port = jdwp_address.substring(jdwp_address.lastIndexOf(':') + 1);
        instance = new JDWPAgent(jdwp_port);
        return instance;
    }

    public JDWPAgent(String port) throws IOException, IllegalConnectorArgumentsException {
        hotswap = new HotSwapper(port);
    }

    @Override
    protected void reload(Map<String, byte[]> modifiedClassFiles) {
        hotswap.reload(modifiedClassFiles);
    }   

}
