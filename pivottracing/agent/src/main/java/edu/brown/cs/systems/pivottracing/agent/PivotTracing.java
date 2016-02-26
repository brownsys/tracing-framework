package edu.brown.cs.systems.pivottracing.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicInstrumentation;
import edu.brown.cs.systems.dynamicinstrumentation.DynamicManager;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPIDisabled;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPIImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPI;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPIImpl;

/** Provides static methods for use by an instrumented system */
public class PivotTracing {
    
    static PTAgent agent = null;
    
    /** Return this process's PT agent, creating it if it hasn't been created already.
     * Ideally, a process should call initialize() when it begins */
    public static PTAgent agent() {
        if (agent == null) {
            initialize();
        }
        return agent;
    }
    
    /** Initialize the PivotTracing agent.  This call will subscribe to pubsub
     * in order to receive weave commands, and attempt to connect to the process's debug port */
    public static synchronized void initialize() {
        // Only initialize once
        if (agent != null) {
            return;
        }
        
        // Get the PT agent config
        Config config = ConfigFactory.load();
        boolean useBaggage = config.getBoolean("pivot-tracing.agent.use_baggage");
        boolean useDynamic = config.getBoolean("pivot-tracing.agent.use_dynamic");
        String resultsTopic = config.getString("pivot-tracing.pubsub.results_topic");
        int reportInterval = config.getInt("pivot-tracing.agent.report_interval_ms");
            
        // Create APIs
        BaggageAPI baggageApi = useBaggage ? new BaggageAPIImpl() : new BaggageAPIDisabled();
        EmitAPI emitApi = new EmitAPIImpl(reportInterval, resultsTopic);
        DynamicManager dynamic = useDynamic ? DynamicInstrumentation.get() : null;
        
        // Create the agent and register it with the privileged proxy
        agent = new PTAgent(baggageApi, emitApi, dynamic);
        PrivilegedProxy.Register(agent);
        
        System.out.println("Pivot Tracing initialized");
    }
    
    /** Use this method if you want to explicitly announce a tracepoint from code.
     * See {@link HardcodedTracepoint} for more details */
    public static HardcodedTracepoint registerHardcodedTracepoint(String id, String... exports) {
        return agent().registerHardcodedTracepoint(id, exports);
    }

}
