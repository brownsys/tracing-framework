package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

// Aggregates HDFS background request information
public class HDFSBackgroundAggregator extends ResourceAggregator {

    private final boolean on;

    public HDFSBackgroundAggregator() {
        super(Resource.Type.HDFSBACKGROUND);
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.HDFS_AGGREGATION_ENABLED;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    /**
     * Indicate that the given tenantclass is starting an HDFS request to
     * perform the specified operation
     * 
     * @param tenantclass
     *            The class of the tenantclass that sent the HDFS request
     * @param op
     *            The operation performed by the HDFS request
     */
    public void starting(int tenantclass, Operation op) {
        starting(op, tenantclass);
    }

    /**
     * Indicate that the given tenantclass has finished an HDFS request that
     * performed the specified operation
     * 
     * @param tenantclass
     *            The class of the tenantclass that sent the HDFS request
     * @param op
     *            The operation performed by the HDFS request
     * @param latency
     *            The amount of time it took to complete the HDFS request, in
     *            nanoseconds
     */
    public void finished(int tenantclass, Operation op, long latency) {
        finished(op, tenantclass, 0, latency);
    }

}
