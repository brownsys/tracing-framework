package edu.brown.cs.systems.retro.throttling;

import java.util.concurrent.BlockingQueue;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;

/**
 * A throttling point for asynchronous request-like objects
 * 
 * It is assumed that
 * 
 * @author a-jomace
 *
 */
public interface ThrottlingQueue<T> extends BlockingQueue<T> {

    /** Update the throttling point */
    public void update(ThrottlingPointSpecification spec);

    /** Clear the throttling point */
    public void clearRates();

}
