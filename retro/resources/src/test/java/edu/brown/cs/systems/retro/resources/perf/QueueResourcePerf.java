package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.QueueResource;

public class QueueResourcePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "Queue Resource Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates queue resource consumption";

    public QueueResourcePerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testDiskResource() {
        enqueue();
        starting();
        finished();
        all();
        printResults();
    }

    private final QueueResource q = new QueueResource("QueueResourcePerf", 1000);

    public void enqueue() {
        Runnable r = new Runnable() {
            public void run() {
                q.enqueue();
            }
        };

        for (int numthreads : threads) {
            doTest("QueueResource.enqueue", r, numthreads, true, false, false);
        }
    }

    public void starting() {
        Runnable r = new Runnable() {
            public void run() {
                q.starting(0, 0);
            }
        };

        for (int numthreads : threads) {
            doTest("QueueResource.starting", r, numthreads, true, false, false);
        }
    }

    public void finished() {
        Runnable r = new Runnable() {
            public void run() {
                q.finished(0, 0, 0);
            }
        };

        for (int numthreads : threads) {
            doTest("QueueResource.finished", r, numthreads, true, false, false);
        }
    }

    public void all() {
        Runnable r = new Runnable() {
            public void run() {
                q.enqueue();
                q.starting(0, 0);
                q.finished(0, 0, 0);
            }
        };

        for (int numthreads : threads) {
            doTest("Total QueueResource", r, numthreads, true, false, false);
        }
    }
}
