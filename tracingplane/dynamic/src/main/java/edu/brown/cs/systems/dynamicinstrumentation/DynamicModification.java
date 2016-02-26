package edu.brown.cs.systems.dynamicinstrumentation;

import java.util.Collection;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

/** A dynamic change (eg a rewrite of a method) */
public interface DynamicModification {
    
    /** Class names this modification affects */
    public Collection<String> affects();
    
    /** Apply the modification.  May throw exceptions  */
    public void apply(ClassPool pool) throws NotFoundException, CannotCompileException;

}
