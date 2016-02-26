package edu.brown.cs.systems.retro.resources;

import edu.brown.cs.systems.clockcycles.CPUCycles;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.CPUAggregator;

/** Tracks CPU cycles and aggregates to the current tenant */
public class CPUTracking {

    private static final CPUAggregator aggregator = LocalResources.getCPUAggregator();

    private static final ThreadLocal<TrackingState> threadstatus = new ThreadLocal<TrackingState>() {
        public TrackingState initialValue() {
            return new TrackingState();
        }
    };

    public static boolean enabled() {
        return aggregator.enabled();
    }

    public static void startTracking() {
        if (enabled())
            threadstatus.get().startTracking();
    }

    public static void pauseTracking() {
        if (enabled())
            threadstatus.get().pauseTracking();
    }

    public static void continueTracking() {
        if (enabled())
            threadstatus.get().continueTracking();
    }

    public static void finishTracking() {
        if (enabled())
            threadstatus.get().finishTracking();
    }

    /** Used to keep track of cycles expended by a thread */
    private static final class TrackingState {

        private static enum STATUS {
            ON, OFF, PAUSED
        };

        public STATUS status = STATUS.OFF;
        public long cycles;
        public long hrt;

        private void report() {
            long currentcycles = CPUCycles.get();
            long currenthrt = System.nanoTime();
            long elapsedcycles = currentcycles - cycles;
            long elapsedhrt = currenthrt - hrt;
            int tenant = Retro.getTenant();
            if (!(tenant == -1 && elapsedhrt > 5000000000L)) { // disallow hrt
                                                               // greater than 5
                                                               // seconds for
                                                               // background
                                                               // cycles
                if (status == STATUS.ON) {
                    aggregator.cpuend(tenant, elapsedhrt, elapsedcycles, 0, 0);
                } else if (status == STATUS.PAUSED) {
                    aggregator.cpuend(tenant, 0, 0, elapsedhrt, elapsedcycles);
                }
            }
            hrt = currenthrt;
            cycles = currentcycles;
        }

        public void startTracking() {
            report();
            status = STATUS.ON;
        }

        public void pauseTracking() {
            report();
            status = STATUS.PAUSED;
        }

        public void continueTracking() {
            report();
            status = STATUS.ON;
        }

        public void finishTracking() {
            report();
            status = STATUS.PAUSED;
        }
    }
}
