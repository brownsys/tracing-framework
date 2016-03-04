package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.baggage.DetachedBaggage;

public interface BaggageAdded {
    public void saveBaggage(DetachedBaggage baggage);
    public DetachedBaggage getSavedBaggage();
    public void discardSavedBaggage();
}
