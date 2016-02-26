package edu.brown.cs.systems.retro.throttling.schedulers;

import java.util.Collection;

import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.Limit;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.Reservation;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.SchedulerSpecification;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.Weight;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.QueueAggregator;
import edu.brown.cs.systems.retro.throttling.Scheduler;
import edu.brown.cs.systems.retro.throttling.mclock.MClock;

/**
 * A wrapper around an MClock instance that implements the Scheduler interface
 * 
 * @author a-jomace
 *
 */
public class MClockScheduler implements Scheduler {

    public final MClock mclock;
    private QueueAggregator aggregator;

    public MClockScheduler(String name, int concurrency) {
        mclock = new MClock(concurrency);
        aggregator = LocalResources.getQueueAggregator(name);
    }

    private static final class RequestStats {
        public int tenant;
        public long waited;
        public long begin;
        private double cost;

        public RequestStats reset(QueueAggregator aggregator, double cost) {
            tenant = Retro.getTenant();
            this.cost = cost;
            aggregator.starting(tenant);
            return this;
        }

        public RequestStats begin(long waited) {
            this.waited = waited;
            begin = System.nanoTime();
            return this;
        }

        public RequestStats complete(QueueAggregator aggregator) {
            long end = System.nanoTime();
            aggregator.finished(Operation.READ, tenant, (long) cost, end - begin); // the
                                                                                   // actual
                                                                                   // operation
            aggregator.finished(Operation.FOREGROUND, tenant, end - begin, (end - begin) + waited); // the
                                                                                                    // queueing
                                                                                                    // time
            return this;
        }
    }

    private final ThreadLocal<RequestStats> request = new ThreadLocal<RequestStats>() {
        @Override
        public RequestStats initialValue() {
            return new RequestStats();
        }
    };

    @Override
    public void schedule() {
        schedule(1);
    }

    @Override
    public void schedule(double cost) {
        RequestStats request = this.request.get().reset(aggregator, cost);
        long waited = 0;
        try {
            waited = mclock.schedule(request.tenant, cost);
        } catch (InterruptedException e) {
        }
        request.begin(waited);
    }

    @Override
    public void scheduleInterruptably() throws InterruptedException {
        scheduleInterruptably(1);
    }

    @Override
    public void scheduleInterruptably(double cost) throws InterruptedException {
        RequestStats request = this.request.get().reset(aggregator, cost);
        long waited = mclock.schedule(request.tenant, cost);
        request.begin(waited);
    }

    @Override
    public void complete() {
        request.get().complete(aggregator);
        mclock.complete();
    }

    @Override
    public void complete(double actualCost) {
        // TODO: for now, mclock does NOT make up the difference between
        // estimated and actual cost
        complete();
    }

    @Override
    public void update(SchedulerSpecification spec) {
        if (spec != null) {
            mclock.setConcurrency(spec.getConcurrency());

            Collection<Integer> remaining = mclock.tenants();
            for (Reservation r : spec.getReservationList()) {
                mclock.setReservation(r.getTenantID(), r.getReservation());
                remaining.remove(r.getTenantID());
            }
            for (Integer tenantId : remaining) {
                mclock.resetReservation(tenantId);
            }

            remaining = mclock.tenants();
            for (Weight w : spec.getWeightList()) {
                mclock.setWeight(w.getTenantID(), w.getWeight());
                remaining.remove(w.getTenantID());
            }
            for (Integer tenantId : remaining) {
                mclock.resetWeight(tenantId);
            }

            remaining = mclock.tenants();
            for (Limit l : spec.getLimitList()) {
                mclock.setLimit(l.getTenantID(), l.getLimit());
                remaining.remove(l.getTenantID());
            }
            for (Integer tenantId : remaining) {
                mclock.resetLimit(tenantId);
            }
        }
    }

    @Override
    public void clear() {
        for (Integer tenantId : mclock.tenants()) {
            mclock.resetReservation(tenantId);
            mclock.resetWeight(tenantId);
            mclock.resetLimit(tenantId);
        }
    }

    @Override
    public String toString() {
        return mclock.toString();
    }

}
