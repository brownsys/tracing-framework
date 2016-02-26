package edu.brown.cs.systems.retro.resources;

import edu.brown.cs.systems.clockcycles.CPUCycles;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.CPUAggregator;

/** Tracks CPU cycles and aggregates to the current tenant */
public class CPUTrackingOld {

    private static CPUAggregator aggregator = LocalResources.getCPUAggregator();

    private static final ThreadLocal<TrackingState> threadstatus = new ThreadLocal<TrackingState>() {
        public TrackingState initialValue() {
            return new TrackingState();
        }
    };

    public static boolean enabled() {
        return aggregator.enabled() && Retro.hasTenant(); // for now, only track
                                                          // cpu for tenants
                                                          // (not background)
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

    private static void starting() {
        if (enabled())
            aggregator.cpustart(Retro.getTenant());
    }

    private static void finished(long duration_on, long cycles_on, long duration_paused, long cycles_paused) {
        if (enabled()) {
            aggregator.cpuend(Retro.getTenant(), duration_on, cycles_on, duration_paused, cycles_paused);
        }
    }

    /** Used to keep track of cycles expended by a thread */
    private static final class TrackingState {

        private static enum STATUS {
            ON, OFF, PAUSED
        };

        public STATUS status = STATUS.OFF;
        public long lastcycles;
        public long lasthrt;

        public long cycles_on = 0;
        public long hrt_on = 0;
        public long cycles_paused = 0;
        public long hrt_paused = 0;

        private void reset() {
            cycles_on = 0;
            hrt_on = 0;
            cycles_paused = 0;
            hrt_paused = 0;
        }

        private void accumulate() {
            long currentcycles = CPUCycles.get();
            long currenthrt = System.nanoTime();
            if (status == STATUS.ON) {
                cycles_on += currentcycles - lastcycles;
                hrt_on += currenthrt - lasthrt;
            } else if (status == STATUS.PAUSED) {
                cycles_paused += currentcycles - lastcycles;
                hrt_paused += currenthrt - lasthrt;
            }
            lastcycles = currentcycles;
            lasthrt = currenthrt;
        }

        public void startTracking() {
            reset();
            lasthrt = System.nanoTime();
            lastcycles = CPUCycles.get();
            status = STATUS.ON;
            starting();
        }

        public void pauseTracking() {
            accumulate();
            status = STATUS.PAUSED;
        }

        public void continueTracking() {
            accumulate();
            status = STATUS.ON;
        }

        public void finishTracking() {
            accumulate();
            status = STATUS.OFF;
            if (cycles_on > 0) {
                finished(hrt_on, cycles_on, hrt_paused, cycles_paused);
            }
        }
    }
}
