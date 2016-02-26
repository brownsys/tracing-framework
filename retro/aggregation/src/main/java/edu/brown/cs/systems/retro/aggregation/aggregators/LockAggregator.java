package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class LockAggregator extends ResourceAggregator {

    private static final boolean on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.LOCKS_AGGREGATION_ENABLED;

    /**
     * @param lockid
     *            The ID of the lock object for whom we are aggregating usage
     *            statistics
     */
    public LockAggregator(String lockid) {
        super(Resource.Type.LOCKING, lockid);
    }

    @Override
    public boolean enabled() {
        return on;
    }

    public static boolean valid() {
        return on;
    }

    /**
     * Indicate that the specified tenantclass is about to request this lock
     * 
     * @param tenantclass
     *            the class of the tenant requesting the lock
     */
    public void requesting(int tenantclass) {
        starting(tenantclass);
    }

    /**
     * Indicate that the specified tenantclass just released this lock
     * 
     * @param tenantclass
     *            the class of the tenant whom just released this lock
     * @param hrt_requested
     *            the high resolution timer value when the lock was about to be
     *            requested, in nanoseconds
     * @param hrt_acquired
     *            the high resolution timer value just after the lock was
     *            acquired, in nanoseconds
     * @param hrt_released
     *            the high resolution timer value just after the lock was
     *            released, in nanoseconds
     */
    public void released(int tenantclass, long hrt_requested, long hrt_acquired, long hrt_released) {
        finished(tenantclass, hrt_released - hrt_acquired, hrt_released - hrt_requested);
    }
}
