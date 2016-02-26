package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.primitives.Longs;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPIImpl.EmitImpl;

public class EmitGrouped extends EmitImpl {
    public final EmitSpec emitSpec;
    public final GroupBySpec spec;
    public final BlockingQueue<List<Object[]>> pending = Queues.newLinkedBlockingQueue();
    
    private int tupleSize, groupKeyCount, aggregationCount;
    private Agg[] aggs;
    
    public EmitGrouped(EmitSpec emitSpec, GroupBySpec spec) {
        this.emitSpec = emitSpec;
        this.spec = spec;
        
        this.groupKeyCount = spec.getGroupByCount();
        this.aggregationCount = spec.getAggregateCount();
        this.tupleSize = groupKeyCount + aggregationCount;
        
        this.aggs = new Agg[spec.getAggregateCount()];
        for (int i = 0; i < spec.getAggregateCount(); i++) {
            aggs[i] = spec.getAggregate(i).getHow();
        }
    }
    
    public void emit(List<Object[]> tuples) {
        pending.add(tuples);
    }
    
    /** Interpret an Object as a Long, returning null if it cannot do it.  Checks if it's a Number or a String */
    private Long interpretLong(Object v) {
        if (v == null) {
            return null;
        } else if (v instanceof Number) {
            return ((Number) v).longValue();
        } else if (v instanceof String) {
            return Longs.tryParse((String) v);
        } else {
            return null;
        }
    }
    
    public Map<List<String>, List<Long>> process(Iterable<? extends Object[]> tuples) {
        // Turn into groups
        Map<List<String>, List<Long>> results = Maps.newHashMap();
        tupleloop:
        for (Object[] tuple : tuples) {
            // Ignore tuples of the wrong length
            if (tuple.length != tupleSize) {
                continue;
            }
            
            // Pull out tuple keys
            List<String> keys = Lists.newArrayListWithExpectedSize(groupKeyCount);
            for (int i = 0; i < groupKeyCount; i++) {
                keys.add(String.valueOf(tuple[i]));
            }
            
            // Pull out tuple values
            List<Long> values = Lists.newArrayListWithExpectedSize(aggregationCount);
            for (int i = 0; i < aggregationCount; i++) {
                if (aggs[i] == Agg.COUNT) {
                    values.add(1L); // Don't need to parse for count
                } else {
                    Long longValue = interpretLong(tuple[groupKeyCount + i]);
                    if (longValue == null) {
                        continue tupleloop;
                    } else {
                        values.add(longValue);
                    }
                }
            }
            
            // Either add the new group or merge with existing group
            List<Long> existing = results.get(keys);
            if (existing == null) {
                results.put(keys, values);
            } else {
                for (int i = 0; i < aggs.length; i++) {
                    switch (aggs[i]) {
                    case COUNT: existing.set(i, existing.get(i) + values.get(i)); break;
                    case SUM: existing.set(i, existing.get(i) + values.get(i)); break;
                    case MAX: existing.set(i, Math.max(existing.get(i), values.get(i))); break;
                    case MIN: existing.set(i, Math.min(existing.get(i), values.get(i))); break;
                    }
                }
            }
        }
        return results;
    }
    
    public QueryResults getResults(AgentInfo agentInfo, long timestamp) {
        // Drain all pending results
        List<List<Object[]>> pendingResults = Lists.newArrayList();
        pending.drainTo(pendingResults);
        
        // Process results
        Map<List<String>, List<Long>> results = process(Iterables.concat(pendingResults));
        
        // Construct the output message
        QueryResults.Builder b = QueryResults.newBuilder();
        b.setEmit(emitSpec);
        b.setAgent(agentInfo);
        b.setTimestamp(timestamp);
        
        // Add the groups
        for (Entry<List<String>, List<Long>> result : results.entrySet()) {
            b.addGroupBuilder().addAllGroupBy(result.getKey()).addAllAggregation(result.getValue());
        }
        return b.build();
    }
}