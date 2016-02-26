package edu.brown.cs.systems.retro.resources.perf;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.PerfTest;
import edu.brown.cs.systems.retro.resources.RWLockResource;

public class RWLockResourcePerf extends PerfTest {

    int[] threads = new int[] { 1, 4, 16 };

    private static final String name = "ReadWriteLock Resource Timing";
    private static final String description = "Tests cost of the API call that measures and aggregates read write lock consumption";

    public RWLockResourcePerf() {
        super(name, description, 1, 5000);
    }

    @Test
    public void testDiskResource() {
        requestRead();
        acquiredRead();
        releaseRead();
        allRead();
        requestWrite();
        acquiredWrite();
        releaseWrite();
        allWrite();
        printResults();
    }

    RWLockResource l = new RWLockResource("perf");

    public void requestRead() {
        Runnable r = new Runnable() {
            public void run() {
                l.Read.request(0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Read.request", r, numthreads, true, false, false);
        }
    }

    public void acquiredRead() {
        Runnable r = new Runnable() {
            public void run() {
                l.Read.acquire(0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Read.acquire", r, numthreads, true, false, false);
        }
    }

    public void releaseRead() {
        Runnable r = new Runnable() {
            public void run() {
                l.Read.release(0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Read.release", r, numthreads, true, false, false);
        }
    }

    public void allRead() {
        Runnable r = new Runnable() {
            public void run() {
                l.Read.request(0, null);
                l.Read.acquire(0, 0, null);
                l.Read.release(0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("Total RWLockResource.Read", r, numthreads, true, false, false);
        }
    }

    public void requestWrite() {
        Runnable r = new Runnable() {
            public void run() {
                l.Write.request(0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Write.request", r, numthreads, true, false, false);
        }
    }

    public void acquiredWrite() {
        Runnable r = new Runnable() {
            public void run() {
                l.Write.acquire(0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Write.acquire", r, numthreads, true, false, false);
        }
    }

    public void releaseWrite() {
        Runnable r = new Runnable() {
            public void run() {
                l.Write.release(0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("RWLockResource.Write.release", r, numthreads, true, false, false);
        }
    }

    public void allWrite() {
        Runnable r = new Runnable() {
            public void run() {
                l.Write.request(0, null);
                l.Write.acquire(0, 0, null);
                l.Write.release(0, 0, 0, null);
            }
        };

        for (int numthreads : threads) {
            doTest("Total RWLockResource.Write", r, numthreads, true, false, false);
        }
    }
}
