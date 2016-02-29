package edu.brown.cs.systems.xtrace.logging;

import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;

public enum XTraceLoggingLevel {
    
    FATAL, ERROR, WARN, TRACE, INFO, DEBUG;
    
    public boolean valid() {
        return XTraceBaggageInterface.getLoggingLevel().ordinal() >= this.ordinal();
    }

}
