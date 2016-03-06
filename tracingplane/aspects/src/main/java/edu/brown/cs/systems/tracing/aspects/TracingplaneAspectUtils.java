package edu.brown.cs.systems.tracing.aspects;

import java.lang.reflect.Field;

public class TracingplaneAspectUtils {
    
    static final Field threadRunnableField;
    static {
        try {
            threadRunnableField = Thread.class.getDeclaredField("target");
            threadRunnableField.setAccessible(true);
        } catch (Throwable t) {
            throw new RuntimeException("Cannot get target field of thread, instrumentation incompatible, aborting", t);
        }
    }
    
    /** Get the wrapped runnable from a thrad */
    static Runnable getRunnable(Thread thread) {
        try {
            return (Runnable) threadRunnableField.get(thread);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    
}
