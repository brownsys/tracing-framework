package edu.brown.cs.systems.xtrace.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.brown.cs.systems.xtrace.wrappers.CommonsLogWrapper;
import edu.brown.cs.systems.xtrace.wrappers.Slf4jLoggerWrapper;
import junit.framework.TestCase;

public class TestLoggerWrapper extends TestCase {

    @Test
    public void testCommonsLoggerIsWrapped() {
        Log log = LogFactory.getLog(TestLoggerWrapper.class);
        assertTrue(CommonsLogWrapper.class.equals(log.getClass()));
        
        assertNull(((CommonsLogWrapper) log).previousEntryPoint());
        log.debug("Hello");
        assertNotNull(((CommonsLogWrapper) log).previousEntryPoint());
        assertNull(((CommonsLogWrapper) log).previousEntryPoint());
        
    }

    @Test
    public void testCommonsLoggerIsWrapped2() {
        Log log = LogFactory.getLog("TestLoggerWrapper");
        assertTrue(CommonsLogWrapper.class.equals(log.getClass()));
        
        assertNull(((CommonsLogWrapper) log).previousEntryPoint());
        log.debug("Hello");
        assertNotNull(((CommonsLogWrapper) log).previousEntryPoint());
        assertNull(((CommonsLogWrapper) log).previousEntryPoint());
    }

    @Test
    public void testLog4jLoggerIsWrapped() {
        Logger logger = LoggerFactory.getLogger(TestLoggerWrapper.class);
        assertTrue(Slf4jLoggerWrapper.class.equals(logger.getClass()));
        
        assertNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
        logger.debug("Hello");
        assertNotNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
        assertNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
    }

    @Test
    public void testLog4jLoggerIsWrapped2() {
        Logger logger = LoggerFactory.getLogger("TestLoggerWrapper");
        assertTrue(Slf4jLoggerWrapper.class.equals(logger.getClass()));
        
        assertNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
        logger.debug("Hello");
        assertNotNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
        assertNull(((Slf4jLoggerWrapper) logger).previousEntryPoint());
    }
    
}
