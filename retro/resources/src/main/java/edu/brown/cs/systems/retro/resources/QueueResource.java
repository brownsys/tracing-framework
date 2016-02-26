package edu.brown.cs.systems.retro.resources;

import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.QueueAggregator;
import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public class QueueResource {

    private static XTraceLogger xtrace = XTrace.getLogger("Queue");

    private final QueueAggregator aggregator;
    private String name;

    private final int capacity;

    public QueueResource(String name, int capacity) {
        this.name = name;
        this.capacity = capacity; // number of threads in thread pool
        this.aggregator = LocalResources.getQueueAggregator(name);
    }

    public void enqueue() {
        if (xtrace.valid())
            xtrace.log(JoinPointTracking.Caller.get(null), "threadpool-enqueue", "Queue", name);
        if (aggregator.enabled())
            aggregator.starting(Retro.getTenant());
    }

    public void starting(long enqueue, long start) {
        if (xtrace.valid())
            xtrace.log(JoinPointTracking.Caller.get(null), "threadpool-start", "Queue", name, "QueueDuration", start - enqueue);
    }

    public void finished(long enqueue, long start, long finish) {
        if (xtrace.valid())
            xtrace.log(JoinPointTracking.Caller.get(null), "threadpool-end", "Queue", name, "QueueDuration", start - enqueue, "ThreadDuration", finish - start);
        if (aggregator.enabled())
            aggregator.finished(Retro.getTenant(), finish - start, finish - enqueue);
    }

}
