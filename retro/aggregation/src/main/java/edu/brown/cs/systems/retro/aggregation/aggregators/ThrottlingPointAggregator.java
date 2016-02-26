package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

/**
 * Aggregates consumption statistics for a throttling point.
 * 
 * Latency is determined by the amount of time a request waited due to
 * throttling
 * 
 * Work is a meaningless value and is reported with a value 0.
 * 
 * @author a-jomace
 *
 */
public class ThrottlingPointAggregator extends ResourceAggregator {

    private final boolean on;

    public ThrottlingPointAggregator(String throttlingPointName) {
        super(Resource.Type.THROTTLINGPOINT, throttlingPointName);
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.THROTTLINGPOINT_AGGREGATION_ENABLED;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    /**
     * If no throttling was required for the tenant to pass through the
     * throttling point
     */
    public void didNotThrottle(int tenantClass) {
        super.startedAndFinished(Operation.READ, tenantClass, 0, 0);
    }

    /**
     * If a tenant is about to be throttled at the throttling point
     */
    public void throttling(int tenantClass) {
        super.starting(Operation.READ, tenantClass);
    }

    /**
     * Once a tenant has finished being throttled and is about to proceed
     */
    public void throttled(int tenantClass, long latency) {
        super.finished(Operation.READ, tenantClass, 0, latency);
    }

}
