package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

// Aggregates HDFS request information
public class BatchAggregator extends ResourceAggregator {

    private final boolean on;

    public BatchAggregator(int batch_tenant_id) {
        super(Resource.Type.BATCH, Integer.toString(batch_tenant_id));
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.HDFS_AGGREGATION_ENABLED;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    public void starting(int tenantclass, Operation op) {
        starting(op, tenantclass);
    }

    public void finished(int tenantclass, Operation op, long work, long latency) {
        finished(op, tenantclass, work, latency);
    }

}
