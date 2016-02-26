package edu.brown.cs.systems.pivottracing.query;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.AggVar;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.NamedQuerySource;
import edu.brown.cs.systems.pivottracing.query.Components.ObservedVar;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Components.QuerySource;
import edu.brown.cs.systems.pivottracing.query.Components.UnnamedQuerySource;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;

public abstract class PTQuery {

    public final QuerySource source;
    public final Map<String, ObservedVar> observed = Maps.newHashMap();
    public final Map<String, PTQuery> happenedBefore = Maps.newHashMap();
    public final Map<String, String> shortVars = Maps.newHashMap(); // short varnames to refer to upstream variables
    public final Map<String, LetVar> constructed = Maps.newHashMap();
    public final Set<WhereCondition> conditions = Sets.newHashSet();
    public final Set<Var> outputs = Sets.newHashSet();
    
    protected PTQuery(QuerySource source) {
        this.source = source;
    }

    /** Copy constructor */
    protected PTQuery(PTQuery other) {
        this.source = other.source;
        this.observed.putAll(other.observed);
        this.happenedBefore.putAll(other.happenedBefore);
        this.shortVars.putAll(other.shortVars);
        this.constructed.putAll(other.constructed);
        this.conditions.addAll(other.conditions);
        this.outputs.addAll(other.outputs);
    }
    
    /** Copy the query */
    public abstract PTQuery copy();
    
    /**
     * Start a new PT query directly from a tracepoint, omitting a variable name
     * 
     * From tracepoint
     * Select a,b,c
     */
    public static PTQuery_Partial From(Tracepoint tracepoint) {
        return new PTQuery_Partial(new UnnamedQuerySource(tracepoint));
    }

    /**
     * Start a new PT query with a variable name
     * 
     * From x In tracepoint
     * Select x.a, x.b, x.c
     */
    public static PTQuery_Partial From(String x, Tracepoint tracepoint) throws PTQueryException {
        QueryUtils.validateVarName(x);
        return new PTQuery_Partial(new NamedQuerySource(x, tracepoint));
    }
    
    /** Adds a happened-before to the provided query */
    public static <T extends PTQuery> T AddHappenedBefore(T query, String q, PTQuery Q) throws PTQueryException {
        T newQuery = (T) query.copy();
        newQuery.checkVarName(q);
        newQuery.happenedBefore.put(q, Q);
        return newQuery;
    }

    /** Adds a filtered happened-before to the provided query */
    public static <T extends PTQuery> T AddHappenedBefore(T query, Filter filter, String q, PTQuery_Partial Q) throws PTQueryException {
        T newQuery = (T) query.copy();
        PTQuery_Filter filtered = new PTQuery_Filter(Q, filter);
        newQuery.checkVarName(q);
        newQuery.happenedBefore.put(q, filtered);
        return newQuery;
    }

    /**
     * Happened-before join with another tracepoint.
     * If the current query is Q, then this call is:
     * 
     * From x In tracepoint
     * Join q In Q On q -> x
     */
    public PTQuery_Partial HappenedBefore2(String x, Tracepoint tracepoint, String q) throws PTQueryException {
        return AddHappenedBefore(From(x, tracepoint), q, this);
    }
    
    /** Return an optimized version of this query.
     * The current implementation, unfortunately, modifies existing queries that this one references.
     * TODO: don't modify existing queries */
    public PTQuery Optimize() {
        return copy().doOptimize();
    }
    
