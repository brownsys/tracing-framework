package edu.brown.cs.systems.dynamicinstrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * Dynamic instrumentation by adding an agent lib to ourselves.
 * Approach based on Philip Zeyliger's Stethoscope: https://github.com/philz/stethoscope
 */
public class JVMAgent extends Agent {

    private static final Logger log = LoggerFactory.getLogger(JVMAgent.class);
    
    private static JVMAgent instance = null;
    private static CountDownLatch waitForInstance = new CountDownLatch(1);
    
    public final Instrumentation instrumentation;
    
    public JVMAgent(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }
    
    /** Try to start an agent. Wait for it to attach, or throw an exception */
    public static JVMAgent get() throws AgentLoadException, AgentInitializationException, IOException, AttachNotSupportedException {
        // There may be only one
        if (instance != null) {
            return instance;
        }
        
        // Not guaranteed to be correct, but seems to work.
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];    
        VirtualMachine jvm = VirtualMachine.attach(pid);

        // Gets the current path
        String path = JVMAgent.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        jvm.loadAgent(decodedPath, null);
        
        // Wait for the instance, time out relatively quickly
        try {
            if (!waitForInstance.await(2000, TimeUnit.MILLISECONDS)) {
                System.err.println("Unable to create JVM agent: timed out waiting for JVM agent to attach");
                return null;
            }
        } catch (InterruptedException e) {
            System.err.println("Unable to create JVM agent: interrupted waiting for JVM agent to attach");
            return null;
        }
        
        if (instance == null) {
            System.err.println("Unable to create JVM agent");
        } else {
            System.out.println("Successfully attached JVM agent");
        }
        
        // Return the instance, which might be null
        synchronized(JVMAgent.class) {
            return instance;            
        }
    }

    /** When the agent gets loaded, this method is called, giving us an instrumentation instance. Voila! */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        synchronized(JVMAgent.class) {
            if (instance == null) {
                instance = new JVMAgent(inst);
                waitForInstance.countDown();
            }
        }
    }

    @Override
    public void reload(Map<String, byte[]> modifiedClassFiles) throws UnmodifiableClassException {
        new Transformer(modifiedClassFiles).transform();
    }
    
    /** Transforms class files as they are reloaded */
    private class Transformer implements ClassFileTransformer {
        
        public final Map<Class<?>, byte[]> classdata = Maps.newHashMap();
        
        public Transformer(Map<String, byte[]> modifiedClassFiles) {
            for (String className : modifiedClassFiles.keySet()) {
                try {
                    Class<?> actualClass = ClassUtils.getClass(className);
                    classdata.put(actualClass, modifiedClassFiles.get(className));
                } catch (ClassNotFoundException e) {
                    // If the class can't be found, just ignore it
                    log.warn("Unable to reload class " + className, e);
                }
            }
        }
        
        /** Do the transformation */
        public void transform() throws UnmodifiableClassException {
            instrumentation.addTransformer(this, true);
            try {
                Class<?>[] classList = classdata.keySet().toArray(new Class<?>[classdata.size()]);
                instrumentation.retransformClasses(classList);
            } finally {
                instrumentation.removeTransformer(this);
            }
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] currentVersion)
                throws IllegalClassFormatException {
            // If we have modified bytes for the class, return those; otherwise, return the current version
            if (classBeingRedefined != null && classdata.containsKey(classBeingRedefined)) {
                log.debug("Replacing {} {}", className, classBeingRedefined);
                return classdata.get(classBeingRedefined);
            } else {
                log.debug("Ignoring {} {}", className, classBeingRedefined);
                return currentVersion;
            }
        }
        
    }

}
