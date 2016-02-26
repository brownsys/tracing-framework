package edu.brown.cs.systems.retro.throttling.schedulers;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.throttling.mclock.SlotManager;

public class TestSlotManager extends TestCase {

    @Test
    public void testSlotManager() {

        for (int numslots = 1; numslots < 100; numslots++) {
            SlotManager m = new SlotManager(numslots);
            for (int i = 0; i < numslots; i++) {
                assertTrue("request didn't get a slot " + i + " max=" + numslots, m.addRequest());
            }
            assertFalse("request got a slot " + numslots, m.addRequest());
            assertTrue("slot reentry failed", m.addSlot());
            assertFalse("slot got nonexistant request", m.addSlot());
            assertTrue("request didnt get a slot", m.addRequest());
            assertFalse("request got a slot " + numslots, m.addRequest());
            for (int i = 1; i < numslots; i++) {
                m.setMaxSlots(numslots - i);
                assertFalse("slot reentry succeeded", m.addSlot());
                assertTrue("slot reentry failed", m.addSlot());
                assertFalse("slot got nonexistant request", m.addSlot());
                assertTrue("request didnt get a slot", m.addRequest());
                assertFalse("request got a slot " + numslots, m.addRequest());
            }
        }
    }

}
