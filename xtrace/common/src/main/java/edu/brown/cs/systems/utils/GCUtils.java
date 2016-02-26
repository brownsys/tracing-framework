package edu.brown.cs.systems.utils;

/**
 * Provides static methods to access various useful information about the
 * current process
 */
public class GCUtils {

    public static long getElapsedGC() {
        return XTraceGCUtils.calculateElapsedGC();
    }

    private static final ThreadLocal<Long> gcSnapshot = new ThreadLocal<Long>();

    public static Long saveGC() {
        long gc = getElapsedGC();
        gcSnapshot.set(gc);
        return gc;
    }

    public static Long getAndResetGC() {
        Long gca = gcSnapshot.get();
        if (gca == null)
            return 0L;
        gcSnapshot.set(null);
        return getElapsedGC() - gca;
    }
}