package edu.brown.cs.systems.dynamicinstrumentation;

import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicModification;
import edu.brown.cs.systems.dynamicinstrumentation.JVMAgent;
import edu.brown.cs.systems.dynamicinstrumentation.TestJVMAgentPrivileged.TestUnproxiedPrivilegedModification;
import edu.brown.cs.systems.pivottracing.agent.PrivilegedAgent;
import edu.brown.cs.systems.pivottracing.agent.PrivilegedProxy;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import junit.framework.TestCase;

public class TestJVMAgent extends TestCase {
    
    class ClassToModify {
        public int value;
        public void setValue(int x) {
            this.value = x;
        }
    }
    
    class TestModification implements DynamicModification {

        public final String affects = ClassToModify.class.getName();
        
        @Override
        public Collection<String> affects() {
            return Lists.newArrayList(affects);
        }

        @Override
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            CtMethod m = pool.getCtClass(affects).getDeclaredMethod("setValue");
            m.insertAfter("this.value += 75;");
        }
        
    }

    /**
     * Simple method rewrite
     */
    @Test
    public void testMethodRewriteModification() throws Exception {
        ClassToModify inst = new ClassToModify();
        
        for (int i = 0; i < 100; i++) {
            inst.setValue(i);
            assertEquals(i, inst.value);
        }
        
        JVMAgent dynamic = JVMAgent.get();
        DynamicModification modification = new TestModification();
        dynamic.install(modification);
        
        for (int i = 0; i < 100; i++) {
            inst.setValue(i);
            assertEquals(i+75, inst.value);
        }        
        
        dynamic.reset(modification.affects());
        
        for (int i = 0; i < 100; i++) {
            inst.setValue(i);
            assertEquals(i, inst.value);
        }
    }
    

    
    class TestPrivilegedModification implements DynamicModification {

        public final String affects = Integer.class.getName();
        
        @Override
        public Collection<String> affects() {
            return Lists.newArrayList(affects);
        }

        @Override
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            CtMethod m = pool.getCtClass(affects).getDeclaredMethod("toString", new CtClass[] { pool.getCtClass("int") });
            m.insertAfter("$_ = \"10\";");
        }
        
    }
    
    /**
     * Should be able to rewrite privileged methods, just not access unprivileged code
     */
    @Test
    public void testPrivilegedMethodRewriteModification() throws Exception {
        assertEquals("7", String.valueOf(7));

        JVMAgent dynamic = JVMAgent.get();
        DynamicModification modification = new TestPrivilegedModification();
        dynamic.install(modification);

        assertEquals("10", String.valueOf(7));
        
        dynamic.reset(modification.affects());
        
        assertEquals("7", String.valueOf(7));
    }

    /**
     * Should fail to call unprivileged code from privileged
     */
    @Test
    public void testUnproxied() throws Exception {
        assertEquals("7", String.valueOf(7));
        assertEquals(0, TestJVMAgentPrivileged.i);
        
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
        assertEquals(0, TestJVMAgentPrivileged.i);
        
        dynamic.reset(modification.affects());

        assertEquals("7", String.valueOf(7));
        assertEquals(0, TestJVMAgentPrivileged.i);
    }
    
    private static class TestPrivilegedAgent implements PrivilegedAgent {
        public void Advise(int i, Object[] args) {
            TestJVMAgentPrivileged.DoSomething(i, args);
        }
    }
    

    // Directly tries to call DoSomething
    class TestProxiedPrivilegedModification implements DynamicModification {
        public final String affects = String.class.getName();
        public Collection<String> affects() {
            return Lists.newArrayList(affects);
        }
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            CtMethod m = pool.getCtClass(affects).getDeclaredMethod("trim");
            m.insertAfter(String.format("%s.Advise(7, new Object[0]);", PrivilegedProxy.class.getName()));
        }
    }
    
    /**
     * Even though it tries to go through the privileged proxy, this should fail because it isn't added to the boot classpath
     * @throws Exception
     */
    @Test
    public void testProxied() throws Exception {
        assertEquals("7", String.valueOf(7));
        assertEquals(0, TestJVMAgentPrivileged.i);
        
        // Register privileged proxy
        PrivilegedProxy.Register(new TestPrivilegedAgent());
        
        // Install good code that routes via privileged proxy
        JVMAgent dynamic = JVMAgent.get();
        DynamicModification modification = new TestProxiedPrivilegedModification();
        dynamic.install(modification);

        try {
            new String().trim();
            fail("Shouldn't be able to do this");
        } catch (NoClassDefFoundError e) {
            // Good
        }        
        assertEquals(0, TestJVMAgentPrivileged.i);
        
        dynamic.reset(modification.affects());

        assertEquals("7", String.valueOf(7));
        assertEquals(0, TestJVMAgentPrivileged.i);
        TestJVMAgentPrivileged.i = 0;
    }
    
    @Test
    public void testBadModification() throws ClassNotFoundException, AgentLoadException, AgentInitializationException, IOException, AttachNotSupportedException, UnmodifiableClassException, CannotCompileException {
        // Rewrite method
        DynamicModification badModification = new DynamicModification() {
            public Collection<String> affects() {
                return Lists.newArrayList(TestJVMAgent.class.getName());
            }
            public void apply(ClassPool arg0) throws NotFoundException, CannotCompileException {
                arg0.getCtClass("edu.brown.cs.systems.pivottracing.dynamicinstrumentation.NotaRealClass");
            }
        };
        JVMAgent dynamic = JVMAgent.get();
        // Modification should just be ignored since it throws a notfoundexception
        dynamic.install(badModification);
    }
    
    @Test
    public void testBadModification2() throws ClassNotFoundException, AgentLoadException, AgentInitializationException, IOException, AttachNotSupportedException, UnmodifiableClassException {
        // Rewrite method
        DynamicModification badModification = new DynamicModification() {
            public Collection<String> affects() {
                return Lists.newArrayList(TestJVMAgent.class.getName());
            }
            public void apply(ClassPool arg0) throws NotFoundException, CannotCompileException {
                CtClass cls = arg0.getCtClass(TestJVMAgent.class.getName());
                cls.getMethods()[0].insertBefore("definitely not code...");
            }
        };
        JVMAgent dynamic = JVMAgent.get();
        // Modification should just be ignored since it throws a notfoundexception
        try {
            dynamic.install(badModification);
            fail();
        } catch (CannotCompileException e) {
        }
    }

}
