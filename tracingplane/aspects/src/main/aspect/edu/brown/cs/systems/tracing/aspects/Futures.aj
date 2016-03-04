package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Future;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/**
 * Returns a InstrumentedThread whenever a new thread is created. Allows
 * rejoining of XTraceMetadata.
 * 
 * Captures most thread creation, but obviously not from external libraries
 */
public aspect Futures {

    after(Future f): target(f) && call(* Future+.get(..)) {
        try {
            XTraceReport.entering(thisJoinPointStaticPart);
            Baggage.join(WrappedFuture.getSavedBaggage(f));
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

}
