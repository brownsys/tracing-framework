package edu.brown.cs.systems.xtrace.logging;

import org.aspectj.lang.JoinPoint.StaticPart;
import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import junit.framework.TestCase;

public class TestBaggageWrappers extends TestCase {
    
    private static class TestLogger implements XTraceLogger {
        
        int logCount, tagCount, logJoinCount, tagJoinCount; 

        public boolean valid() {
            return true;
        }

        public boolean valid(XTraceLoggingLevel level) {
            return true;
        }

        public void log(String message, Object... labels) {
            logCount++;
        }

        public void tag(String message, String... tags) {
            tagCount++;
        }

        public void log(StaticPart joinPoint, String message, Object... labels) {
            logJoinCount++;
        }

        public void tag(StaticPart joinPoint, String message, String... tags) {
            tagJoinCount++;
        }
        
        public void check(int log, int tag, int logJoin, int tagJoin) {
            TestCase.assertEquals(log, logCount);
            TestCase.assertEquals(tag, tagCount);
            TestCase.assertEquals(logJoin, logJoinCount);
            TestCase.assertEquals(tagJoin, tagJoinCount);
        }
    }

    @Test
    public void testBaggageWrappers() {
        TestLogger logger = new TestLogger();
        BaggageWrappers.xtrace = logger;
        
        logger.check(0,0,0,0);
        logger.log("hello");
        logger.check(0,0,1,0);
        
        Baggage.start();
        logger.check(0,0,2,0);
        
        Baggage.start((byte[]) null);
        logger.check(0,0,3,0);
        
        Baggage.start((ByteString) null);
        logger.check(0,0,4,0);
        
        Baggage.start((DetachedBaggage) null);
        logger.check(0,0,5,0);
        
        Baggage.start("");
        logger.check(0,0,6,0);
        
        Baggage.stop();
        logger.check(0,0,7,0);
        
        Baggage.fork();
        logger.check(0,0,8,0);
        
        Baggage.join((byte[]) null);
        logger.check(0,0,9,0);
        
        Baggage.join((ByteString) null);
        logger.check(0,0,10,0);
        
        Baggage.join((DetachedBaggage) null);
        logger.check(0,0,11,0);
        
        Baggage.join("");
        logger.check(0,0,12,0);
    }
    
}
