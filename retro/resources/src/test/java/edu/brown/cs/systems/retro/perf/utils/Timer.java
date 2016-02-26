package edu.brown.cs.systems.retro.perf.utils;

import java.util.concurrent.atomic.AtomicLong;

import edu.brown.cs.systems.clockcycles.CPUCycles;

public class Timer {

    private AtomicLong duration_hrt = new AtomicLong();
    private AtomicLong duration_cpu = new AtomicLong();

    private ThreadLocal<Long> start_hrt = new ThreadLocal<Long>();
    private ThreadLocal<Long> start_cpu = new ThreadLocal<Long>();

    private AtomicLong count = new AtomicLong();

    public void start() {
        start_hrt.set(System.nanoTime());
        start_cpu.set(CPUCycles.get());
    }

    public void stop(long numcompleted) {
        duration_hrt.addAndGet(System.nanoTime() - start_hrt.get());
        duration_cpu.addAndGet(CPUCycles.get() - start_cpu.get());
        count.addAndGet(numcompleted);
    }

    public double cpu() {
        return duration_cpu.get() / ((double) count.get());
    }

    public double hrt() {
        return duration_hrt.get() / ((double) count.get());
    }

    public long count() {
        return count.get();
    }

}
