package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.Execution;

public class ExecutionPerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "CPU Resource Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates cpu resource consumption and sleeping";

    public ExecutionPerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testExecutionResource() {
        starting();
        finished();
        both();
        printResults();
    }

    public void starting() {
        for (final Execution dr : Execution.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".starting", r, numthreads, true, false, false);
            }
        }
    }

    public void finished() {
        for (final Execution dr : Execution.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.finished(null);
                }
            };

            for (int numthreads : threads) {
                doTest(dr.name() + ".finished", r, numthreads, true, false, false);
            }
        }
    }

    public void both() {
        for (final Execution dr : Execution.values()) {
            Runnable r = new Runnable() {
                public void run() {
                    dr.starting(null);
                    dr.finished(null);
                }
            };

            for (int numthreads : threads) {
                doTest("Total " + dr.name(), r, numthreads, true, false, false);
            }
        }
    }
}
