package edu.brown.cs.systems.xtrace.logging;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;

/** Invoke X-Trace APIs when baggage is set and unset */
public aspect BaggageWrappers {

    static XTraceLogger xtrace = XTrace.getLogger(BaggageWrappers.class);
    
//    after(): call(void Baggage.start()) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Baggage started with Baggage.start()");
//    }
//    
//    after(): (
//            call(void Baggage.start(DetachedBaggage)) ||
//            call(void Baggage.start(ByteString)) ||
//            call(void Baggage.start(byte[])) ||
//            call(void Baggage.start(String))
//            ) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Resumed previously detached baggage");
//    }

//    before(): call(DetachedBaggage Baggage.swap(..)) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Detaching current baggage with Baggage.swap()");
//    }
    
//    after(): call(DetachedBaggage Baggage.swap(..)) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Baggage started with Baggage.swap()");
//    }
    
//    before(): call(void Baggage.discard()) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Discarding baggage with Baggage.discard()");
//    }
    
//    before(): call(DetachedBaggage Baggage.stop()) && if(xtrace.valid()) {
//        xtrace.log(thisJoinPointStaticPart, "Detaching current baggage with Baggage.stop()");
//    }
    
    before(): call(DetachedBaggage Baggage.fork()) && if(xtrace.valid()) {
        xtrace.log(thisJoinPointStaticPart, "Forking current baggage with Baggage.fork()", "Operation", "fork");
    }
    
    after(): (
            call(void Baggage.join(DetachedBaggage)) ||
            call(void Baggage.join(ByteString)) ||
            call(void Baggage.join(byte[])) ||
            call(void Baggage.join(String))
            ) && if(xtrace.valid()) {
        if (XTraceBaggageInterface.getParentEventIds().size() > 1) {
            xtrace.log(thisJoinPointStaticPart, "Merged current baggage with other baggage", "Operation", "join");
        }
    }
    
}
