package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class DiskAggregator extends ResourceAggregator {

    private final boolean on;
    private final long small_read_work;
    private final long small_read_seek;

    public DiskAggregator() {
        this(128 * 1024, 10000000);
    }

    public DiskAggregator(long small_read_work, long small_read_seek) {
        super(Resource.Type.DISK);
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.DISK_AGGREGATION_ENABLED;
        this.small_read_work = small_read_work;
        this.small_read_seek = small_read_seek;
    }

    @Override
    public boolean enabled() {
        return on;
    }

    @Override
    public void finished(Resource.Operation op, int tenantclass, long work, long latency) {
        if (op == Operation.READ && work <= small_read_work && latency >= small_read_seek) {
            super.startedAndFinished(Operation.SEEK, tenantclass, work, latency);
        }
        super.finished(op, tenantclass, work, latency);
    }

    /** Indicate that the given tenant is beginning an OPEN disk operation */
    public void opening(int tenant) {
        starting(Operation.OPEN, tenant);
    }

    /** Indicate that the given tenant is beginning a READ disk operation */
    public void reading(int tenant) {
        starting(Operation.READ, tenant);
    }

    /** Indicate that the given tenant is beginning a WRITE disk operation */
    public void writing(int tenant) {
        starting(Operation.WRITE, tenant);
    }

    /** Indicate that the given tenant is beginning a CLOSE disk operation */
    public void closing(int tenant) {
        starting(Operation.CLOSE, tenant);
    }

    /** Indicate that the given tenant is beginning a FLUSH disk operation */
    public void flushing(int tenant) {
        starting(Operation.FLUSH, tenant);
    }

    /** Indicate that the given tenant is beginning a SYNC disk operation */
    public void syncing(int tenant) {
        starting(Operation.SYNC, tenant);
    }

    /** Indicate that the given tenant has finished an OPEN disk operation */
    public void opened(int tenant, long time_nanos) {
        finished(Operation.OPEN, tenant, 0, time_nanos);
    }

    /** Indicate that the given tenant has finished a READ disk operation */
    public void read(int tenant, long time_nanos, long bytes) {
        finished(Operation.READ, tenant, bytes, time_nanos);
    }

    /** Indicate that the given tenant has finished a WRITE disk operation */
    public void wrote(int tenant, long time_nanos, long bytes) {
        finished(Operation.WRITE, tenant, bytes, time_nanos);
    }

    /** Indicate that the given tenant has finished a CLOSE disk operation */
    public void closed(int tenant, long time_nanos) {
        finished(Operation.CLOSE, tenant, 0, time_nanos);
    }

    /** Indicate that the given tenant has finished a FLUSH disk operation */
    public void flushed(int tenant, long time_nanos) {
        finished(Operation.FLUSH, tenant, 0, time_nanos);
    }

    /** Indicate that the given tenant has finished a SYNC disk operation */
    public void synced(int tenant, long time_nanos) {
        finished(Operation.SYNC, tenant, 0, time_nanos);
    }
}
