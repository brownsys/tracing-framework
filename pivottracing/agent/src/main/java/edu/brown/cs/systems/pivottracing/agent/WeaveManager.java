package edu.brown.cs.systems.pivottracing.agent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.dynamicinstrumentation.DynamicModification;
import edu.brown.cs.systems.pivottracing.PivotTracingUtils;
import edu.brown.cs.systems.pivottracing.agent.PTAgent.PTAgentException;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.TracepointSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.WeaveSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.AdviceImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.dynamicinstrumentation.MethodRewriteModification;

/** Keeps track of the advice that should be woven.
 * No changes are made to the woven advice until installChanges() is called. */
public class WeaveManager {

    private static final Logger log = LoggerFactory.getLogger(WeaveManager.class);
    
    private final PTAgent agent;
    
    private final Collection<Throwable> problems = Lists.newArrayList(); // Problems occurred during weaving
    private final Map<ByteString, Weave> woven = Maps.newHashMap(); // Woven advice
    private final Map<ByteString, WeaveSpec> pending = Maps.newHashMap(); // Pending advice to weave
    private final Set<Weave> removed = Sets.newHashSet(); // Pending advice to remove
    
    public WeaveManager(PTAgent agent) {
        this.agent = agent;
    }
    
    /** Modify and reinstall any classes that have changed since the last call to installChanges */
    public synchronized void install() {        
        // Process the removed weaves
        for (Weave w : removed) {
            w.destroy();
        }
        
        // Process the pending weaves.
        for (ByteString id : pending.keySet()) {
            // Unweave previously woven if exists
            Weave previous = woven.remove(id);
            if (previous != null) {
                previous.destroy();
            }
            
            // Weave new advice
            try {
                woven.put(id, new Weave(pending.get(id)));
            } catch (InvalidAdviceException e) {
                log.warn("Cannot install invalid advice", e);
                problems.add(e);
            } catch (PTAgentException e) {
                log.warn("Cannot install advice with invalid weave", e);
                problems.add(e);
            }            
        }
        
        // A lot of wasted work if dynamic instrumentation isn't enabled, that's ok for now
        if (agent.dynamic != null) {
            try {
                agent.dynamic.install();
            } catch (Throwable t) {
                log.warn("Unable to install modified classes", t);
                problems.add(t);
            }
        }
        
        // Reset our state
        pending.clear();
        removed.clear();
    }
    
    public synchronized Collection<Throwable> problems() {
        try {
            return Lists.newArrayList(problems); 
        } finally {
            problems.clear();
        }
    }
    
    /** Remove all woven advice. Clears all pending advice.  
     * Changes won't apply until install is called. */
    public synchronized void removeAll() {
        removed.addAll(woven.values());
        woven.clear();
        pending.clear();
    }
    
    /** Remove the indicated weave on next installation */
    public synchronized void unweave(ByteString id) {
        // Mark the weave as removed if it exists
        if (woven.containsKey(id)) {
            removed.add(woven.remove(id));
        }
    }
    
    /** Add the weave on next installation, potentially replacing an existing weave with the same id */
    public synchronized void weave(ByteString id, WeaveSpec spec) {
        // Add to pending, potentially overwriting an existing pending spec with this id.
        pending.put(id, spec);
    }
    
    /** Get the specs for all currently woven advice */
    public synchronized Collection<WeaveSpec> woven() {
        Collection<WeaveSpec> specs = Lists.newArrayList();
        for (Weave w : woven.values()) {
            specs.add(w.spec);
        }
        return specs;
    }

    // State of woven advice
    private class Weave {
        public final WeaveSpec spec;
        private final AdviceImpl advice;
        private final int adviceLookupId;
        private final List<DynamicModification> modifications = Lists.newArrayList();
        private final Multimap<HardcodedTracepoint, Integer> hardcoded = HashMultimap.create();

        /** Install advice in the advice manager.  Add all class modifications.
         * Mark all modified classes as dirty so that they are reloaded */
        public Weave(WeaveSpec spec) throws InvalidAdviceException, PTAgentException {
            this.spec = spec;
            advice = new AdviceImpl(spec.getAdvice(), agent.baggageApi, agent.emitApi);
            adviceLookupId = agent.adviceManager.register(advice);
            for (TracepointSpec tspec : spec.getTracepointList()) {
                if (tspec.hasMethodTracepoint()) {
                    DynamicModification m = new MethodRewriteModification(tspec.getMethodTracepoint(), adviceLookupId); 
                    modifications.add(m);
                    if (agent.dynamic != null) {
                        agent.dynamic.add(m);
                    }
                } else if (tspec.hasHardcodedTracepoint()) {
                    for (HardcodedTracepoint t : agent.hardcodedTracepoints.get(tspec.getHardcodedTracepoint().getId())) {
                        hardcoded.put(t, t.weave(advice, spec.getAdvice().getObserve().getVarList()));
                    }
                }
            }
            log.info("Adding weave Q {}, A {}", PivotTracingUtils.queryId(spec.getId()), PivotTracingUtils.adviceId(spec.getId()));
        }
        
        /** Unregister advice from the advice manager.  Remove all class modifications.
         * Mark all modified classes as dirty so that they are reloaded. */
        public void destroy() {
            agent.adviceManager.remove(adviceLookupId);
            if (agent.dynamic != null) {
                agent.dynamic.removeAll(modifications);
            }
            for (HardcodedTracepoint t : hardcoded.keySet()) {
                for (Integer wovenAdviceId : hardcoded.get(t)) {
                    try {
                        t.unweave(wovenAdviceId);
                    } catch (PTAgentException e) {
                        log.warn("Unable to unweave advice", e);
                    }
                }
            }
            advice.destroy();
            log.info("Weave removed Q {}, A {}", PivotTracingUtils.queryId(spec.getId()), PivotTracingUtils.adviceId(spec.getId()));
        }
    }
    

}
