package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.MonitorLock;

public class MonitorLockPerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Monitor Lock Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates monitor lock consumption";

    public MonitorLockPerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testDiskResource() {
        acquiring();
        acquired();
        released();
        all();
        printResults();
    }

    public void acquiring() {
        Runnable r = new Runnable() {
            public void run() {
                MonitorLock.acquiring(this, null);
            }
        };

        for (int numthreads : threads) {
            doTest("MonitorLock.acquiring", r, numthreads, true, false, false);
        }
    }

    public void acquired() {
        Runnable r = new Runnable() {
            public void run() {
                MonitorLock.acquired(this, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("MonitorLock.acquired", r, numthreads, true, false, false);
        }
    }

    public void released() {
        Runnable r = new Runnable() {
            public void run() {
                MonitorLock.released(this, 0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("MonitorLock.released", r, numthreads, true, false, false);
        }
    }

    public void all() {
        Runnable r = new Runnable() {
            public void run() {
                MonitorLock.acquiring(this, null);
                MonitorLock.acquired(this, 0, 0, null);
                MonitorLock.released(this, 0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("Total MonitorLock", r, numthreads, true, false, false);
        }
    }
}
