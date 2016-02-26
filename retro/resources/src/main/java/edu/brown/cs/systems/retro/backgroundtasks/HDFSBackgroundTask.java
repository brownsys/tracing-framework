package edu.brown.cs.systems.retro.backgroundtasks;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.resources.CPUTracking;

/**
 * Convenience functions for HDFS background task instrumentation
 * 
 * @author a-jomace
 */
public enum HDFSBackgroundTask {
    REPLICATION("replication", Operation.REPLICATION), HEARTBEAT("heartbeat", Operation.HEARTBEAT), INVALIDATE("invalidate", Operation.INVALIDATE), FINALIZE(
            "finalize", Operation.FINALIZE), RECOVER("recover", Operation.RECOVER);

    private final String name;
    private final Operation op;
    private final int tenantclass;

    private HDFSBackgroundTask(String name, Operation op) {
        this.name = name;
        this.op = op;
        this.tenantclass = ConfigFactory.load().getInt("resource-tracing.background." + name);
    }

    public void start() {
        if (tenantclass != -1) {
            CPUTracking.finishTracking();
            Baggage.stop();
            Retro.setTenant(tenantclass);
            CPUTracking.startTracking();
            LocalResources.getHDFSBackgroundAggregator().starting(tenantclass, op);
        }
    }

    public void end(long latency) {
        if (tenantclass != -1) {
            LocalResources.getHDFSBackgroundAggregator().finished(tenantclass, op, latency);
            CPUTracking.finishTracking();
            Baggage.stop();
            CPUTracking.startTracking();
        }
    }

}
