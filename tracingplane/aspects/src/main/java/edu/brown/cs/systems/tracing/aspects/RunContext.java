package edu.brown.cs.systems.tracing.aspects;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Stores the baggage for an execution. Baggage will be forked once during constructor and saved. Then during
 * execution, the baggage will restored. After execution, the baggage will be saved again. In addition, any baggage
 * present before execution will be restored after execution completes. Furthermore, this is implemented to make sure
 * multiple calls (for example, due to subclasses and wrapping) have correct behavior. */
public class RunContext {
    DetachedBaggage baggage = null;
    int reentry_count = 0;

    /** Called when a runnable is constructed, to fork the current baggage and save it. The forked baggage will be
     * resumed when the execution begins */
    public void ForkCurrentBaggageIfNecessary(JoinPoint.StaticPart jp) {
        if (baggage == null) {
            XTraceReport.entering(jp);
            baggage = Baggage.fork();
            XTraceReport.left(jp);
        }
    }

    /** Called after a runnable has completed, and its ending context is being rejoined (eg, from a call to
     * Thread.join()) */
    public void JoinSavedBaggageIfPossible(JoinPoint.StaticPart jp) {
        if (baggage != null) {
            XTraceReport.entering(jp);
            Baggage.join(baggage);
            XTraceReport.left(jp);
            baggage = null;
        }
    }

    /** Called to indicate that a method such as run() or call() has begun. Due to subclassing and wrapping, this might
     * be called multiple times. Only the first invocation manipulates the baggage */
    public void BeginExecution(JoinPoint.StaticPart jp) {
        // Only the first invocation manipulates the baggage
        if (reentry_count++ == 0) {
            XTraceReport.entering(jp);
            baggage = Baggage.swap(baggage);
            XTraceReport.left(jp);
        }
    }

    /** Called to indicate that a method such as run() or call() has completed. Due to subclassing and wrapping, this
     * might be called multiple times. Only the final invocation saves the baggage */
    public void EndExecution(JoinPoint.StaticPart jp) {
        if (--reentry_count == 0) {
            XTraceReport.entering(jp);
            baggage = Baggage.swap(baggage);
            XTraceReport.left(jp);
        }
    }

    /** Called if this runnable should just ditch its baggage */
    public void Discard() {
        baggage = null;
    }
}