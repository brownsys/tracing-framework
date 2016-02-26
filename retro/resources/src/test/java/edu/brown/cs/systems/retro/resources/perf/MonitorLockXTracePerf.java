package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.MonitorLock;

public class MonitorLockXTracePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Monitor Lock XTraceLogging Timing";
    private static final String description = "Tests cost of the API call that measures, aggregates, and logs an XTrace event";

    public MonitorLockXTracePerf() {
        super(name, description, 1, 1000);
    }

    @Test
    public void testDiskResourceXTrace() {
        xtrace();
        printResults();
    }

    public void xtrace() {
        Runnable r = new Runnable() {
            public void run() {
                MonitorLock.acquiring(this, null);
                MonitorLock.acquired(this, 0, 0, null);
                MonitorLock.released(this, 0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("XTrace MonitorLock", r, numthreads, true, true, true);
        }
    }

}
