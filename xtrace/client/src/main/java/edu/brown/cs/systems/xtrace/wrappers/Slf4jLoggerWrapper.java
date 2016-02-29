package edu.brown.cs.systems.xtrace.wrappers;

import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public class Slf4jLoggerWrapper implements Logger {

    private static ThreadLocal<JoinPoint.StaticPart> logSources = new ThreadLocal<JoinPoint.StaticPart>();

    public final Logger logger;
    public final XTraceLogger xtrace;

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
        return logger.isTraceEnabled() || xtrace.valid();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker) || xtrace.valid();
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        logger.trace(marker, format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled() || xtrace.valid();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker) || xtrace.valid();
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled() || xtrace.valid();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker) || xtrace.valid();
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled() || xtrace.valid();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker) || xtrace.valid();
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled() || xtrace.valid();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker) || xtrace.valid();
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, msg);
        xtrace.log(previousEntryPoint(), msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, format, arg);
        xtrace.log(previousEntryPoint(), format(format, arg));
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, format, arg1, arg2);
        xtrace.log(previousEntryPoint(), format(format, arg1, arg2));
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, format, arguments);
        xtrace.log(previousEntryPoint(), format(format, arguments));
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, msg, t);
        xtrace.log(previousEntryPoint(), msg);
    }

}
