package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.agent.PTAgent;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pubsub.PubSub;

public class EmitAPIImpl implements EmitAPI, Runnable {

    private static final Logger log = LoggerFactory.getLogger(EmitAPIImpl.class);
    
    // Runs threads as daemon threads, but ensures runnables finish before exiting
    private static final ScheduledExecutorService exec = MoreExecutors.getExitingScheduledExecutorService(
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1), 2, TimeUnit.SECONDS);

    final Set<EmitImpl> emits = Sets.newCopyOnWriteArraySet(); // Active emits that haven't been destroyed yet
    public final long reportInterval;
    public final String reportsTopic;
    public final boolean emitIfNoResults;

    public EmitAPIImpl(long reportInterval, String reportsTopic, boolean emitIfNoResults) {
        this.reportInterval = reportInterval;
        this.reportsTopic = reportsTopic;
        this.emitIfNoResults = emitIfNoResults;
        exec.scheduleAtFixedRate(this, reportInterval, reportInterval, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this)); // Also publish once on shutdown
    }

    @Override
    public Emit create(EmitSpec spec) throws InvalidAdviceException {
        EmitImpl impl;
        if (spec.hasTupleSpec()) {
            impl = new EmitTuples(spec, spec.getTupleSpec());
        } else if (spec.hasGroupBySpec()) {
            impl = new EmitGrouped(spec, spec.getGroupBySpec());
        } else {
            throw new InvalidAdviceException(spec, "EmitSpec lacks tuple or groupby spec");
        }
        emits.add(impl);
        return impl;
    }

    @Override
    public void destroy(Emit emit) {
        emits.remove(emit);
    }

    @Override
    public void run() {
        AgentInfo agentInfo = PTAgent.getAgentInfo();
        long timestamp = System.currentTimeMillis();
        for (EmitImpl emit : emits) {
            try {
                QueryResults results = emit.getResults(agentInfo, timestamp);
                if (emitIfNoResults || results.getTupleCount() > 0 || results.getGroupCount() > 0) {
                    PubSub.publish(reportsTopic, results);
                }
            } catch (Throwable t) {
                log.warn("Unable to publish query results", t);
            }
        }
    }

    /** Base class for Emits */
    static abstract class EmitImpl implements Emit {
        public abstract QueryResults getResults(AgentInfo agentInfo, long timestamp);
    }

}
