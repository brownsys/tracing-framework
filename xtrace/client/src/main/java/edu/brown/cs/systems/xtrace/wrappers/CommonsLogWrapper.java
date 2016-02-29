package edu.brown.cs.systems.xtrace.wrappers;

import org.apache.commons.logging.Log;
import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.xtrace.logging.XTraceLogger;
import edu.brown.cs.systems.xtrace.logging.XTraceLoggingLevel;

/** Wraps an apache commons logging Log */
public class CommonsLogWrapper implements org.apache.commons.logging.Log {
    
    private static ThreadLocal<JoinPoint.StaticPart> logSources = new ThreadLocal<JoinPoint.StaticPart>();
    
    public Log log;
    public XTraceLogger xtrace;
    
    public CommonsLogWrapper(Log log, XTraceLogger xtrace) {
        this.log = log;
        this.xtrace = xtrace;
    }
    
    public void save(JoinPoint.StaticPart joinPoint){
        logSources.set(joinPoint);
    }
    
    public JoinPoint.StaticPart previousEntryPoint() {
        try {
            return logSources.get();
        } finally {
            logSources.remove();
        }
    }

    @Override
    public void debug(Object arg0) {
        log.debug(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void debug(Object arg0, Throwable arg1) {
        log.debug(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

    @Override
    public void error(Object arg0) {
        log.error(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void error(Object arg0, Throwable arg1) {
        log.error(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

    @Override
    public void fatal(Object arg0) {
        log.fatal(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void fatal(Object arg0, Throwable arg1) {
        log.fatal(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

    @Override
    public void info(Object arg0) {
        log.info(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void info(Object arg0, Throwable arg1) {
        log.info(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled() || xtrace.valid(XTraceLoggingLevel.DEBUG);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled() || xtrace.valid(XTraceLoggingLevel.ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
        return log.isFatalEnabled() || xtrace.valid(XTraceLoggingLevel.FATAL);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled() || xtrace.valid(XTraceLoggingLevel.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled() || xtrace.valid(XTraceLoggingLevel.TRACE);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled() || xtrace.valid(XTraceLoggingLevel.WARN);
    }

    @Override
    public void trace(Object arg0) {
        log.trace(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void trace(Object arg0, Throwable arg1) {
        log.trace(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

    @Override
    public void warn(Object arg0) {
        log.warn(arg0);
        xtrace.log(previousEntryPoint(), "{}", arg0);
    }

    @Override
    public void warn(Object arg0, Throwable arg1) {
        log.warn(arg0, arg1);
        xtrace.log(previousEntryPoint(), "{} {}", arg0, arg1);
    }

}
