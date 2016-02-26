package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.DiskResource;

public class DiskResourcePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Disk Resource Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates disk resource consumption";

    public DiskResourcePerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testDiskResource() {
        starting();
        finished();
        both();
        printResults();
    }

    public void starting() {
        for (final DiskResource dr : DiskResource.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, 5, null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".starting", r, numthreads, true, false, false);
            }
        }
    }

    public void finished() {
        for (final DiskResource dr : DiskResource.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.finished(this, 5, null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".finished", r, numthreads, true, false, false);
            }
        }
    }

    public void both() {
        for (final DiskResource dr : DiskResource.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, 5, null);
                    dr.finished(this, 5, null);
                }
            };

            for (int numthreads : threads) {
                doTest("Total " + dr.name(), r, numthreads, true, false, false);
            }
        }
    }
}
