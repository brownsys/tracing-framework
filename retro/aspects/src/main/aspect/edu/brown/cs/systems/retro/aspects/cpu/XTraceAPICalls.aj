package edu.brown.cs.systems.retro.aspects.cpu;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.retro.resources.CPUTracking;
import edu.brown.cs.systems.retro.resources.Execution;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;

/** Intercepts X-Trace API calls in user code. Don't apply these pointcuts to the X-Trace library itself!
 * 
 * @author jon */
public aspect XTraceAPICalls {

    before(): call(void Baggage.start(..)) || call(DetachedBaggage Baggage.swap(..)) {
        Execution.CPU.finished(thisJoinPointStaticPart);
    }

    after(): call(void Baggage.start(..)) || call(DetachedBaggage Baggage.swap(..)) || call(* Baggage.join(..)) {
        Execution.CPU.starting(thisJoinPointStaticPart);
    }

    /** Whenever the XTraceContext is cleared, log an event to indicate the end of CPU processing bounds */
    before(): call(* Baggage.stop(..)) || call(* Baggage.discard(..)) {
        Execution.CPU.finished(thisJoinPointStaticPart);
    }

    before(): call(void ThrottlingPoint+.throttle()) {
        CPUTracking.finishTracking(); // finish and log current cycles
    }

    after():  call(void ThrottlingPoint+.throttle()) {
        CPUTracking.startTracking(); // start tracking cycles
    }

}
