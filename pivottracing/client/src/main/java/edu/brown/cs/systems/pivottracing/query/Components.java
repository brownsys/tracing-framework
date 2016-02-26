package edu.brown.cs.systems.pivottracing.query;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;

public class Components {

    /** Exception thrown if a query is invalid */
    public static class PTQueryException extends Exception {
        private static final long serialVersionUID = 1L;
        public PTQueryException(String message) {
            super(message);
        }
        public PTQueryException(String format, Object... args) {
            super(String.format(format, args));
        }
    }

    /** A query variable */
    public static interface Var {
    }

    /** A filter, eg 'where x = 5' **/
    public static class WhereCondition {
        public final String replacementExpression; // Expression of how to perform the filter
        public final List<Var> replacementVariables; // Variables to replace in the expression; one variable per occurrence of {}
        public WhereCondition(String replacementExpression, Var[] replacementVariables) {
            this.replacementExpression = replacementExpression;
            this.replacementVariables = Lists.newArrayList(replacementVariables);
        }
    }
    
    /** A named or unnamed tracepoint */
    public static abstract class QuerySource {
        public final Tracepoint tracepoint;        
        public QuerySource(Tracepoint tracepoint) {
            this.tracepoint = tracepoint;
        }
        public abstract boolean available(String varName);
        public abstract boolean exports(String varName);
        public abstract String observedAs(String varName);
        public abstract String varName(String observedAs);
        public abstract String name();
    }
    
    /** A query source with a name, eg as the result of a "From x in Tracepoint"
     * Variables from this tracepoint will be of the form "name.variable" */
    public static class NamedQuerySource extends QuerySource {
        public final String name;
        public NamedQuerySource(String name, Tracepoint tracepoint) {
            super(tracepoint);
            this.name = name;
        }
        public boolean available(String varName) {
            return !varName.equals(name);
        }
        public boolean exports(String varName) {
            Pair<String, String> split = QueryUtils.split(varName);
            return split != null && name.equals(split.getLeft()) && tracepoint.exports(split.getRight());
        }
        public String observedAs(String varName) {
            return QueryUtils.split(varName).getRight();
        }
        public String varName(String observedAs) {
            return String.format("%s.%s", name, observedAs);
        }
        public String toString() {
            return String.format("From %s In %s", name, tracepoint.getName());
        }
        public String name() {
            return name;
        }
    }
    
    /** A query without a name, eg as the result of a direct "Join x in Tracepoint on x -> ..." */
    public static class UnnamedQuerySource extends QuerySource {
        public UnnamedQuerySource(Tracepoint tracepoint) {
            super(tracepoint);
        }
        public boolean available(String varName) {
            return !tracepoint.exports(varName);
        }
        public boolean exports(String varName) {
            return tracepoint.exports(varName);
        }
        public String observedAs(String varName) {
            return varName;
        }
        public String varName(String observedAs) {
            return observedAs;
        }
        public String toString() {
            return String.format("From %s", tracepoint.getName());
        }
        public String name() {
            return "<unnamed>";
        }
    }

    /** A variable constructed using a Let expression */
    public static class LetVar implements Var {
        public final String varName; // Name of the variable (eg, let x = ...)
        public final String replacementExpression; // Expression of how to create variable (eg, 2 * {} + 5)
        public final List<Var> replacementVariables; // Variables to replace in the expression; one variable per occurrence of {}

        public LetVar(String varName, String replacementExpression, List<Var> replacementVariables) {
            this.varName = varName;
            this.replacementExpression = replacementExpression;
            this.replacementVariables = replacementVariables;
        }
        public String toString() {
            return varName;
        }
    }

    /** A variable observed from a tracepoint */
    public static class ObservedVar implements Var {
        public final QuerySource source; // Source of the variable (eg, tracepoint)
        public final String observedAs; // Name of the variable observed
        public ObservedVar(QuerySource source, String observedAs) {
            this.source = source;
            this.observedAs = observedAs;
        }
        public String toString() {
            return observedAs;
        }
    }

    /** An aggregated variable, eg SUM(x) **/
    public static class AggVar implements Var {
        public Var aggregatedVar; // Variable to aggregate
        public Agg aggregationType; // Type of aggregation
        public AggVar(Var toAggregate, Agg how) {
            this.aggregatedVar = toAggregate;
            this.aggregationType = how;
        }
        public String toString() {
            return aggregatedVar == null ? aggregationType.toString() : aggregatedVar.toString();
        }
        public String toAggString() {
            return QueryPrintUtils.aggToString(aggregationType, aggregatedVar);
        }
    }

}
