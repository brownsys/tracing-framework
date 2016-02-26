package edu.brown.cs.systems.retro.throttling.schedulers;

import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.SchedulerSpecification;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.QueueAggregator;
import edu.brown.cs.systems.retro.throttling.Scheduler;

/**
 * A wrapper around an MClock instance that implements the Scheduler interface
 * 
 * @author a-jomace
 *
 */
public class NullScheduler implements Scheduler {

    private QueueAggregator aggregator;

    public NullScheduler(String name) {
        aggregator = LocalResources.getQueueAggregator(name);
    }

    private static final class RequestStats {
        public int tenant;
        public long begin;

        public RequestStats begin(QueueAggregator aggregator) {
            tenant = Retro.getTenant();
            aggregator.starting(tenant);
            begin = System.nanoTime();
            return this;
        }

        public RequestStats complete(QueueAggregator aggregator) {
            long end = System.nanoTime();
            aggregator.finished(tenant, end - begin, end - begin);
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
        request.get().begin(aggregator);
    }

    @Override
    public void complete() {
        request.get().complete(aggregator);
    }

    @Override
    public void complete(double actualCost) {
        // TODO: for now, mclock does NOT make up the difference between
        // estimated and actual cost
        complete();
    }

    @Override
    public void update(SchedulerSpecification spec) {
    }

    @Override
    public void clear() {
    }

    @Override
    public String toString() {
        return "NullScheduler";
    }

    @Override
    public void scheduleInterruptably() throws InterruptedException {
        request.get().begin(aggregator);
    }

    @Override
    public void scheduleInterruptably(double cost) throws InterruptedException {
        request.get().complete(aggregator);
    }

}
