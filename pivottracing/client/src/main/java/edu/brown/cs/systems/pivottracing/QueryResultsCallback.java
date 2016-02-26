package edu.brown.cs.systems.pivottracing;

/** Periodically receives query results from the Pivot Tracing agents.
 * Each time query results are received from an agent, the user defined callback will be invoked */
public interface QueryResultsCallback {
    
    /** Called whenever results are received over pubsub */
    public void onResultsReceived(ResultsProtos.QueryResults results);
    

}
