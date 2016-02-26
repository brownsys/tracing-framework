package edu.brown.cs.systems.xtrace.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.wrappers.CommonsLogWrapper;
import edu.brown.cs.systems.xtrace.wrappers.Slf4jLoggerWrapper;

/** Intercepts calls to log4j and logs X-Trace events */
public aspect LoggerWrappers {

    Log around(Class<?> cls): call(Log LogFactory+.getLog(Class+)) && args(cls) {
        return new CommonsLogWrapper(proceed(cls), XTrace.getLogger(cls));
    }

    Log around(String name): call(Log LogFactory+.getLog(String+)) && args(name) {
        return new CommonsLogWrapper(proceed(name), XTrace.getLogger(name));
    }

    Logger around(Class<?> cls): call(Logger LoggerFactory+.getLogger(Class+)) && args(cls) {
        return new Slf4jLoggerWrapper(proceed(cls), XTrace.getLogger(cls));
    }

    Logger around(String name): call(Logger LoggerFactory+.getLogger(String+)) && args(name) {
        return new Slf4jLoggerWrapper(proceed(name), XTrace.getLogger(name));
    }

    before(CommonsLogWrapper log): target(log) && (
      call(void org.apache.commons.logging.Log+.debug(..)) ||
      call(void org.apache.commons.logging.Log+.trace(..)) ||
      call(void org.apache.commons.logging.Log+.info(..)) ||
      call(void org.apache.commons.logging.Log+.warn(..)) ||
      call(void org.apache.commons.logging.Log+.error(..)) ||
      call(void org.apache.commons.logging.Log+.fatal(..))
      ) {
        log.save(thisJoinPointStaticPart);
    }

    before(Slf4jLoggerWrapper logger): target(logger) && (
      call(void org.slf4j.Logger+.debug(..)) ||
      call(void org.slf4j.Logger+.trace(..)) ||
      call(void org.slf4j.Logger+.info(..)) ||
      call(void org.slf4j.Logger+.warn(..)) ||
      call(void org.slf4j.Logger+.error(..))
      ) {
        logger.save(thisJoinPointStaticPart);
    }

}
