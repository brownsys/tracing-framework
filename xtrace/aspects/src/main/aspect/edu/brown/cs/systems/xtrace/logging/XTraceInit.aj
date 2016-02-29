package edu.brown.cs.systems.xtrace.logging;

import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.XTraceSettings;

/**
 * Adds an X-Trace start event in any main method
 */
public aspect XTraceInit {

    before(): execution(public static void main(String[])) {
        if (XTraceSettings.traceMainMethods()) {
            XTrace.startTask(true);
            XTrace.setLoggingLevel(XTraceSettings.mainMethodLoggingLevel());
            XTrace.getLogger(XTraceInit.class).tag(thisJoinPointStaticPart, "Process main method begin", Utils.getMainClass().getSimpleName(), "main");
        }
    }

}
