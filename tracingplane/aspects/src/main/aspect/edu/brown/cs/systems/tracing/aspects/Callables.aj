package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.Callable;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Instruments all callables */
public aspect Callables {

    declare parents: (!@BaggageInheritanceDisabled Callable)+ implements BaggageAdded;

    before(BaggageAdded r): this(r) && execution(Callable+.new(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
        try {
            r.saveBaggage(Baggage.fork());
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

    Object around(BaggageAdded r): this(r) && execution(* Callable+.call(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
        DetachedBaggage previousBaggage = Baggage.swap(r.getSavedBaggage());
        XTraceReport.left(thisJoinPointStaticPart);

        try {
            return proceed(r);
        } finally {
            XTraceReport.entering(thisJoinPointStaticPart);
            r.saveBaggage(Baggage.swap(previousBaggage));
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

}