package edu.brown.cs.systems.retro.aggregation.aggregators;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport.Builder;
import edu.brown.cs.systems.retro.aggregation.Resource;

class TenantOperationAggregator {

    private static final AtomicIntegerFieldUpdater<TenantOperationAggregator> startedUpdater = AtomicIntegerFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "started");
    private static final AtomicIntegerFieldUpdater<TenantOperationAggregator> finishedUpdater = AtomicIntegerFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "finished");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalWorkUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalWork");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalWorkSquaredUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalWorkSquared");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalLatencyUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalLatency");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalLatencySquaredUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalLatencySquared");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalOccupancyUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalOccupancy");
    private static final AtomicLongFieldUpdater<TenantOperationAggregator> totalOccupancySquaredUpdater = AtomicLongFieldUpdater.newUpdater(
            TenantOperationAggregator.class, "totalOccupancySquared");

    public final int tenantclass;
    public final Resource.Operation op;

    protected TenantOperationAggregator(int tenantclass) {
        this(null, tenantclass);
    }

    protected TenantOperationAggregator(Resource.Operation op, int tenantclass) {
        this.op = op;
        this.tenantclass = tenantclass;
    }

    private volatile int started;
    private volatile int finished;

    private volatile long totalWork;
    private volatile long totalWorkSquared;

    private volatile long totalLatency;
    private volatile long totalLatencySquared;

    private volatile long totalOccupancy;
    private volatile long totalOccupancySquared;

    /**
     * Indicate that we have just started an operation on this resource
     */
    public void started() {
        startedUpdater.getAndIncrement(this);
    }

    /**
     * Indicate that we have just finished an operation on this resource
     * 
     * @param work
     *            the amount of work that was done
     * @param latency
     *            the wallclock time in nanoseconds that it took to complete
     *            this work
     */
    public void finished(long work, long latency) {
        finishedUpdater.getAndIncrement(this);
        totalWorkUpdater.getAndAdd(this, work);
        totalWorkSquaredUpdater.getAndAdd(this, work * work);
        totalLatencyUpdater.getAndAdd(this, latency);
        totalLatencySquaredUpdater.getAndAdd(this, latency * latency);
    }

    /**
     * Returns a report for this usage
     */
    public TenantOperationReport getReport() {
        Builder builder = TenantOperationReport.newBuilder();
        builder.setTenantClass(tenantclass);
        if (op != null)
            builder.setOperation(op);
        builder.setNumStarted(started);
        builder.setNumFinished(finished);
        builder.setTotalWork(totalWork);
        builder.setTotalWorkSquared(totalWorkSquared);
        builder.setTotalLatency(totalLatency);
        builder.setTotalLatencySquared(totalLatencySquared);
        builder.setTotalOccupancy(totalOccupancy);
        builder.setTotalOccupancySquared(totalOccupancySquared);
        return builder.build();
    }
}