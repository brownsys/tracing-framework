package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.BlockingQueue;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.tracing.aspects.Annotations.InstrumentQueues;
import edu.brown.cs.systems.tracing.aspects.Annotations.InstrumentedQueueElement;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Instruments potentially asynchronous executions -- runnables, callables, and threads. Special-case handling for
 * threads since they can themselves be a runnable, or they can wrap a runnable */
public aspect Queues {

    /* ================================================================================================================
     * 
     * QueueElementWithBaggage is added to any class that is used as a queue element, with the InstrumentedQueueElement
     * annotation. */

    public interface QueueElementWithBaggage {
        public DetachedBaggage takeAttachedBaggage();
        public void attachProvidedBaggage(DetachedBaggage baggage);
    }

    public volatile DetachedBaggage QueueElementWithBaggage.attachedQueueElementBaggage;

    public DetachedBaggage QueueElementWithBaggage.takeAttachedBaggage() {
        try {
            return attachedQueueElementBaggage;
        } finally {
            attachedQueueElementBaggage = null;
        }
    }

    public void QueueElementWithBaggage.attachProvidedBaggage(DetachedBaggage baggage) {
        attachedQueueElementBaggage = baggage;
    }

    /* ================================================================================================================
     * 
     * All instrumented queue element annotated get baggage added */

    declare parents: (@InstrumentedQueueElement Object)+ implements QueueElementWithBaggage;
    
    before(QueueElementWithBaggage e): args(e,..) && within(@InstrumentQueues *) && (
            call(* BlockingQueue+.add(..)) || call(* BlockingQueue+.offer(..)) || call(* BlockingQueue+.put(..))
            ) {
        XTraceReport.entering(thisJoinPointStaticPart);
        e.attachProvidedBaggage(Baggage.fork());
        XTraceReport.left(thisJoinPointStaticPart);
    }
    
    before(): within(@InstrumentQueues *) && call(* BlockingQueue+.take(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
        Baggage.discard();
        XTraceReport.left(thisJoinPointStaticPart);
    }
    
    after() returning(QueueElementWithBaggage e): within(@InstrumentQueues *) && call(* BlockingQueue+.take(..)) {
        if (e != null) {
            XTraceReport.entering(thisJoinPointStaticPart);
            Baggage.start(e.takeAttachedBaggage());
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }

}