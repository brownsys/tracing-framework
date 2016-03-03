package edu.brown.cs.systems.dynamicinstrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

/** Does class reloading, modification, and hotswapping using Javassist to rewrite classes. The actual hotswapping
 * implementation is abstract since there are more than one way to do it */
public abstract class Agent {

    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    /** Reload original class definition */
    public void reset(String... classNames) throws UnmodifiableClassException, CannotCompileException {
        reset(Lists.newArrayList(classNames));
    }

    /** Reload original class definition */
    public void reset(Collection<String> classNames) throws UnmodifiableClassException, CannotCompileException {
        Map<String, Collection<DynamicModification>> modifications = Maps.newHashMap();
        for (String className : classNames) {
            modifications.put(className, new HashSet<DynamicModification>());
        }
        install(modifications);
    }

    /** Install one modification affecting one class */
    public void install(String className, DynamicModification modification) throws UnmodifiableClassException, CannotCompileException {
        Map<String, Collection<DynamicModification>> modifications = Maps.newHashMap();
        modifications.put(className, Sets.newHashSet(modification));
        install(modifications);
    }

    /** Install the provided modifications, deriving the affected classes from the modifications */
    public void install(DynamicModification... modifications) throws UnmodifiableClassException, CannotCompileException {
        install(Lists.newArrayList(modifications));
    }

    /** Install the provided modifications, deriving the affected classes from the modifications */
    public void install(Collection<? extends DynamicModification> modifications) throws UnmodifiableClassException, CannotCompileException {
        Multimap<String, DynamicModification> modificationsByClass = HashMultimap.create();
        for (DynamicModification modification : modifications) {
            for (String className : modification.affects()) {
                modificationsByClass.put(className, modification);
            }
        }
        install(modificationsByClass.asMap());
    }

    /** Install the provided modifications.  If a class is unknown, its modification is ignored, but if the provided modifications
     * cannot compile then exceptions will be thrown */
    public void install(Map<String, Collection<DynamicModification>> modifications) throws CannotCompileException, UnmodifiableClassException {
        install(modifications, Lists.<Throwable>newArrayList());
    }

    /** Install the provided modifications.  If a class is unknown, its modification is ignored, but if the provided modifications
     * cannot compile then exceptions will be thrown */
    public void install(Map<String, Collection<DynamicModification>> modifications, Collection<Throwable> problems) throws CannotCompileException, UnmodifiableClassException {
        Installation i = new Installation();
        i.modifyAll(modifications, problems);
        if (!i.reloadMap.isEmpty()) {
            log.info("Reloading {} classes: {}", modifications.size(), modifications.keySet());
            reload(i.reloadMap);
        }
    }

    /** Install the provided modifications asynchronously, swallowing exceptions */
    public void installAsync(final Map<String, Collection<DynamicModification>> modifications) {
        new Thread() {
            public void run() {
                try {
                    install(modifications);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }.start();
    }

    /** Reload the classes specified, replacing with the provided class files */
    protected abstract void reload(Map<String, byte[]> modifiedClassFiles) throws UnmodifiableClassException;

    /** Maintains installation state for reloading some classes */
    private class Installation {

        public final Map<String, byte[]> reloadMap = Maps.newHashMap();

        /** Apply modifications to a class */
        public void modify(String className, Collection<DynamicModification> modifications)
                throws ClassNotFoundException, IOException, CannotCompileException, NotFoundException {
            // Load the original definition if necessary
            if (modifications.isEmpty()) {
                reloadMap.put(className, getClassBytes(className));
                return;
            }

            // If there are no modifications, then just reload the original definition
            if (modifications.isEmpty()) {
                return;
            }

            // Pool containing modified classes. Apparently uses a lot of memory - must destroy afterwards
            ClassPool pool = new ClassPool(true);
            ClassPool.doPruning = true;

            // Apply the modifications, saving the reason for any exceptions
            for (DynamicModification m : modifications) {
                m.apply(pool);
            }

            // Extract the modified class bytes for reloading
            reloadMap.put(className, pool.get(className).toBytecode());
        }

        /** Apply several modifications to multiple classes. If the specified class cannot be found or loaded for some
         * reason, it is ignore. If the class can be loaded but the provided modifications can't compile, an exception
         * will be thrown */
        public void modifyAll(Map<String, Collection<DynamicModification>> modifications, Collection<Throwable> problems) throws CannotCompileException {
            for (String className : modifications.keySet()) {
                try {
                    modify(className, modifications.get(className));
                } catch (NotFoundException e) {
                    // If a class is not found, we continue modifying the other classes
                    log.warn("Unable to modify " + className, e);
                    problems.add(e);
                } catch (ClassNotFoundException e) {
                    // If a class is not found, we continue modifying the other classes
                    log.warn("Unable to modify " + className, e);
                    problems.add(e);
                } catch (IOException e) {
                    // If a class cannot be loaded from file, we continue modifying the other classes
                    log.warn("Unable to modify " + className, e);
                    problems.add(e);
                }
            }
        }
    }

    /** Utility method to get the original class bytes for a class */
    public static byte[] getClassBytes(String className) throws ClassNotFoundException, IOException {
        return IOUtils.toByteArray(getClassFileStream(className));
    }

    /** Utility method to get an input stream to a class file */
    public static InputStream getClassFileStream(String className) throws ClassNotFoundException {
        Class<?> c = ClassUtils.getClass(className);
        ClassLoader loader = c.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
            while (loader != null && loader.getParent() != null) {
                loader = loader.getParent();
            }
        }
        if (loader == null) {
            return null;
        }
        return loader.getResourceAsStream(c.getName().replace(".", "/") + ".class");
    }
}
