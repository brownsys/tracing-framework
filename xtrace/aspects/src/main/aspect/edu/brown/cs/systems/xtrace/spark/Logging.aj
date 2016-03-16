package edu.brown.cs.systems.xtrace.spark;

import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Wrap calls to Logging.log* in spark */
public aspect Logging {

    static XTraceLogger xtrace = XTrace.getLogger(Logging.class);
    


    before(): (
            call(void org.apache.spark.Logging+.logInfo(..)) ||
            call(void org.apache.spark.Logging+.logDebug(..)) ||
            call(void org.apache.spark.Logging+.logTrace(..)) ||
            call(void org.apache.spark.Logging+.logWarning(..)) ||
            call(void org.apache.spark.Logging+.logError(..))
      ) {
        XTraceReport.entering(thisJoinPointStaticPart);
    }

    after(): (
            call(void org.apache.spark.Logging+.logInfo(..)) ||
            call(void org.apache.spark.Logging+.logDebug(..)) ||
            call(void org.apache.spark.Logging+.logTrace(..)) ||
            call(void org.apache.spark.Logging+.logWarning(..)) ||
            call(void org.apache.spark.Logging+.logError(..))
      ) {
        XTraceReport.left(thisJoinPointStaticPart);
    }

}
