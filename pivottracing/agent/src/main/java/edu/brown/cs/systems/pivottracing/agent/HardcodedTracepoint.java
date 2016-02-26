package edu.brown.cs.systems.pivottracing.agent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.agent.PTAgent.PTAgentException;

/** Tracepoints can be hard-coded instead of dynamically added, if need be.
 * Hard-coded tracepoints first register themselves statically with an ID and naming the keys they export.
 * Inline in code, the tracepoint can be invoked and must pass arguments equal to the declared exports.
 * To reduce overheads, since advice is likely not registered at the tracepoint, call .woven() to check
 * before actually invoking the tracepoint */
public class HardcodedTracepoint {
    
    private final AtomicInteger adviceIdSeed = new AtomicInteger(0);
    
    public final String id;
    public final int numExportedVariables;
    public final List<String> exportedVariables;
    private final AtomicReference<ConfiguredAdvice[]> woven;
    
    HardcodedTracepoint(String id, String[] exportedVariables) {
        this.id = id;
        this.exportedVariables = Lists.newArrayList(exportedVariables);
        this.numExportedVariables = exportedVariables.length;
        this.woven = new AtomicReference<ConfiguredAdvice[]>(new ConfiguredAdvice[0]);
    }
    
    /** Weave some advice here.  Returns a unique lookup ID */
    int weave(Advice advice, List<String> toObserve) throws PTAgentException {
        // Get indices of exported variables and make sure they are valid
        int[] observedVariableIndices = new int[toObserve.size()];
        for (int i = 0; i < observedVariableIndices.length; i++) {
            int exportIndex = exportedVariables.indexOf(toObserve.get(i));
            if (exportIndex == -1) {
                throw new PTAgentException("Hardcoded tracepoint %s does not export any variable %s", id, toObserve.get(i));
            } else {
                observedVariableIndices[i] = exportIndex;
            }
        }
        
        // Create and add the configured advice
        ConfiguredAdvice a = new ConfiguredAdvice(advice, observedVariableIndices);

        while (true) {
            // Get the existing array and check woven advice ids
            ConfiguredAdvice[] oldWoven = woven.get();
            for (ConfiguredAdvice e : oldWoven) {
                if (e.id == a.id) {
                    throw new PTAgentException("Cannot weave multiple advice with the same id %s", id);
                }
            }
            
            // Copy into a new array
            ConfiguredAdvice[] newWoven = new ConfiguredAdvice[oldWoven.length+1];
            System.arraycopy(oldWoven, 0, newWoven, 0, oldWoven.length);
            newWoven[oldWoven.length] = a;
            
            // CAS the array
            if (woven.compareAndSet(oldWoven, newWoven)) {
                return a.id;
            }
        }
    }
    
    /** Unweave some previously woven advice here */
    void unweave(int id) throws PTAgentException {
        while (true) {
            // Get the existing array and find the advice
            ConfiguredAdvice[] oldWoven = woven.get();
            boolean found = false;
            for (int i = 0; i < oldWoven.length; i++) {
                ConfiguredAdvice e = oldWoven[i];
                if (e.id == id) {
                    // Copy into a new array
                    ConfiguredAdvice[] newWoven = new ConfiguredAdvice[oldWoven.length-1];
                    System.arraycopy(oldWoven, 0, newWoven, 0, i);
                    System.arraycopy(oldWoven, i+1, newWoven, i, newWoven.length - i);
                    
                    // CAS the array
                    if (woven.compareAndSet(oldWoven, newWoven)) {
                        return;
                    } else {
                        // found but concurrent modifications
                        found = true;
                    }
                }
            }
            
            // Couldn't find the advice, throw an exception even though it means the advice is now unwoven
            if (!found) {
                throw new PTAgentException("No advice with ID %d woven at tracepoint %s", id, this.id);
            }
        }
    }
    
    /** Returns true if there is advice woven at this tracepoint and therefore it should be invoked */
    public boolean hasWovenAdvice() {
        return woven.get().length != 0;
    }
    
    /** Advise any advice that is woven here.  The same number of objects must be passed to advise as were registered initially.  
     * Null values are allowed.
     * For efficiency, call woven() before calling this method to check whether any advice is woven here. */
    public void Advise(Object... exports) {
        // Do nothing if exports don't match up with what was registered
        if (exports.length != numExportedVariables) {
            return;
        }
        ConfiguredAdvice[] woven = this.woven.get();
        if (woven.length == 0) {
            return;
        }
        for (int j = 0; j < woven.length; j++) {
            try {
                ConfiguredAdvice w = woven[j];

                // Pull out the observed variables
                Object[] observed = new Object[w.observedVariableIndices.length];
                for (int i = 0; i < observed.length; i++) {
                    observed[i] = exports[w.observedVariableIndices[i]];
                }

                // Pass the observed variables to the advice
                w.advice.advise(observed);
            } catch (Throwable t) {
                // All exceptions will be swallowed
            }
        }
    }
    
    /** Represents some advice woven here */
    private class ConfiguredAdvice {
        public final int id = adviceIdSeed.getAndIncrement();
        public final Advice advice;
        public final int[] observedVariableIndices;
        public ConfiguredAdvice(Advice advice, int[] observedVariableIndices) {
            this.advice = advice;
            this.observedVariableIndices = observedVariableIndices;
        }
    }

}
