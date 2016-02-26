package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class QueueAggregator extends ResourceAggregator {

    private static final boolean on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.QUEUE_AGGREGATION_ENABLED;

    /**
     * @param lockid
     *            The ID of the lock object for whom we are aggregating usage
     *            statistics
     */
    public QueueAggregator(String lockid) {
        super(Resource.Type.QUEUE, lockid);
    }

    @Override
    public boolean enabled() {
        return on;
    }

    public static boolean valid() {
        return on;
    }

}
