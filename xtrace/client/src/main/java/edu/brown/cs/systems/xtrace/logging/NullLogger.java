package edu.brown.cs.systems.xtrace.logging;

import org.aspectj.lang.JoinPoint.StaticPart;

/**
 * Logger that does nothing
 */
public class NullLogger implements XTraceLogger {
    public boolean valid() {
        return false;
    }
    public boolean valid(XTraceLoggingLevel level) {
        return false;
    }

    public void log(String message, Object... labels) {
    }

    public void tag(String message, String... tags) {
    }

    public void log(StaticPart joinPoint, String message, Object... labels) {
    }

    public void tag(StaticPart joinPoint, String message, String... tags) {
    }
}
