package edu.brown.cs.systems.pivottracing.query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.query.Components.AggVar;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;

/**
 * A query that has had GroupBy applied 
 */
public class PTQuery_GroupBy extends PTQuery {
    
    public final List<Var> groupby = Lists.newArrayList();
    public final List<AggVar> aggregate = Lists.newArrayList();

    protected PTQuery_GroupBy(PTQuery_GroupBy other) {
        super(other);
        groupby.addAll(other.groupby);
        aggregate.addAll(other.aggregate);
    }
    
    protected PTQuery_GroupBy(PTQuery other, String... groupByVars) throws PTQueryException {
        super(other);
        for (String groupBy : groupByVars) {
            groupby.add(requireInput(groupBy));
        }
    }
    
    protected PTQuery_GroupBy(PTQuery other, Collection<Var> groupByVars) {
        super(other);
        groupby.addAll(groupByVars);
    }

    /** Sum a variable for each group */
    public PTQuery_GroupBy Sum(String varName) throws PTQueryException {
        return Aggregate(varName, Agg.SUM);
    }

    /**
     * Counts the number of tuples in each group
     * @param as
     *            The name to give the count, for later query elements to refer to
     * @return The PTGroupedQuery that further aggregations can be applied to
     * @throws PTQueryException
     */
    public PTQuery_GroupBy Count() throws PTQueryException {
        PTQuery_GroupBy copy = new PTQuery_GroupBy(this);
        copy.aggregate.add(new AggVar(null, Agg.COUNT));
        return copy;
    }

    /** Aggregate tuples within each group */
    public PTQuery_GroupBy Aggregate(String varName, Agg how) throws PTQueryException {
        PTQuery_GroupBy copy = new PTQuery_GroupBy(this);
        copy.aggregate.add(new AggVar(copy.requireInput(varName), how));
        return copy;
    }

    public PTQuery copy() {
        return new PTQuery_GroupBy(this);
    }

    @Override
    protected Var requireOutput(String varName) throws PTQueryException {
        // Check if it's an agg
        Pair<Agg, String> split = QueryUtils.splitAgg(varName);
        if (split != null) {
            Var v = split.getRight().equals("") ? null : requireInput(split.getRight());
            for (AggVar av : aggregate) {
                if (av.aggregationType == split.getLeft() && av.aggregatedVar == v) {
                    outputs.add(av);
                    return av;
                }
            }
            throw new PTQueryException("%s not included in Aggregate", varName);
        } else {
            Var v = requireInput(varName);
            if (groupby.contains(v)) {
                outputs.add(v);
                return v;                
            }
            throw new PTQueryException("%s not included in GroupBy", varName);
        }
    }
    
    /** Variables that will be output by this query */
    Collection<Var> outputs(boolean pack) {
        if (pack) {
            Set<Var> groupby_outputs = Sets.newHashSet(outputs);
            groupby_outputs.removeAll(aggregate);
            groupby_outputs.addAll(groupby);
            return groupby_outputs;
        } else {
            return groupby;
        }
    }
    
    /** Aggregations that will be output by this query */
    Collection<AggVar> aggregates(boolean pack) {
        if (pack) {
            return Sets.intersection(Sets.newHashSet(aggregate), Sets.newHashSet(outputs));
        } else {
            return aggregate;
        }
    }

    @Override
    protected boolean optimizable(WhereCondition condition) {
        return groupby.containsAll(condition.replacementVariables);
    }

    @Override
    protected boolean optimizable(LetVar let) {
        return false;
    }
    
    @Override
    protected PTQuery doOptimize() {
        super.doOptimize();
        
        // Optimize queries one at a time
        Map<String, PTQuery_GroupBy> optimized = Maps.newHashMap();
        
        hbloop:
        for (String hbVar : happenedBefore.keySet()) {
            PTQuery hb = happenedBefore.get(hbVar);
            
            // Exclude if the hb already ends in a groupby or filter
            if (hb instanceof PTQuery_Filter || hb instanceof PTQuery_GroupBy) {
                continue hbloop;
            }
            
            // Get the vars that originate in this happened before query
            Set<Var> groupbys = Sets.intersection(hb.outputs, Sets.newHashSet(groupby));
            Set<AggVar> aggregates = Sets.newHashSet();
            for (AggVar av : aggregate) {
                if (av.aggregatedVar != null && hb.outputs.contains(av.aggregatedVar)) {
                    aggregates.add(av);
                }
            }
            
            // If any of the aggregates appear in a WHERE or LET, then we cannot optimize
            for (WhereCondition where : conditions) {
                if (!Collections.disjoint(where.replacementVariables, aggregates)) {
                    continue hbloop;
                }
            }
            for (LetVar let : constructed.values()) {
                if (!Collections.disjoint(let.replacementVariables, aggregates)) {
                    continue hbloop;
                }
            }
            
            // Construct the new query
            PTQuery_GroupBy newHb = new PTQuery_GroupBy(hb, groupbys);
            for (AggVar av : aggregates) {
                // Twiddle the agg var so that ours is now a combiner
                AggVar pushed = new AggVar(av.aggregatedVar, av.aggregationType);
                av.aggregatedVar = pushed;
                av.aggregationType = QueryUtils.combinerFor(av.aggregationType);
                
                // Push the agg up
                newHb.aggregate.add(pushed);
                newHb.outputs.add(pushed);
                newHb.outputs.remove(pushed.aggregatedVar);
            }
            
            optimized.put(hbVar, newHb);
        }
        
        // Second pass: optimize counts
        for (AggVar av : aggregate) {
            if (av.aggregatedVar == null && av.aggregationType == Agg.COUNT) {
                List<Var> counts = Lists.newArrayList();
                for (PTQuery_GroupBy newHb : optimized.values()) {
                    AggVar hbav = new AggVar(null, Agg.COUNT);
                    newHb.aggregate.add(hbav);
                    newHb.outputs.add(hbav);
                    counts.add(hbav);
                }
                
                // Create the count sum variable
                String countSumName = "counts";
                LetVar countSum = new LetVar(countSumName, "1"+StringUtils.repeat("*{}", counts.size()), counts);
                constructed.put(countSumName, countSum);
                
                // Modify the count to now sum the sum
                av.aggregatedVar = countSum;
                av.aggregationType = Agg.SUM;
            }
        }
        
        // Reoptimize all optimized queries
        for (PTQuery newHb : optimized.values()) {
            newHb.doOptimize();
        }
        
        // Replace old queries with optimized
        happenedBefore.putAll(optimized);        
        
        return this;
    }

}
