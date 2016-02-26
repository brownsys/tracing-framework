package edu.brown.cs.systems.retro.logging;

import org.aspectj.lang.JoinPoint.StaticPart;

import edu.brown.cs.systems.xtrace.XTraceSettings;

/**
 * Saves the join point of application-level code so that wrappers can log the
 * correct entry points
 * 
 * @author a-jomace
 */
public class JoinPointTracking {

    public static interface Tracking {
        public void set(StaticPart jp);

        public void clear();

        public StaticPart get();

        public StaticPart get(StaticPart d);
    }

    private static class TrackingImpl implements Tracking {
        private ThreadLocal<StaticPart> caller = new ThreadLocal<StaticPart>();

        public void set(StaticPart jp) {
            caller.set(jp);
        }

        public void clear() {
            caller.remove();
        }

        public StaticPart get() {
            return caller.get();
        }

        public StaticPart get(StaticPart default_pointcut) {
            if (XTraceSettings.On()) {
                StaticPart thecaller = caller.get();
                return thecaller == null ? default_pointcut : thecaller;
            }
            return null;
        }
    }

    private static class NoTracking implements Tracking {
        public void set(StaticPart jp) {
        }

        public void clear() {
        }

        public StaticPart get() {
            return null;
        }

        public StaticPart get(StaticPart d) {
            return d;
        }
    }

    public static final Tracking Caller;

    static {
        if (XTraceSettings.On())
            Caller = new TrackingImpl();
        else
            Caller = new NoTracking();
    }

}
