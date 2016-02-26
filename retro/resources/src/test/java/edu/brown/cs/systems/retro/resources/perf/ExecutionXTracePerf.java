package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.Execution;

public class ExecutionXTracePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "CPU Resource XTraceLogging Timing";
    private static final String description = "Tests cost of the API call that measures, aggregates, and logs an XTrace event";

    public ExecutionXTracePerf() {
        super(name, description, 1, 1000);
    }

    @Test
    public void testExecutionXTrace() {
        xtrace();
        printResults();
    }

    public void xtrace() {
        for (final Execution dr : Execution.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(null);
                    dr.finished(null);
                }
            };

            for (int numthreads : threads) {
                doTest("XTrace " + dr.name(), r, numthreads, true, true, true);
            }
        }
    }

}
