package edu.brown.cs.systems.xtrace.reporting;

public interface XTraceReporter {

    /**
     * Send the XTraceReport to the X-Trace server
     */
    public void send(XTraceReport report);

}
