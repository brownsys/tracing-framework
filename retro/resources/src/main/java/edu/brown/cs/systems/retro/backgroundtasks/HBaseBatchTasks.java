package edu.brown.cs.systems.retro.backgroundtasks;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.BatchAggregator;
import edu.brown.cs.systems.retro.resources.CPUTracking;

/**
 * Convenience functions for HDFS background task instrumentation
 * 
 * @author a-jomace
 */
public enum HBaseBatchTasks {
    FSHLOG("fshlog");

    private final String name;
    private final int tenantclass;
    private final BatchAggregator agg;

    private HBaseBatchTasks(String name) {
        this.name = name;
        this.tenantclass = ConfigFactory.load().getInt("resource-tracing.batch.hbase." + name);
        this.agg = new BatchAggregator(tenantclass);
    }

    public void start() {
        CPUTracking.pauseTracking();
        Retro.setTenant(tenantclass);
        CPUTracking.continueTracking();
        agg.starting(tenantclass, Operation.BACKGROUND);
    }

    public void complete(long work, long latency) {
        agg.finished(Operation.BACKGROUND, tenantclass, work, latency);
        Baggage.stop();
        CPUTracking.continueTracking();
    }

    public void contribute(long amount) {
        int tenant = Retro.getTenant();
        agg.startedAndFinished(Operation.OPEN, tenant, amount, 0);
    }

    public void contribute(long amount, long latency) {
        int tenant = Retro.getTenant();
        agg.startedAndFinished(Operation.OPEN, tenant, amount, latency);
    }

}
