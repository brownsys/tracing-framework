package edu.brown.cs.systems.clockcycles;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import com.wapmx.nativeutils.jniloader.NativeLoader;

public class CPUCycles {

    private static final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
    private static final boolean linked;
    static {
        boolean success = false;
        try {
            NativeLoader.loadLibrary("threadcputimer");
            getNative(); // test get to make sure it works
            success = true;
        } catch (Throwable e) {
            System.out.println("Unabled to load native CPU timer library");
            e.printStackTrace();
        }
        linked = success;
    }

    /**
     * Returns the thread cycle timer for the current thread. If possible,
     * delegates this call to a native method, which will give a much higher
     * accuracy than calling the MXBean method.
     * 
     * @return
     */
    public static long get() {
        if (linked)
            return getNative();
        else
            return getJava();
    }

    /**
     * Calls the system high resolution thread cycle timer if supported
     * 
     * @return thread cpu time in nanoseconds
     */
    static native long getNative();

    /**
     * Calls the java thread cycle timer implementation. Is usually pretty crap
     * 
     * @return thread cpu time in nanoseconds
     */
    static long getJava() {
        return mxBean.getCurrentThreadCpuTime();
    }

    private static class MeaninglessWork implements Runnable {

        public double sum;

        @Override
        public void run() {
            while (true) {
                int x = 5;
                long cyclesstart = get();
                long start = System.nanoTime();
                while (System.nanoTime() - start < 100000000) {
                    x *= 2;
                }
                long cyclesend = get();
                sum += (cyclesend - cyclesstart) / 1000000.0;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int numthreads = 100;
        MeaninglessWork r = new MeaninglessWork();
        for (int i = 0; i < numthreads; i++) {
            new Thread(r).start();
        }
        long previous = System.nanoTime();
        while (true) {
            Thread.sleep(1000);
            System.out.println(r.sum / ((System.nanoTime() - previous) / 1000000000.0));
            previous = System.nanoTime();
            r.sum = 0;
        }
    }

}
