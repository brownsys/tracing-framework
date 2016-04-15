package edu.brown.cs.systems.dynamicinstrumentation;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.VirtualMachine;
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
            addToolJarToClasspath("tools");
            boolean useJdwp = ConfigFactory.load().getBoolean("dynamic-instrumentation.use_jdwp");
            instance = new DynamicManager(create(useJdwp));
        }
    }
    
    /** From https://github.com/philz/stethoscope/blob/master/src/main/java/org/cloudera/stethoscope/Util.java */
    public static void addToolJarToClasspath(String name) {
        try {
          String javaHome = System.getProperty("java.home");
          String toolsJarURL = "file:" + javaHome + "/../lib/" + name + ".jar";
          new URL(toolsJarURL).getContent();
          // Make addURL public
          Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
          method.setAccessible(true);
          URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
          method.invoke(sysloader, (Object) new URL(toolsJarURL));
          VirtualMachine.class.toString();
        } catch (Exception e) {
          System.err.println("Failed to add " + name + ".jar to classpath: " + e.toString());
          e.printStackTrace();
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
        Agent agent = null;
        try {
            if (preferJdwp) {
                agent = JDWPAgent.get();
            } else {
                agent = JVMAgent.get();
            }
        } catch (Throwable t) {
            System.err.printf("Unable to create %s dynamic instrumentation agent\n", preferJdwp ? "JDWP" : "JVM");
            t.printStackTrace();
        }
        
        // First choice failed, try second choice
        if (agent == null) {
            try {
                if (preferJdwp) {
                    agent = JVMAgent.get();
                } else {
                    agent = JDWPAgent.get();
                }
            } catch (Throwable t) {
                System.err.printf("Unable to create %s dynamic instrumentation agent\n", preferJdwp ? "JVM" : "JDWP");
                t.printStackTrace();
            }
        }
        return agent;
    }
    
    

}
