package edu.brown.cs.systems.retro.perf.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import junit.framework.TestCase;

public class AspectJPerfTest extends TestCase {

    public static interface PerfRunnable {
        public void run(AspectJPerfAPI api);
    }

    // Collects perf test results
    protected final StringBuilder output = new StringBuilder();

    // If needed by test cases
    protected byte[] bytes = PerfUtils.getBytes(5);
    protected DetachedBaggage context = PerfUtils.getContext(5);

    // Overridable
    protected int numthreads = 10;
    protected int runs = 5;
    protected int iterations_per_run = 100000;

    protected final AspectJPerfAPI uninstrumented = new Uninstrumented();
    protected final AspectJPerfAPI instrumented = new Instrumented();

    @Test
    public void testNothingStupidJava() {
    }

    public AspectJPerfTest() {
        super();
    }

    public AspectJPerfTest(String name, String desc) {
        super();
        output.append(name);
        output.append("\n");
        output.append(desc);
        output.append("\n");
        output.append(String.format("%d threads, %d repetitions of %d calls", numthreads, runs, iterations_per_run));
        output.append("\n");
    }

    public AspectJPerfTest(String name, String desc, int runs, int iterations_per_run) {
        super();
        this.runs = runs;
        this.iterations_per_run = iterations_per_run;
        output.append(name);
        output.append("\n");
        output.append(desc);
        output.append("\n");
        output.append(String.format("%d threads, %d repetitions of %d calls", numthreads, runs, iterations_per_run));
        output.append("\n");
    }

    protected void time(PerfRunnable r, AspectJPerfAPI api, Timer t) {
        t.start();
        for (int i = 0; i < iterations_per_run; i++) {
            r.run(api);
        }
        t.stop(iterations_per_run);
    }

    protected void doTest(String testName, String testDesc, final PerfRunnable r) {
        // Runs the testing threads
        ExecutorService executor = Executors.newCachedThreadPool();

        // Control the multithreaded execution
        final CountDownLatch begin = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(numthreads);

        // Timers to aggregate results
        final Timer tu = new Timer();
        final Timer ti = new Timer();

        // Runs the test for one thread
        Runnable tester = new Runnable() {
            public void run() {
                try {
                    // Wait for the goahead
                    begin.await();

                    // Warmup
                    for (int i = 0; i < iterations_per_run; i++) {
                        r.run(instrumented);
                        r.run(uninstrumented);
                    }

                    // Run
                    for (int i = 0; i < runs; i++) {
                        time(r, uninstrumented, tu);
                        time(r, instrumented, ti);
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
        recordResults(testName, testDesc, ti, tu);
    }

    private void recordResults(String testName, String testDesc, Timer ti, Timer tu) {
        output.append(String.format("- %s\t//%s\n", testName, testDesc));
        output.append(String.format("    wallclock = %.2f vs %.2f\n", tu.hrt(), ti.hrt()));
        output.append(String.format("    cpu clock = %.2f vs %.2f\n", tu.cpu(), ti.cpu()));
    }

    public void printResults() {
        if (output.length() > 0) {
            output.append("\n");
            System.out.println(output);
        }
    }

}
