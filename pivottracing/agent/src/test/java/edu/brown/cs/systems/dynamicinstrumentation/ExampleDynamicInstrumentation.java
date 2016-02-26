package edu.brown.cs.systems.dynamicinstrumentation;

import java.lang.instrument.UnmodifiableClassException;
import java.util.Collection;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicInstrumentation;
import edu.brown.cs.systems.dynamicinstrumentation.Agent;
import edu.brown.cs.systems.dynamicinstrumentation.DynamicModification;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

public class ExampleDynamicInstrumentation {
    
    public static final String className = "edu.brown.cs.systems.pivottracing.dynamicinstrumentation.ExampleDynamicInstrumentation";
    
    public static void sayHello() {
        System.out.println("Hello world");
    }
    
    public static void sayHelloAgain() {
        System.out.println("Hello George");
    }
    
    public static void methodBegin(String experimentId) {
        System.out.println("Begin speedup method for expid " + experimentId);
    }
    
    public static void methodEnd(String experimentId) {
        System.out.println("End speedup method for expid " + experimentId);
    }
    
    public static class ExperimentModification implements DynamicModification {

        public static int expIdSeed = 0;
        public String expId = "Experiment-"+expIdSeed++;
        public final String className, methodName;
        
        public ExperimentModification(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
        
        @Override
        public Collection<String> affects() {
            return Lists.newArrayList(className);
        }

        @Override
        public void apply(ClassPool pool) throws NotFoundException, CannotCompileException {
            pool.get(className).getDeclaredMethod(methodName).insertBefore("edu.brown.cs.systems.pivottracing.dynamicinstrumentation.ExampleDynamicInstrumentation.methodBegin(\""+expId+"\");");
            pool.get(className).getDeclaredMethod(methodName).insertAfter("edu.brown.cs.systems.pivottracing.dynamicinstrumentation.ExampleDynamicInstrumentation.methodEnd(\""+expId+"\");");
        }
        
    }
    
    
    public static void main(String[] args) throws UnmodifiableClassException, CannotCompileException {
        
        sayHello();
        DynamicModification modifySayHello = new ExperimentModification(ExampleDynamicInstrumentation.class.getName(), "sayHello");
        Agent dynamic = DynamicInstrumentation.create(false);
        dynamic.install(modifySayHello);
        
        sayHello();
        
    }

}