    protected PTQuery doOptimize() {
        // First, copy and optimize all hb queries
        Map<String, PTQuery> optimizedHappenedBefore = Maps.newHashMap();
        for (String hbVar : happenedBefore.keySet()) {
            optimizedHappenedBefore.put(hbVar, happenedBefore.get(hbVar).Optimize());
        }
        happenedBefore.clear();
        happenedBefore.putAll(optimizedHappenedBefore);
        
        // Determine the let conditions that can be pushed upwards
        boolean optimized = true;
        while (optimized) {
            optimized = false;
            
            optimizationloop:
            for (String letVarName : constructed.keySet()) {
                LetVar letVar = constructed.get(letVarName);
                for (PTQuery hb : happenedBefore.values()) {
                    if (hb.optimizable(letVar)) {
                        hb.constructed.put(letVarName, letVar); // TODO: this will have a bug where duplicate names can occur
                        constructed.remove(letVarName);
                        // TODO: might now have unnecessary outputs from hb
                        if (outputs.contains(letVar)) {     
                            hb.outputs.add(letVar);
                        }
                        optimized = true;
                        break optimizationloop;
                    }
                }
            }
        }
        
        // Determine the where conditions that can be pushed upwards
        optimized = true;
        while (optimized) {
            optimized = false;
            
            optimizationloop:
            for (WhereCondition where : conditions) {
                for (PTQuery hb : happenedBefore.values()) {
                    if (hb.optimizable(where)) {
                        hb.conditions.add(where);
                        conditions.remove(where);
                        // TODO: might now have unnecessary outputs from hb 
                        optimized = true;
                        break optimizationloop;
                    }
                }
            }
        }
        
        return this;
    }
    
    protected abstract boolean optimizable(WhereCondition condition);
    
    protected abstract boolean optimizable(LetVar let);
    
    /** Called to indicate that a query element in this query needs the specified variable as input */
    protected Var requireInput(String varName) throws PTQueryException {
        // Unroll the varname if it's shorthand
        if (shortVars.containsKey(varName)) {
            varName = shortVars.get(varName);
        }
        
        // First see if we constructed the variable here
        if (constructed.containsKey(varName)) {
            return constructed.get(varName);
        }

        // Check to see whether the source tracepoint exports the var
        if (source.exports(varName)) {
            String observedVarName = source.observedAs(varName);
            if (!observed.containsKey(observedVarName)) {
                observed.put(observedVarName, new ObservedVar(source, observedVarName));
            }
            return observed.get(observedVarName);
        }
        
        // See if the var originates in a joined query
        Pair<String, String> split = QueryUtils.split(varName);
        if (split==null) {
            throw new PTQueryException("Could not find var %s", varName);
        }
        
        // Unroll the varname if it's shorthand and try again
        if (shortVars.containsKey(split.getLeft())) {
            return requireInput(String.format("%s.%s", shortVars.get(split.getLeft()), split.getRight()));
        }
        
        // Check hb's
        if (happenedBefore.containsKey(split.getLeft())) {
            PTQuery copy = happenedBefore.get(split.getLeft()).copy();
            happenedBefore.put(split.getLeft(), copy);
            return copy.requireOutput(split.getRight());
        }
        
        // Nope
        throw new PTQueryException("Could not find var %s", varName);
    }
    
    protected Var[] requireInputs(String... varNames) throws PTQueryException {
        Var[] vars = new Var[varNames.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = requireInput(varNames[i]);
        }
        return vars;
    }
    
    /** Called to indicate that a subsequent query is looking for a var. */
    protected abstract Var requireOutput(String varName) throws PTQueryException;

    /** Check to see whether a proposed var name is available and valid. A var name is invalid if it contains invalid characters; if a var already exists by the
     * name; if it's the same name that we use to refer to the tracepoint; or if its the name we use to refer to other tracepoints we've joined with */
    public boolean available(String varName) {
        return QueryUtils.validVarName(varName) && !constructed.containsKey(varName) && !happenedBefore.containsKey(varName) && source.available(varName) && !shortVars.containsKey(varName);
    }

    /** Check to see whether a proposed var name is available and valid. A var name is invalid if it contains invalid characters; if a var already exists by the
     * name; if it's the same name that we use to refer to the tracepoint; or if its the name we use to refer to other tracepoints we've joined with */
    public void checkVarName(String varName) throws PTQueryException {
        if (!available(varName)) {
            throw new PTQueryException("Invalid var name " + varName);
        }
    }
    
    @Override
    public String toString() {
        return QueryPrintUtils.queryToString(this, "");
    }
    
    /** Variables that will be output by this query */
    Collection<Var> outputs(boolean pack) {
        return outputs;
    }
    
}
