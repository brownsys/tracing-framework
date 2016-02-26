package edu.brown.cs.systems.pivottracing.query;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.AggVar;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.ObservedVar;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;

public class QueryPrintUtils {
    
    public static String aggToString(Agg agg, Object o) {
        return o == null ? agg.name() : String.format("%s(%s)", agg.name(), o.toString());
    }
    
    public static String filterToString(Filter filter, Object o) {
        return o == null ? filter.name() : String.format("%s(%s)", filter.name(), o.toString()); 
    }
    
    public static String queryToString(PTQuery q, String nameForQuery) {
        StringBuilder b = new StringBuilder();
        
        // Print all dependent queries
        Map<String, String> queryNames = Maps.newHashMap();
        for (String queryVarName : q.happenedBefore.keySet()) {
            String queryName = nameForQuery+queryVarName.toUpperCase();
            queryNames.put(queryVarName, queryName);
            b.append(queryToString(q.happenedBefore.get(queryVarName), queryName));
            b.append("\n\n");
        }
        
        // From ...
        if ("".equals(nameForQuery)) {
            b.append(q.source.toString());
        } else {
            b.append(String.format("Q%s = %s", nameForQuery, q.source.toString()));            
        }
        
        // Join ...
        for (String hbName : q.happenedBefore.keySet()) {
            PTQuery other = q.happenedBefore.get(hbName);
            if (other instanceof PTQuery_Filter) {
                b.append(String.format("\nJoin %s In %s(Q%s) On %s -> %s", hbName, ((PTQuery_Filter) other).filter, queryNames.get(hbName), hbName, q.source.name()));
            } else {
                b.append(String.format("\nJoin %s In Q%s On %s -> %s", hbName, queryNames.get(hbName), hbName, q.source.name()));
            }
        }
        
        // Let ...
        for (LetVar lv : q.constructed.values()) {
            b.append(String.format("\nLet %s = %s %s", lv.varName, lv.replacementExpression, StringUtils.join(namesFor(q, lv.replacementVariables), ", ")));
        }
        
        // Where ...
        for (WhereCondition w : q.conditions) {
            b.append(String.format("\nWhere %s %s", w.replacementExpression, StringUtils.join(namesFor(q, w.replacementVariables), ", ")));
        }
        
        // GroupBy...
        if (q instanceof PTQuery_GroupBy) {
            PTQuery_GroupBy qgb = (PTQuery_GroupBy) q;
            boolean pack = "".equals(nameForQuery);
            b.append(String.format("\nGroupBy %s", StringUtils.join(namesFor(qgb, qgb.outputs(pack)), ", ")));
            b.append(String.format("\nSelect %s", StringUtils.join(namesFor(qgb, Iterables.concat(qgb.outputs(pack), qgb.aggregates(pack))), ", ")));
        }

        // Select ...
        if (q instanceof PTQuery_Select) {
            b.append(String.format("\nSelect %s", StringUtils.join(namesFor(q, ((PTQuery_Select) q).selected), ", ")));
        }
        
        return b.toString();
    }
    
    /** Get the fully qualified name for the var -- ignore the shorthands */
    public static String nameFor(PTQuery q, Var v) {
        if (q instanceof PTQuery_GroupBy) {
            PTQuery_GroupBy qgb = (PTQuery_GroupBy) q;
            if (v instanceof AggVar && qgb.aggregate.contains(v)) {
                AggVar av = (AggVar) v;
                if (av.aggregationType == Agg.COUNT && av.aggregatedVar == null) {
                    return "COUNT";
                } else {
                    return String.format("%s(%s)", av.aggregationType, nameFor(qgb, av.aggregatedVar));
                }
            }
        }
        
        if (q.constructed.containsValue(v)) {
            return ((LetVar) v).varName;
        } else if (q.observed.containsValue(v)) {
            return q.source.varName(((ObservedVar) v).observedAs);
        } else {
            for (String hb : q.happenedBefore.keySet()) {
                String name = nameFor(q.happenedBefore.get(hb), v);
                if (name != null) {
                    return String.format("%s.%s", hb, name);
                }
            }
        }
        return null;
    }
    
    public static String[] namesFor(PTQuery q, Var[] vs) {
        return namesFor(q, Lists.newArrayList(vs));
    }
    
    public static String[] namesFor(PTQuery q, Iterable<? extends Var> vs) {
        List<String> names = Lists.newArrayList();
        for (Var v : vs) {
            names.add(nameFor(q, v));
        }
        return names.toArray(new String[names.size()]);        
    }

}
