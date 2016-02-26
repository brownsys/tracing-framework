package edu.brown.cs.systems.retro.throttling;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;

/**
 * A throttling point for pausing a thread mid-execution
 * 
 * @author a-jomace
 *
 */
public interface ThrottlingPoint {

    /**
     * Sleeps the current thread for some amount of time, until it is acceptable
     * to proceed
     */
    public void throttle();

    /** Update the throttling point */
    public void update(ThrottlingPointSpecification spec);

    /** Clear the throttling point */
    public void clearRates();

}
