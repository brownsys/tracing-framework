package edu.brown.cs.systems.xtrace.logging;

import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;

public enum XTraceLoggingLevel {
    
    FATAL, ERROR, WARN, INFO, TRACE, DEBUG;
    
    public boolean valid() {
        return XTraceBaggageInterface.getLoggingLevel().ordinal() >= this.ordinal();
    }

}
