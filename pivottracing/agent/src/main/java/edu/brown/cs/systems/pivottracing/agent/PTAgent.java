package edu.brown.cs.systems.pivottracing.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicManager;
import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentStatus;
import edu.brown.cs.systems.pivottracing.PTAgentProtos.PivotTracingCommand;
import edu.brown.cs.systems.pivottracing.PTAgentProtos.PivotTracingUpdate;
import edu.brown.cs.systems.pivottracing.PivotTracingConfig;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.WeaveSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.tracing.Utils;

public class PTAgent implements PrivilegedAgent {
    
    private static final Logger log = LoggerFactory.getLogger(PTAgent.class);

    public final DynamicManager dynamic;
    public final BaggageAPI baggageApi;
    public final EmitAPI emitApi;
    
    final AdviceInstanceManager adviceManager = new AdviceInstanceManager();
    final WeaveManager weaveManager = new WeaveManager(this);
    final Multimap<String, HardcodedTracepoint> hardcodedTracepoints = HashMultimap.create();
    private final PubSubCommandSubscriber subscriber = new PubSubCommandSubscriber();

    /** Create a new PT agent using the provided dynamic instrumentation class.
     * @param baggageAPI the baggage API to use, that implements PACK
     * @param resultsAPI the results API to use, that implements EMIT
     * @param dynamic dynamic instrumentation to use, or null if no dynamic instrumentation
     */
    PTAgent(BaggageAPI baggageApi, EmitAPI emitApi, DynamicManager dynamic) {
        this.baggageApi = baggageApi;
        this.emitApi = emitApi;
        this.dynamic = dynamic;
        
        // Subscribe to PubSub commands
        PubSub.subscribe(PivotTracingConfig.COMMANDS_TOPIC, subscriber);
    }

    /**
     * Use this method if you want to explicitly announce a tracepoint from code. See {@link HardcodedTracepoint} for more details
     */
    public HardcodedTracepoint registerHardcodedTracepoint(String id, String... exports) {
        HardcodedTracepoint t = new HardcodedTracepoint(id, exports);
        hardcodedTracepoints.put(id, t);
        return t;
    }

    @Override
    public void Advise(int adviceId, Object[] observed) {
        Advice advice = adviceManager.lookup(adviceId);
        if (advice != null) {
            advice.advise(observed);
        }
    }

    @SuppressWarnings("serial")
    public static class PTAgentException extends Exception {
        public PTAgentException(String formatString, Object... args) {
            super(String.format(formatString, args));
        }
    }
    
    /** Update the installed PT advice */
    public void install(PivotTracingUpdate update) {
        // Remove all woven advice
        if (update.hasRemoveAll() && update.getRemoveAll()) {
            weaveManager.removeAll();
        }
        
        // Process weaves to remove
        for (ByteString weaveIdToRemove : update.getRemoveList()) {
            weaveManager.unweave(weaveIdToRemove);
        }
        
        // Add new weaves
        for (WeaveSpec weave : update.getWeaveList()) {
            weaveManager.weave(weave.getId(), weave);
        }
        
        // Install
        weaveManager.install();
    }
    
    /** Construct an AgentInfo message containing information about this agent */
    public static AgentInfo getAgentInfo() {
        return AgentInfo.newBuilder()
                .setProcName(Utils.getProcessName())
                .setProcId(Utils.getProcessID())
                .setHost(Utils.getHost())
                .build();
    }
    
    /** Publish a message reporting this PT agent's status */
    public void reportStatus() {
        AgentStatus.Builder status = AgentStatus.newBuilder();
        status.setAgent(getAgentInfo());
        status.setDynamicInstrumentationEnabled(dynamic != null);
        status.addAllWoven(weaveManager.woven());
        for (HardcodedTracepoint t : hardcodedTracepoints.values()) {
            status.addHardcodedTracepointsBuilder().setId(t.id).addAllExport(t.exportedVariables);
        }
        
        PubSub.publish(PivotTracingConfig.STATUS_TOPIC, status.build());
    }

    /** Receives commands over pubsub to weave advice */
    private class PubSubCommandSubscriber extends Subscriber<PivotTracingCommand> {
        @Override
        protected void OnMessage(PivotTracingCommand message) {
            log.info("Received command {}", message);
            
            // Install update maybe
            if (message.hasUpdate()) {
                install(message.getUpdate());
            }
            
            // Send status maybe
            if (message.hasSendStatus() && message.getSendStatus()) {
                reportStatus();
            }
        }
    }

}
