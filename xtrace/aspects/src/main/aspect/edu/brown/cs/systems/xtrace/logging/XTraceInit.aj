package edu.brown.cs.systems.xtrace.logging;

import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;
import edu.brown.cs.systems.xtrace.XTraceSettings;

/**
 * Adds an X-Trace start event in any main method
 */
public aspect XTraceInit {
    
    public static final XTraceLogger xtrace = XTrace.getLogger(XTraceInit.class);

    before(): execution(public static void main(String[])) {
        if (!XTraceBaggageInterface.hasTaskID() && XTraceSettings.traceMainMethods()) {
            XTrace.startTask(true);
            XTrace.setLoggingLevel(XTraceSettings.mainMethodLoggingLevel());
        }
        xtrace.tag(thisJoinPointStaticPart, "Process main method begin", Utils.getMainClass().getSimpleName(), "main");
    }
    
    after(): call(* Thread+.setName(..)) && if(xtrace.valid()) {
        xtrace.log(thisJoinPointStaticPart, "Thread.setName()", "ThreadName", Thread.currentThread().getName());
    }

}
