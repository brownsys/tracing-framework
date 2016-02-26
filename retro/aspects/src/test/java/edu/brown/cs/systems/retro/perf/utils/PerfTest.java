package edu.brown.cs.systems.retro.perf.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.xtrace.XTrace;
import junit.framework.TestCase;

public class PerfTest extends TestCase {

    // Collects perf test results
    protected final StringBuilder output = new StringBuilder();

    // If needed by test cases
    protected byte[] bytes = PerfUtils.getBytes(5);
    protected DetachedBaggage context = PerfUtils.getContext(5);

    // Overridable
    protected int runs = 3;
    protected int iterations_per_run = 100000;

    @Test
    public void testNothingStupidJava() {
    }

    public PerfTest() {
        super();
    }

    public PerfTest(String name, String desc) {
        super();
        output.append(name);
        output.append("\n");
        output.append(desc);
        output.append("\n");
    }

    public PerfTest(String name, String desc, int runs, int iterations_per_run) {
        this(name, desc);
        this.runs = runs;
        this.iterations_per_run = iterations_per_run;
    }

    protected void time(Runnable r, Timer t) {
        t.start();
        for (int i = 0; i < iterations_per_run; i++) {
            r.run();
        }
        t.stop(iterations_per_run);
    }

    protected void doTest(String testName, final Runnable r, int numthreads, final boolean setTenant, final boolean startTask, final boolean trackCausality) {
        // Runs the testing threads
        ExecutorService executor = Executors.newCachedThreadPool();

        // Control the multithreaded execution
        final CountDownLatch begin = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(numthreads);

        // Timer to aggregate results
        final Timer t = new Timer();

        // Runs the test for one thread
        Runnable tester = new Runnable() {
            public void run() {
                try {
                    if (setTenant)
                        Retro.setTenant(1);
                    if (startTask)
                        XTrace.startTask(trackCausality);

                    // Wait for the goahead
                    begin.await();

                    // Warmup
                    for (int i = 0; i < iterations_per_run / 10; i++) {
                        r.run();
                    }

                    // Run
                    for (int i = 0; i < runs; i++) {
                        time(r, t);
                    }

                    // Finish
                    end.countDown();
                } catch (InterruptedException e) {
                    begin.countDown();
                    end.countDown();
                } finally {
                    Baggage.stop();
                }
            }
        };

        // Submit the threads
        for (int i = 0; i < numthreads; i++) {
            executor.submit(tester);
        }

        // Signal the start and await the end
        begin.countDown();
        try {
            end.await();
        } catch (InterruptedException e) {
            System.out.println(testName + " interrupted, aborting");
            executor.shutdownNow();
            return;
        }

        // Print
        recordResults(testName, numthreads, t);
    }

    protected void doTest(String testName, final Runnable r, int numthreads) {
        doTest(testName, r, numthreads, false, false, false);
    }

    private void recordResults(String testName, int numthreads, Timer t) {
        output.append(String.format("- %30s\tt%d\t%.0f ns\t%.0f cycles\n", testName, numthreads, t.hrt(), t.cpu()));
    }

    public void printResults() {
        if (output.length() > 0) {
            output.append("\n");
            System.out.println(output);
        }
    }

}
