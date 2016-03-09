package edu.brown.cs.systems.xtrace.wrappers;

import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import edu.brown.cs.systems.xtrace.logging.XTraceLogger;
import edu.brown.cs.systems.xtrace.logging.XTraceLoggingLevel;

public class Slf4jLoggerWrapper implements Logger {

    private static ThreadLocal<JoinPoint.StaticPart> logSources = new ThreadLocal<JoinPoint.StaticPart>();

    public Logger logger;
    public XTraceLogger xtrace;

    public Slf4jLoggerWrapper(Logger logger, XTraceLogger xtrace) {
        this.logger = logger;
        this.xtrace = xtrace;
    }

    public void save(JoinPoint.StaticPart joinPoint) {
        logSources.set(joinPoint);
    }

    public JoinPoint.StaticPart previousEntryPoint() {
        try {
            return logSources.get();
        } finally {
            logSources.remove();
        }
    }

    /** Format an slf4j message */
    public String format(String str, Object... objs) {
        return MessageFormatter.format(str, objs).getMessage();
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled() || xtrace.valid(XTraceLoggingLevel.TRACE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker) || xtrace.valid(XTraceLoggingLevel.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled() || xtrace.valid(XTraceLoggingLevel.DEBUG);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker) || xtrace.valid(XTraceLoggingLevel.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled() || xtrace.valid(XTraceLoggingLevel.INFO);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker) || xtrace.valid(XTraceLoggingLevel.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled() || xtrace.valid(XTraceLoggingLevel.WARN);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker) || xtrace.valid(XTraceLoggingLevel.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled() || xtrace.valid(XTraceLoggingLevel.ERROR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker) || xtrace.valid(XTraceLoggingLevel.ERROR);
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, msg);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        logger.trace(marker, format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, msg, t);
        if (xtrace.valid(XTraceLoggingLevel.TRACE))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, msg);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, msg, t);
        if (xtrace.valid(XTraceLoggingLevel.DEBUG))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, msg);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, msg, t);
        if (xtrace.valid(XTraceLoggingLevel.INFO))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, msg);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, msg, t);
        if (xtrace.valid(XTraceLoggingLevel.WARN))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, msg);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, format, arg);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, format, arg1, arg2);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, format, arguments);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, msg, t);
        if (xtrace.valid(XTraceLoggingLevel.ERROR))
            xtrace.log(previousEntryPoint(), msg);
    }

}
