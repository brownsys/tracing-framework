package edu.brown.cs.systems.retro.aggregation.aggregators;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;

public class AggregatorPerf extends TestCase {

    public static Operation[] ops = Operation.values();
    public static final Random random = new Random();

    public static Operation randomOperation() {
        return ops[random.nextInt(ops.length)];
    }

    private static final DecimalFormat format = new DecimalFormat("#.##");

    private void printResults(double duration, double count, double cpu) {
        System.out.println("  Time:  " + format.format(duration / 1000000000.0) + " seconds");
        System.out.println("  Count: " + count);
        System.out.println("  Avg:   " + format.format(duration / count) + " ns");
        System.out.println("  CPU:   " + format.format(cpu / count) + " cpu ns");
    }

    private static class DummyAggregator extends ResourceAggregator {
        public DummyAggregator() {
            super(Type.DISK);
        }

        @Override
        public boolean enabled() {
            return true; // say it's enabled so we invoke all aggregation code
        }
    }

    private static class DummyReporter implements Runnable {
        private ResourceAggregator aggregator;
        private int durationms;
        public volatile boolean alive = true;

        public DummyReporter(int durationms, ResourceAggregator aggregator) {
            this.durationms = durationms;
            this.aggregator = aggregator;
        }

        @Override
        public void run() {
            while (alive && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(durationms);
                    aggregator.getReport();
                } catch (InterruptedException e) {
                    alive = false;
                }
            }
        }
    }

    private volatile boolean interrupted = false;

    private void doTest(final int numtests, int numthreads, final int numtenants, final int numops, final int reportinterval) {
        if (interrupted || Thread.currentThread().isInterrupted()) {
            interrupted = true;
            fail("Tests interrupted");
        }

        final AtomicLong totalcount = new AtomicLong();
        final AtomicLong totalduration = new AtomicLong();
        final AtomicLong totalcpu = new AtomicLong();

        final CountDownLatch ready = new CountDownLatch(numthreads);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(numthreads);
        final ThreadMXBean threadbean = ManagementFactory.getThreadMXBean();

        final ResourceAggregator a = new DummyAggregator();
        final DummyReporter reporter = new DummyReporter(reportinterval, a);
        final Thread rthread = new Thread(reporter);
        if (reportinterval > 0)
            rthread.start();

        Runnable work = new Runnable() {
            @Override
            public void run() {
                Operation[] ops = AggregatorPerf.ops;
                ready.countDown();
                try {
                    long startcpu = threadbean.getCurrentThreadCpuTime();
                    long start = System.nanoTime();
                    long count = 0;
                    int tenant = 0, op = 0;
                    while (count < numtests) {
                        for (tenant = 0; tenant < numtenants; tenant++) {
                            for (op = 0; op < numops && count < numtests; op++) {
                                a.finished(ops[op], tenant, 1, 1);
                                count++;
                            }
                        }
                    }
                    long duration = System.nanoTime() - start;
                    long cycles = threadbean.getCurrentThreadCpuTime() - startcpu;

                    totalcount.getAndAdd(count);
                    totalduration.getAndAdd(duration);
                    totalcpu.getAndAdd(cycles);
                } finally {
                    done.countDown();
                }
            }
        };

        for (int i = 0; i < numthreads; i++) {
            new Thread(work).start();
        }

        try {
            ready.await();
        } catch (InterruptedException e) {
            interrupted = true;
        }

        go.countDown();

        try {
            done.await();
        } catch (InterruptedException e) {
        }

        if (!interrupted) {
            printResults(totalduration.get(), totalcount.get(), totalcpu.get());
        }
        reporter.alive = false;
    }

    @Test
    public void testOneSmall() {
        System.out.println("AGGREGATE 1 THREAD, 1 TENANT, 1 OP");
        doTest(50000000, 1, 1, 1, 0);
    }

    @Test
    public void testOneMedium() {
        System.out.println("AGGREGATE 1 THREAD, 3 TENANTS, 5 OPS");
        doTest(50000000, 1, 3, 5, 0);
    }

    @Test
    public void testTwoSmall() {
        System.out.println("AGGREGATE 10 THREAD, 1 TENANT, 1 OP");
        doTest(10000000, 10, 1, 1, 0);
    }

    @Test
    public void testTwoMedium() {
        System.out.println("AGGREGATE 10 THREAD, 3 TENANTS, 5 OPS");
        doTest(10000000, 10, 3, 5, 0);
    }

    @Test
    public void testThreeSmall() {
        System.out.println("AGGREGATE 100 THREAD, 1 TENANT, 1 OP");
        doTest(1000000, 100, 1, 1, 0);
    }

    @Test
    public void testThreeMedium() {
        System.out.println("AGGREGATE 100 THREAD, 3 TENANTS, 5 OPS");
        doTest(1000000, 100, 3, 5, 0);
    }

    @Test
    public void testOneSmallWithReporter() {
        System.out.println("AGGREGATE 1 THREAD, 1 TENANT, 1 OP + REPORTER");
        doTest(50000000, 1, 1, 1, 1000);
    }

    @Test
    public void testOneMediumWithReporter() {
        System.out.println("AGGREGATE 1 THREAD, 3 TENANTS, 5 OPS + REPORTER");
        doTest(50000000, 1, 3, 5, 1000);
    }

    @Test
    public void testTwoSmallWithReporter() {
        System.out.println("AGGREGATE 10 THREAD, 1 TENANT,  1 OP + REPORTER");
        doTest(10000000, 10, 1, 1, 1000);
    }

    @Test
    public void testTwoMediumWithReporter() {
        System.out.println("AGGREGATE 10 THREAD, 3 TENANTS, 5 OPS + REPORTER");
        doTest(10000000, 10, 3, 5, 1000);
    }

    @Test
    public void testThreeSmallWithReporter() {
        System.out.println("AGGREGATE 100 THREAD, 1 TENANT, 1 OP + REPORTER");
        doTest(1000000, 100, 1, 1, 1000);
    }

    @Test
    public void testThreeMediumWithReporter() {
        System.out.println("AGGREGATE 100 THREAD, 3 TENANTS, 5 OPS + REPORTER");
        doTest(1000000, 100, 3, 5, 1000);
    }

}
