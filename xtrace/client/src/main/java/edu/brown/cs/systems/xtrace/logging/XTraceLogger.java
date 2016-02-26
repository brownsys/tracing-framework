package edu.brown.cs.systems.xtrace.logging;

import org.aspectj.lang.JoinPoint;

/**
 * An XTraceLogger simply publishes XTrace reports to the X-Trace server, but
 * can be enabled and disabled similar to how log4j can enable and disable
 * loggers.
 */
public interface XTraceLogger {

    /** Returns true if this logger is currently able to send reports */
    public boolean valid();

    /**
     * Log a message to the X-Trace server.
     * 
     * Example 1: log("My name is {}", "Jon", "Age", 15) will produce an X-Trace
     * report with the message "My name is Jon" and ("Age", 15) k-v pairs
     * 
     * Example 2: log("Hello", "Time", "Monday") will produce an X-Trace report
     * with the message "Hello" and ("Time", "Monday") k-v pairs
     * 
     * Example 3: log("It's {} in {}", "Hot", "Here") will produce an X-Trace
     * report with the message "It's Hot in Here" and no k-v pairs
     * 
     * @param message
     *            A string message to send to the server. Use "{}" for variable
     *            substitution.
     * @param labels
     *            Variables to substitute in the message. After substituting
     *            variables, remaining labels are attached as key-value pairs
     */
    public void log(String message, Object... labels);

    /**
     * Log a message to the X-Trace server
     * 
     * @param message
     *            A string message to send to the server
     * @param tags
     *            Additional tags to attach to the X-Trace Report. Tags show up
     *            on the trace on the X-Trace web ui.
     */
    public void tag(String message, String... tags);

    /**
     * Log a message to the X-Trace server. Methods with joinPoint arguments
     * should typically only be used from within an aspect. Normal usage should
     * be using the non-joinPoint methods
     * 
     * @param joinPoint
     *            an AspectJ static join point, can be null
     * @param message
     *            A string message to send to the server. Use "{}" for variable
     *            substitution.
     * @param labels
     *            Variables to substitute in the message. After substituting
     *            variables, remaining labels are attached as key-value pairs
     */
    void log(JoinPoint.StaticPart joinPoint, String message, Object... labels);

    /**
     * Log a message to the X-Trace server. Methods with joinPoint arguments
     * should typically only be used from within an aspect. Normal usage should
     * be using the non-joinPoint methods
     * 
     * @param joinPoint
     *            an AspectJ static join point, can be null
     * @param message
     *            A string message to send to the server
     * @param tags
     *            Additional tags to attach to the X-Trace Report. Tags show up
     *            on the trace on the X-Trace web ui.
     */
    void tag(JoinPoint.StaticPart joinPoint, String message, String... tags);

}
