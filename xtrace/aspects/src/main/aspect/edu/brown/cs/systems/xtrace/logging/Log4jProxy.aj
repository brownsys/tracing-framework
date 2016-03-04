package edu.brown.cs.systems.xtrace.logging;

import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.wrappers.CommonsLogWrapper;
import edu.brown.cs.systems.xtrace.wrappers.Slf4jLoggerWrapper;

/**
 * Intercepts calls to log4j and logs X-Trace events
 * Loggers are typically wrapped using wrappers
 * But if loggers are defined, for example, in an interface, then they can't be overridden with AspectJ
 */
public aspect Log4jProxy {

      private static final XTraceLogger proxy = XTrace.getLogger("Log4jProxy");

      before(Object t, Object o): target(t) && args(o) && (
          call(void org.apache.commons.logging.Log+.debug(Object,..)) ||
          call(void org.apache.commons.logging.Log+.trace(Object,..)) ||
          call(void org.apache.commons.logging.Log+.info(Object,..)) ||
          call(void org.apache.commons.logging.Log+.warn(Object,..)) ||
          call(void org.apache.commons.logging.Log+.error(Object,..)) ||
          call(void org.apache.commons.logging.Log+.fatal(Object,..)) ||
          call(void org.slf4j.Logger+.debug(String,..)) ||
          call(void org.slf4j.Logger+.trace(String,..)) ||
          call(void org.slf4j.Logger+.info(String,..)) ||
          call(void org.slf4j.Logger+.warn(String,..)) ||
          call(void org.slf4j.Logger+.error(String,..))
          ) {
          if (t instanceof CommonsLogWrapper || t instanceof Slf4jLoggerWrapper) {
              return;
          } else if (proxy.valid()) {
              proxy.log(thisJoinPointStaticPart, o.toString());
          }
      }
    
      
      

}
