package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class CPUAggregator extends ResourceAggregator {

    private final boolean on;

    public CPUAggregator() {
        super(Resource.Type.CPU);
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.CPU_AGGREGATION_ENABLED;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    public void cpustart(int tenantclass) {
        // starting(Operation.FOREGROUND, tenantclass);
        // starting(Operation.BACKGROUND , tenantclass);
    }

    public void cpuend(int tenantclass, long time_on, long cycles_on, long time_paused, long cycles_paused) {
        if (cycles_on > 0)
            startedAndFinished(Operation.FOREGROUND, tenantclass, cycles_on, time_on);
        if (cycles_paused > 0)
            startedAndFinished(Operation.BACKGROUND, tenantclass, cycles_paused, time_paused);
    }

}
