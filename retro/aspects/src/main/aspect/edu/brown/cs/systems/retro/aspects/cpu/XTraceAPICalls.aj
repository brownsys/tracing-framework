package edu.brown.cs.systems.retro.aspects.cpu;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.retro.resources.CPUTracking;
import edu.brown.cs.systems.retro.resources.Execution;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;

/** Intercepts X-Trace API calls in user code. Don't apply these pointcuts to the X-Trace library itself!
 * 
 * @author jon */
public aspect XTraceAPICalls {

    void around(): call(void Baggage.start(..)) {
        Execution.CPU.finished(thisJoinPointStaticPart);
        try {
            proceed();
        } finally {
            Execution.CPU.starting(thisJoinPointStaticPart);
        }
    }

    /** Whenever the XTraceContext is cleared, log an event to indicate the end of CPU processing bounds */
    before(): call(* Baggage.stop(..)) || call(* Baggage.discard(..)) {
        Execution.CPU.finished(thisJoinPointStaticPart);
    }

    /** Whenever an XTraceContext is joined, it might be the case that this is equivalent to setThreadContext */
    void around(): call(void Baggage.join(..)) {
        Execution.CPU.finished(thisJoinPointStaticPart);
        try {
            proceed();
        } finally {
            Execution.CPU.starting(thisJoinPointStaticPart);
        }
    }

    before(): call(void ThrottlingPoint+.throttle()) {
        CPUTracking.finishTracking(); // finish and log current cycles
    }

    after():  call(void ThrottlingPoint+.throttle()) {
        CPUTracking.startTracking(); // start tracking cycles
    }

}
