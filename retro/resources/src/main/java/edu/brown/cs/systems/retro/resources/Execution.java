package edu.brown.cs.systems.retro.resources;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

/**
 * Logs events related to threads, such as starting and stopping processing in a
 * thread, forking, joining, and sleeping
 * 
 * @author Jonathan Mace
 */
public enum Execution {
    CPU("set", "unset"), Branch("branch", null), Join(null, "join"), Sleep(null, "waited");

    private static XTraceLogger xtrace = XTrace.getLogger("Execution");

    private final String startop;
    private final String endop;

    private Execution(String startop, String endop) {
        this.startop = startop;
        this.endop = endop;
    }

    private static final Long ZERO = 0L;
    private ThreadLocal<Long> opstart = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return ZERO;
        }
    };

    public void starting(JoinPoint.StaticPart jp) {
        if (startop != null && xtrace.valid())
            xtrace.log(jp, startop, "Operation", startop);
        if (CPUTracking.enabled())
            if (this == CPU)
                CPUTracking.startTracking();
            else
                CPUTracking.pauseTracking();
        opstart.set(System.nanoTime());
    }

    public void finished(JoinPoint.StaticPart jp) {
        long latency = System.nanoTime() - opstart.get();
        if (CPUTracking.enabled())
            if (this == CPU)
                CPUTracking.finishTracking();
            else
                CPUTracking.continueTracking();
        if (endop != null && xtrace.valid())
            xtrace.log(jp, endop, "Operation", endop, "Duration", latency);
    }

}