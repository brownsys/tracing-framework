package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.Network;

public class NetworkXTracePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Network Resource XTraceLogging Timing";
    private static final String description = "Tests cost of the API call that measures, aggregates, and logs an XTrace event";

    public NetworkXTracePerf() {
        super(name, description, 1, 1000);
    }

    @Test
    public void testNetworkXTrace() {
        xtrace();
        printResults();
    }

    public void xtrace() {
        for (final Network dr : Network.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(this, null);
                    dr.finished(this, null);
                }
            };

            for (int numthreads : threads) {
                doTest("XTrace " + dr.name(), r, numthreads, true, true, true);
            }
        }
    }

}
