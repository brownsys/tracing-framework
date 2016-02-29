package edu.brown.cs.systems.retro.resources;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.aspectj.lang.JoinPoint;

import com.google.common.collect.Maps;

import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.LockAggregator;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public class MonitorLock {

    private static XTraceLogger xtrace = XTrace.getLogger("MonitorLock");
    private static boolean AGGREGATION_ENABLED = LockAggregator.valid();

    private static Class<?> lockClass(Object lock) {
        if (lock instanceof Class)
            return (Class<?>) lock;
        return lock.getClass();
    }

    private static String lockType(Object lock) {
        if (lock instanceof Lock)
            return "Explicit Lock";
        else if (lock instanceof Class)
            return "Class Monitor Lock";
        else
            return "Instance Monitor Lock";
    }

    private static String lockName(Object lock, JoinPoint.StaticPart jp) {
        return lockClass(lock).getName() + System.identityHashCode(lock);
    }

    private static ThreadLocal<Map<Object, Long>> acquire_xmds = new ThreadLocal<Map<Object, Long>>() {
        @Override
        public Map<Object, Long> initialValue() {
            return Maps.newHashMap();
        }
    };

    public static void acquiring(Object lock, JoinPoint.StaticPart jp) {
        if (AGGREGATION_ENABLED)
            LocalResources.getLockAggregator(lockName(lock, jp)).requesting(Retro.getTenant());
        CPUTracking.pauseTracking();
    }

    public static void acquired(Object lock, long request, long acquire, JoinPoint.StaticPart jp) {
        CPUTracking.continueTracking();
        if (xtrace.valid()) {
            xtrace.log(jp, "lockacquire", "Operation", "lockacquire", "LockID", System.identityHashCode(lock), "Lock", lockClass(lock).getName(), "LockType", lockType(lock),
                    "LockRequest", request, "LockAcquire", acquire);
            if (XTraceBaggageInterface.hasParents()) {
                acquire_xmds.get().put(lock, XTraceBaggageInterface.getParentEventIds().iterator().next());
            }
        }
    }

    public static void released(Object lock, long request, long acquire, long release, JoinPoint.StaticPart jp) {
        if (AGGREGATION_ENABLED)
            LocalResources.getLockAggregator(lockName(lock, jp)).released(Retro.getTenant(), request, acquire, release);
        if (xtrace.valid()) {
            if (XTraceBaggageInterface.hasParents()) {
                Long acquirexmd = acquire_xmds.get().remove(lock);
                xtrace.log(jp, "lockrelease", "Operation", "lockrelease","LockID", System.identityHashCode(lock), "Lock", lockClass(lock).getName(), "LockType", lockType(lock),
                        "LockRequest", request, "LockAcquire", acquire, "LockRelease", release, "LockAcquireEdge", acquirexmd == null ? "none" : acquirexmd);
            } else {
                xtrace.log(jp, "lockrelease", "Operation", "lockrelease","LockID", System.identityHashCode(lock), "Lock", lockClass(lock).getName(), "LockType", lockType(lock),
                        "LockRequest", request, "LockAcquire", acquire, "LockRelease", release);
            }
        }
    }
}
