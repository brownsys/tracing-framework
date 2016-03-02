package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Instruments all runnables to add the following logic: 1. When the runnable object is created, the baggage at that
 * point in time is saved 2. When the runnable is run, the baggage that was saved is now resumed 3. When the run method
 * finishes, the baggage is cleared */
public aspect Runnables {
    private DetachedBaggage InstrumentedRunnable.constructor_baggage;
    private DetachedBaggage InstrumentedRunnable.runend_baggage;

    public void InstrumentedRunnable.rememberConstructorContext() {
        if (constructor_baggage == null)
            constructor_baggage = Baggage.fork();
    }

    public void InstrumentedRunnable.rememberRunendContext() {
        if (runend_baggage == null) {
            runend_baggage = Baggage.stop();
        }
    }

    public void InstrumentedRunnable.rejoinConstructorContext() {
        Baggage.start(constructor_baggage);
        constructor_baggage = null;
    }

    public void InstrumentedRunnable.joinRunendContext() {
        Baggage.join(runend_baggage);
    }
    
    public void InstrumentedRunnable.cancelContexts() {
        constructor_baggage = null;
        runend_baggage = null;
    }

    before(InstrumentedRunnable r): this(r) && execution(InstrumentedRunnable+.new(..)) {
        try {
            XTraceReport.entering(thisJoinPointStaticPart);
            r.rememberConstructorContext();
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

    before(InstrumentedRunnable r): this(r) && execution(void InstrumentedRunnable+.run(..)) {
        try {
            XTraceReport.entering(thisJoinPointStaticPart);
            r.rejoinConstructorContext();
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

    after(InstrumentedRunnable r):  this(r) && execution(void InstrumentedRunnable+.run(..)) {
        try {
            XTraceReport.entering(thisJoinPointStaticPart);
            r.rememberRunendContext();
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

    // Runnable itself can't be instrumented, but any subclasses defined by the application can.
    declare parents: (!@BaggageInheritanceDisabled Runnable)+ implements InstrumentedRunnable;
    
    // If a runnable is a shutdown hook, cancel it
    before(InstrumentedRunnable r): call(* *.addShutdownHook(Runnable+,..)) && args(r,..) {
        r.cancelContexts();
    }

}