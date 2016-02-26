package edu.brown.cs.systems.dynamicinstrumentation;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicModification;
import edu.brown.cs.systems.dynamicinstrumentation.JVMAgent;
import edu.brown.cs.systems.pivottracing.agent.PrivilegedAgent;
import edu.brown.cs.systems.pivottracing.agent.PrivilegedProxy;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import junit.framework.TestCase;

/**
 * More dynamic modification tests but this time configured to run with the privileged proxy on the boot classpath.
 * This will enable us to call from privileged code back down to unprivileged code.
 * Tests duplicated here should now succeed
 */
public class TestJVMAgentPrivileged extends TestCase {
    
    static int i = 0;
    static Object[] args = new Object[0];
    
    public static void DoSomething(int i, Object[] args) {
        TestJVMAgentPrivileged.i = i;
        TestJVMAgentPrivileged.args = args;
    }
    
    public static String trycatch(String original) {
        return String.format("try { %s } catch (java.lang.Throwable t) {}", original);
    }
    

    // Directly tries to call DoSomething
    static class TestUnproxiedPrivilegedModification implements DynamicModification {
        public final String affects = String.class.getName();
        public Collection<String> affects() {
            return Lists.newArrayList(affects);
        }
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            CtMethod m = pool.getCtClass(affects).getDeclaredMethod("trim");
            m.insertAfter(String.format("%s.DoSomething(7, new Object[0]);", TestJVMAgentPrivileged.class.getName()));
        }
    }

    
    /**
     * Should fail to call unprivileged code from privileged
     */
    public void testUnproxied() throws Exception {
        assertEquals("7", String.valueOf(7));
        assertEquals(0, i);
        
        // Install bad code -- privileged class that tries to access unprivileged class :(
        JVMAgent dynamic = JVMAgent.get();
        DynamicModification modification = new TestUnproxiedPrivilegedModification();
        dynamic.install(modification);
        
        try {
            new String().trim();
            fail("Shouldn't be able to do this");
        } catch (NoClassDefFoundError e) {
            // Good
        }        
        assertEquals(0, i);
        
        dynamic.reset(modification.affects());

        assertEquals("7", String.valueOf(7));
        assertEquals(0, i);
    }
    
    private static class TestPrivilegedAgent implements PrivilegedAgent {
        public void Advise(int i, Object[] args) {
            DoSomething(i, args);
        }
    }
    

    // Directly tries to call DoSomething
    static class TestProxiedPrivilegedModification implements DynamicModification {
        public final String affects = Integer.class.getName();
        public Collection<String> affects() {
            return Lists.newArrayList(affects);
        }
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            CtMethod m = pool.getCtClass(affects).getDeclaredMethod("toString", new CtClass[] { pool.getCtClass("int") });
            m.insertAfter(String.format("%s.Advise(7, new Object[0]);", PrivilegedProxy.class.getName()));
        }
    }
    
    /**
     * Now that the privileged proxy is on the boot classpath, this should succeed
     */
    @Test
    public void testProxied() throws Exception {
        assertEquals("7", String.valueOf(7));
        assertEquals(0, i);
        
        // Register privileged proxy
        PrivilegedProxy.Register(new TestPrivilegedAgent());
        
        // Install good code that routes via privileged proxy
        JVMAgent dynamic = JVMAgent.get();
        DynamicModification modification = new TestProxiedPrivilegedModification();
        dynamic.install(modification);

        assertEquals("7", String.valueOf(7));
        assertEquals(7, i);
        
        dynamic.reset(modification.affects());

        assertEquals("7", String.valueOf(7));
        assertEquals(7, i);
        i = 0;
    }

}
