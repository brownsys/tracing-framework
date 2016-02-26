package edu.brown.cs.systems.retro.throttling.schedulers;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.RetroSchedulers;
import edu.brown.cs.systems.retro.throttling.Scheduler;
import edu.brown.cs.systems.retro.throttling.mclock.MClock;
import edu.brown.cs.systems.retro.throttling.schedulers.MClockScheduler;

public class TestRetroSchedulers extends TestCase {

    @Test
    public void testSchedulerLoading() {
        // Check the default type is mclock-3
        Scheduler defaultScheduler = RetroSchedulers.get("default");
        assertEquals(defaultScheduler.getClass(), MClockScheduler.class);
        MClock clock = ((MClockScheduler) defaultScheduler).mclock;
        assertEquals(clock.getConcurrency(), 3);

        // Check for singleton instance
        Scheduler defaultScheduler2 = RetroSchedulers.get("default");
        assertTrue(defaultScheduler == defaultScheduler2);
        assertTrue(clock == ((MClockScheduler) defaultScheduler2).mclock);

        // Check that a specially-configured instance works
        Scheduler exampleScheduler = RetroSchedulers.get("scheduler-example");
        assertEquals(exampleScheduler.getClass(), MClockScheduler.class);
        MClock clock2 = ((MClockScheduler) exampleScheduler).mclock;
        assertEquals(clock2.getConcurrency(), 5);
        assertFalse(clock2 == clock);
        assertFalse(exampleScheduler == defaultScheduler);

        // Test a scheduler name with lots of weird punctuation
        Scheduler badThrottlingPoint = RetroSchedulers.get("!@#*($(@#@#@(#$!@#$(@#*$@+_lkj;lads-2323j23j08f0adsju4j32@");
        assertEquals(badThrottlingPoint.getClass(), MClockScheduler.class);
    }

}
