package edu.brown.cs.systems.retro.perf;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;

import edu.brown.cs.systems.clockcycles.CPUCycles;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;
import edu.brown.cs.systems.retro.perf.utils.PerfTest;

/**
 * Ballparks the cost of doing each clock-like operation
 * 
 * @author a-jomace
 */
public class TimerPerf extends PerfTest {

    int maxthreads = 64;

    private static final String name = "System call timing";
    private static final String description = "Tests cost of the system level timing calls and aggregation";

    public TimerPerf() {
        super(name, description);
    }

    @Test
    public void testXTraceAPICalls() {
        systemNanotime();
        CPUCycles();
        starting();
        finished();
        randomShared();
        randomPerThread();
        concurrentHashmapLookup();
        atomicReferenceArrayLookup();
        readLock();
        printResults();
    }

    public void systemNanotime() {
        Runnable r = new Runnable() {
            public void run() {
                System.nanoTime();
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("System.nanoTime()", r, numthreads);
        }
    }

    public void CPUCycles() {
        Runnable r = new Runnable() {
            public void run() {
                CPUCycles.get();
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("CPUCycles.get()", r, numthreads);
        }
    }

    private ResourceAggregator dummy = new ResourceAggregator(Type.DISK) {
        public boolean enabled() {
            return true; // say it's enabled so we invoke all aggregation code
        }
    };

    public void starting() {
        Runnable r = new Runnable() {
            public void run() {
                dummy.starting(1);
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("Aggregator.starting()", r, numthreads);
        }
    }

    public void finished() {
        Runnable r = new Runnable() {
            public void run() {
                dummy.finished(1, 2, 3);
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("Aggregator.finished()", r, numthreads);
        }
    }

    public void randomShared() {
        final Random rand = new Random();
        Runnable r = new Runnable() {
            public void run() {
                rand.nextDouble();
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("Random.nextDouble (shared)", r, numthreads);
        }
    }

    public void randomPerThread() {
        Runnable r = new Runnable() {
            private ThreadLocal<Random> r = new ThreadLocal<Random>() {
                public Random initialValue() {
                    return new Random(Thread.currentThread().getId());
                }
            };

            public void run() {
                r.get().nextDouble();
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("Random.nextDouble (perthread)", r, numthreads);
        }
    }

    public void concurrentHashmapLookup() {
        final ConcurrentHashMap<Integer, Object> tenantLookup = new ConcurrentHashMap<Integer, Object>();

        final Random rand = new Random();
        Runnable r = new Runnable() {
            public void run() {
                tenantLookup.get(rand.nextInt(100));
            }
        };

        for (int i = 0; i < 100; i++) {
            tenantLookup.put(i, new Object());
        }

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("ConcurrentHashMap.get", r, numthreads);
        }
    }

    public void atomicReferenceArrayLookup() {
        final AtomicReferenceArray<Object> data = new AtomicReferenceArray<Object>(100);

        final Random rand = new Random();
        Runnable r = new Runnable() {
            public void run() {
                data.get(rand.nextInt(100));
            }
        };

        for (int i = 0; i < 100; i++) {
            data.set(i, new Object());
        }

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("AtomicReferenceArray.get", r, numthreads);
        }
    }

    public void readLock() {
        final ReentrantReadWriteLock l = new ReentrantReadWriteLock();

        Runnable r = new Runnable() {
            public void run() {
                l.readLock().lock();
                l.readLock().unlock();
            }
        };

        for (int numthreads = 1; numthreads <= maxthreads; numthreads *= 2) {
            doTest("ReadWriteLock.ReadLock.lock/unlock", r, numthreads);
        }
    }

}
