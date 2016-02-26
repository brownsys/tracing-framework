package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.QueueResource;

public class QueueResourceXTracePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Queue Resource XTraceLogging Timing";
    private static final String description = "Tests cost of the API call that measures, aggregates, and logs an XTrace event";

    public QueueResourceXTracePerf() {
        super(name, description, 1, 1000);
    }

    @Test
    public void testDiskResourceXTrace() {
        xtrace();
        printResults();
    }

    private final QueueResource q = new QueueResource("QueueResourcePerf", 1000);

    public void xtrace() {
        Runnable r = new Runnable() {
            public void run() {
                q.enqueue();
                q.starting(0, 0);
                q.finished(0, 0, 0);
            }
        };

        for (int numthreads : threads) {
            doTest("Total QueueResource", r, numthreads, true, true, true);
        }
    }

}
