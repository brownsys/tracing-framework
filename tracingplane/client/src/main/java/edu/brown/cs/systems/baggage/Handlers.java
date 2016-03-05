package edu.brown.cs.systems.baggage;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * API for registering callback handlers to be invoked when baggage gets split and joined.
 * Most users should not need to use this API.
 */
public class Handlers {
    
    /** Custom logic invoked whenever an execution context branches or joins. */
    public interface BaggageHandler {
        
        /** Invoked just before a baggage is about to be split */
        public void preSplit(BaggageImpl current);
        
        /** Invoked just after a baggage is split */
        public void postSplit(BaggageImpl left, BaggageImpl right);
        
        /** Invoked just before two baggages are about to be joined */
        public void preJoin(BaggageImpl left, BaggageImpl right);
        
        /** Invoked just after two baggages were joined */
        public void postJoin(BaggageImpl current);
        
        /** Invoked just before a baggage is about to be serialized */
        public void preSerialize(BaggageImpl baggage);
        
        /** Invoked just after a baggage was deserialized */
        public void postDeserialize(BaggageImpl baggage);

    }


    static final List<BaggageHandler> handlers = Lists.newCopyOnWriteArrayList();

    /** Add a baggage handler that is invoked any time baggage operations are performed on valid baggages
     * 
     * @param handler */
    public static void registerBaggageHandler(BaggageHandler handler) {
        handlers.add(handler);
    }

    /** Remove an existing baggage handler that was previously registered. Method does nothing if the handler isn't
     * currently registered.
     * 
     * @param handler */
    public static void unregisterBaggageHandler(BaggageHandler handler) {
        handlers.remove(handler);
    }

    static void preSplit(BaggageImpl current) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.preSplit(current);
            } catch (Throwable t) {
                // TODO: log this
            }
        }
    }

    static void postSplit(BaggageImpl current, BaggageImpl copy) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.postSplit(current, copy);
            } catch (Throwable t) {}
        }
    }

    static void preMerge(BaggageImpl current, BaggageImpl other) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.preJoin(current, other);
            } catch (Throwable t) {}
        }
    }

    static void postMerge(BaggageImpl current) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.postJoin(current);
            } catch (Throwable t) {}
        }
    }
    
    static void preSerialize(BaggageImpl baggage) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.preSerialize(baggage);
            } catch (Throwable t) {}
        }
    }
    
    static void postDeserialize(BaggageImpl baggage) {
        for (BaggageHandler handler : handlers) {
            try {
                handler.postDeserialize(baggage);
            } catch (Throwable t) {}
        }
    }

}
