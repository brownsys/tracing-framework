package edu.brown.cs.systems.retro.throttling.throttlingpoints;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.LocalThrottlingPoints;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.ThrottlingQueue;
import edu.brown.cs.systems.retro.throttling.throttlingpoints.BatchedThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.throttlingpoints.SimpleThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.throttlingqueues.ThrottlingDelayQueue;
import edu.brown.cs.systems.retro.throttling.throttlingqueues.ThrottlingLockingQueue;

public class TestLocalThrottlingPoints extends TestCase {

    @Test
    public void testThrottlingPointTypeLoading() {
        // Check that the default type is SimpleThrottlingPoint
        ThrottlingPoint defaultThrottlingPoint = LocalThrottlingPoints.getThrottlingPoint("default");
        assertEquals(defaultThrottlingPoint.getClass(), SimpleThrottlingPoint.class);

        // Check for singleton instance
        ThrottlingPoint defaultThrottlingPoint2 = LocalThrottlingPoints.getThrottlingPoint("default");
        assertTrue(defaultThrottlingPoint == defaultThrottlingPoint2);

        // Check that a specially-configured instance works
        ThrottlingPoint exampleThrottlingPoint = LocalThrottlingPoints.getThrottlingPoint("point-example");
        assertEquals(exampleThrottlingPoint.getClass(), BatchedThrottlingPoint.class);
        ThrottlingPoint wrapped = ((BatchedThrottlingPoint) exampleThrottlingPoint).wrapped;
        assertEquals(((BatchedThrottlingPoint) exampleThrottlingPoint).batchsize, 5);
        assertEquals(wrapped.getClass(), SimpleThrottlingPoint.class);

        // Check for singleton instance with unchanged members
        ThrottlingPoint exampleThrottlingPoint2 = LocalThrottlingPoints.getThrottlingPoint("point-example");
        assertTrue(exampleThrottlingPoint == exampleThrottlingPoint2);
        assertEquals(((BatchedThrottlingPoint) exampleThrottlingPoint).batchsize, 5);
        assertEquals(((BatchedThrottlingPoint) exampleThrottlingPoint).wrapped, wrapped);

        // Test a "bad" throttling point name
        ThrottlingPoint badThrottlingPoint = LocalThrottlingPoints.getThrottlingPoint("ohno$bad");
        assertEquals(badThrottlingPoint.getClass(), SimpleThrottlingPoint.class);
    }

    @Test
    public void testThrottlingQueueTypeLoading() {
        // Check that the default type is SimpleThrottlingPoint
        ThrottlingQueue<Object> defaultThrottlingPoint = LocalThrottlingPoints.getThrottlingQueue("default");
        assertEquals(defaultThrottlingPoint.getClass(), ThrottlingLockingQueue.class);

        // Check for singleton instance
        try {
            ThrottlingQueue<Object> defaultThrottlingPoint2 = LocalThrottlingPoints.getThrottlingQueue("default");
            assertFalse("Only one throttling point queue allowed per named instance", true);
        } catch (RuntimeException e) {
        }

        // Check that a specially-configured instance works
        ThrottlingQueue<Object> exampleThrottlingPoint = LocalThrottlingPoints.getThrottlingQueue("queue-example");
        assertEquals(exampleThrottlingPoint.getClass(), ThrottlingDelayQueue.class);

        // Test a "bad" throttling queue
        ThrottlingQueue<Object> badThrottlingQueue = LocalThrottlingPoints.getThrottlingQueue("ohno$bad");
        assertEquals(badThrottlingQueue.getClass(), ThrottlingLockingQueue.class);

    }

}
