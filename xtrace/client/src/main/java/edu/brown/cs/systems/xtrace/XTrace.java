package edu.brown.cs.systems.xtrace;

import java.util.Random;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.logging.NullLogger;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;
import edu.brown.cs.systems.xtrace.logging.XTraceLoggerImpl;
import edu.brown.cs.systems.xtrace.reporting.NullReporter;
import edu.brown.cs.systems.xtrace.reporting.PubSubReporter;
import edu.brown.cs.systems.xtrace.reporting.XTraceReporter;

/**
 * The front door to X-Trace v4.
 * 
 * X-Trace v4 is a logging interface built on top of ETrace, a generic metadata
 * propagation API.
 * 
 * Logging is provided as instance methods on instances returned by
 * XTrace.getLogger
 */
public class XTrace {

    /** For generating random task and event IDs */
    private static final Random r = new Random(31 * (17 * Utils.getHost().hashCode() + Utils.getProcessID()) * System.currentTimeMillis());
    public static synchronized long randomId() {
        return r.nextLong();
    }
    
    /** Deterministic task ID used in discovery mode */
    static long discoveryId() {
        return Lists.<Object>newArrayList(Utils.getHost(), Utils.getProcessID(), Thread.currentThread().getId()).hashCode();
    }

    /** Publishes reports to the X-Trace server */
    static XTraceReporter DEFAULT_REPORTER;

    static XTraceLogger DEFAULT_LOGGER; // Default logger

    static XTraceLogger NULL_LOGGER = new NullLogger(); // Logger that does
                                                        // nothing

    static boolean XTRACE_ENABLED = false; // Global config value - is XTrace
                                           // actually enabled?

    /**
     * Lazy initialization of reporter and loggers
     */
    private static void init() {
        if (DEFAULT_REPORTER == null) {
            synchronized (XTrace.class) {
                if (DEFAULT_REPORTER == null) {
                    XTRACE_ENABLED = XTraceSettings.On();
                    if (XTRACE_ENABLED) {
                        // Create and start the PubSub reporter
                        PubSubReporter pubsub = new PubSubReporter();
                        pubsub.start();
                        DEFAULT_REPORTER = pubsub;
                        DEFAULT_LOGGER = new XTraceLoggerImpl("", DEFAULT_REPORTER);
                    } else {
                        DEFAULT_REPORTER = new NullReporter();
                        DEFAULT_LOGGER = NULL_LOGGER;
                    }

                }
            }
        }

    }

    /**
     * Gets or creates the default X-Trace reporter
     */
    public static XTraceReporter getDefaultReporter() {
        init();
        return DEFAULT_REPORTER;
    }

    /**
     * Returns the default logger
     * 
     * @return
     */
    public static XTraceLogger getDefaultLogger() {
        if (XTraceSettings.On()) {
            init();
            return DEFAULT_LOGGER;
        } else {
            return NULL_LOGGER;
        }
    }

    /**
     * Get the logger for the specified logging agent. A side effect of this
     * method might be that the reporter thread is started. If logging is
     * disabled for this agent, a dummy logger is returned.
     * 
     * @param agent
     *            The agent to get a logger for
     * @return A logger for the specified agent
     */
    public static XTraceLogger getLogger(String agent) {
        if (!XTraceSettings.On()) {
            return NULL_LOGGER;
        } else if (agent == null) {
            return getDefaultLogger();
        } else if (XTraceSettings.Enabled(agent)) {
            init();
            return new XTraceLoggerImpl(agent, DEFAULT_REPORTER);
        } else {
            return NULL_LOGGER;
        }
    }

    /**
     * Shorthand for getLogger(agent.getName())
     * 
     * @param agent
     *            The name of the agent will be used as the name of the logger
     *            to retrieve
     * @return an xtrace event logger that can be used to log events
     */
    public static XTraceLogger getLogger(Class<?> agent) {
        if (!XTraceSettings.On()) {
            return NULL_LOGGER;
        } else if (agent == null) {
            return getDefaultLogger();
        } else {
            return getLogger(agent.getName());
        }
    }

    /**
     * Start propagating a task ID in this thread if we aren't already
     * propagating a task ID. If we aren't currently propagating a task ID, a
     * new one is randomly generated
     * 
     * @param trackCausality
     *            should we also track causality for this task?
     */
    public static void startTask(boolean trackCausality) {
        XTraceBaggageInterface.setTaskID(randomId());
        if (trackCausality) {
            XTraceBaggageInterface.setParentEventId(0);
        }
    }

    /**
     * Start propagating the specified taskID in this thread. If X-Trace is
     * already propagating a taskid in this thread, then this method call does
     * nothing.
     * 
     * @param taskid
     *            the taskID to start propagating in this thread
     * @param trackCausality
     *            should we also track causality for this task?
     */
    public static void setTask(long taskid, long... parentEventIds) {
        XTraceBaggageInterface.setTaskID(taskid);
        XTraceBaggageInterface.setParentEventIds(parentEventIds);
    }
}
