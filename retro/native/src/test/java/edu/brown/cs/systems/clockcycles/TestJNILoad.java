package edu.brown.cs.systems.clockcycles;

import org.junit.Test;

public class TestJNILoad {

    @Test
    public void testLoadDoesntExcept() {
        CPUCycles.get();
    }

    @Test
    public void testCallsNative() {
        try {
            long cycles = CPUCycles.getNative();
            System.out.println("Native call succeeded - Nanotime: " + System.nanoTime() + " Cycles: " + cycles);
            cycles = CPUCycles.getNative();
            System.out.println("Native call succeeded - Nanotime: " + System.nanoTime() + " Cycles: " + cycles);
            cycles = CPUCycles.getNative();
            System.out.println("Native call succeeded - Nanotime: " + System.nanoTime() + " Cycles: " + cycles);
        } catch (Throwable t) {
            System.out.println("Native call failed");
        }
    }

}
