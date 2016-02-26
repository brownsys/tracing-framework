package edu.brown.cs.systems.dynamicinstrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigFactory;

/** Static API to create dynamic instrumentation agent and modification manager */
public class DynamicInstrumentation {

    private static final Logger log = LoggerFactory.getLogger(DynamicInstrumentation.class);

    private static DynamicManager instance = null;
    
    public static DynamicManager get() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }
    
    private static synchronized void initialize() {
        if (instance == null) {
            boolean useJdwp = ConfigFactory.load().getBoolean("dynamic-instrumentation.use_jdwp");
            instance = new DynamicManager(create(useJdwp));
        }
    }
    
    /** Creates and returns a dynamic instrumentation agent. If useJdwp is set to true, this method first tries to
     * attach to the process using JDWP. If useJdwp is set to false, this method first tries to attach a java
     * instrumentation agent to the process. If the preferred method fails, the other method is attempted. If both
     * methods fail, returns null.
     * 
     * @param preferJdwp Prefer JDWP based instrumentation to java instrumentation agent
     * @return a dynamic instrumentation agent, or null if one could not be created. */
    static Agent create(boolean preferJdwp) {
        // Try first choice
        try {
            if (preferJdwp) {
                return JDWPAgent.get();
            } else {
                return JVMAgent.get();
            }
        } catch (Throwable t) {
            log.warn("Unable to create {} dynamic instrumentation agent", preferJdwp ? "JDWP" : "JVM");
        }
        // First choice failed, try second choice
        try {
            if (preferJdwp) {
                return JVMAgent.get();
            } else {
                return JDWPAgent.get();
            }
        } catch (Throwable t) {
            log.warn("Unable to create {} dynamic instrumentation agent", preferJdwp ? "JVM" : "JDWP");
        }
        return null;
    }
    
    

}
