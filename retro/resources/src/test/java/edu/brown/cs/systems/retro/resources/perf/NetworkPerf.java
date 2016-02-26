package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.Network;

public class NetworkPerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Network Resource Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates network consumption";

    public NetworkPerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testNetwork() {
        starting();
        finished();
        both();
        printResults();
    }

    public void starting() {
        for (final Network dr : Network.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".starting", r, numthreads, true, false, false);
            }
        }
    }

    public void finished() {
        for (final Network dr : Network.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.finished(this, null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".finished", r, numthreads, true, false, false);
            }
        }
    }

    public void both() {
        for (final Network dr : Network.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, null);
                    dr.finished(this, null);
                }
            };

            for (int numthreads : threads) {
                doTest("Total " + dr.name(), r, numthreads, true, false, false);
            }
        }
    }
}
