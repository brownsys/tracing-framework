package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.DiskResource;

public class DiskResourceXTracePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Disk Resource XTraceLogging Timing";
    private static final String description = "Tests cost of the API call that measures, aggregates, and logs an XTrace event";

    public DiskResourceXTracePerf() {
        super(name, description, 1, 1000);
    }

    @Test
    public void testDiskResourceXTrace() {
        xtrace();
        printResults();
    }

    public void xtrace() {
        for (final DiskResource dr : DiskResource.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, 5, null);
                    dr.finished(this, 5, null);
                }
            };

            for (int numthreads : threads) {
                doTest("XTrace " + dr.name(), r, numthreads, true, true, true);
            }
        }
    }

}
