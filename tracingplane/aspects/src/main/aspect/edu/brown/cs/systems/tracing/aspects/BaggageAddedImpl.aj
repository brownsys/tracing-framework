package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.baggage.DetachedBaggage;

/** Classes can have the BaggageAdded interface added to them to get these methods. */
public aspect BaggageAddedImpl {
    
    private volatile DetachedBaggage BaggageAdded.baggageAddedSavedBaggage;
    
    public void BaggageAdded.saveBaggage(DetachedBaggage baggage) {
        baggageAddedSavedBaggage = baggage;
    }
    
    public DetachedBaggage BaggageAdded.getSavedBaggage() {
        DetachedBaggage saved = baggageAddedSavedBaggage;
        baggageAddedSavedBaggage = null;
        return saved;
    }
    
    public void BaggageAdded.discardSavedBaggage() {
        baggageAddedSavedBaggage = null;
    }
    
}