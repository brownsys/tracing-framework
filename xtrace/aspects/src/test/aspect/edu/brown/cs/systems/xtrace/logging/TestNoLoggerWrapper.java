package edu.brown.cs.systems.xtrace.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.brown.cs.systems.xtrace.wrappers.CommonsLogWrapper;
import edu.brown.cs.systems.xtrace.wrappers.Slf4jLoggerWrapper;
import junit.framework.TestCase;

public class TestNoLoggerWrapper extends TestCase {

    @Test
    public void testCommonsLoggerIsWrapped() {
        Log log = LogFactory.getLog(TestLoggerWrapper.class);
        assertFalse(CommonsLogWrapper.class.equals(log.getClass()));
    }

    @Test
    public void testCommonsLoggerIsWrapped2() {
        Log log = LogFactory.getLog("TestLoggerWrapper");
        assertFalse(CommonsLogWrapper.class.equals(log.getClass()));
    }

    @Test
    public void testLog4jLoggerIsWrapped() {
        Logger logger = LoggerFactory.getLogger(TestLoggerWrapper.class);
        assertFalse(Slf4jLoggerWrapper.class.equals(logger.getClass()));
    }

    @Test
    public void testLog4jLoggerIsWrapped2() {
        Logger logger = LoggerFactory.getLogger("TestLoggerWrapper");
        assertFalse(Slf4jLoggerWrapper.class.equals(logger.getClass()));
    }
}
