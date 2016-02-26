package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class DiskCacheAggregator extends ResourceAggregator {

    private final boolean on;
    private final long threshold;

    public DiskCacheAggregator(long threshold) {
        super(Resource.Type.DISKCACHE);
        this.threshold = threshold;
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.DISKCACHE_AGGREGATION_ENABLED;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    /** Returns true if the READ operation was a cached operation. Kinda hacky */
    public boolean isCached(long time_nanos, long bytes) {
        return bytes * 1000000000 / (time_nanos + 1) >= threshold;
    }

    /** Indicate that the given tenant is beginning a READ disk cache operation */
    public void reading(int tenant) {
        starting(Operation.READ, tenant);
    }

    /** Indicate that the given tenant has finished a READ disk cache operation */
    public void read(int tenant, long time_nanos, long bytes) {
        finished(Operation.READ, tenant, bytes, time_nanos);
    }

}
