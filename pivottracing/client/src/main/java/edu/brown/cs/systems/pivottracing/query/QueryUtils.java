package edu.brown.cs.systems.pivottracing.query;

import org.apache.commons.lang3.tuple.Pair;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;

public class QueryUtils {

    /** Splits a qualified varname into two parts, a left and a right Left will be the text before the first instance of '.' Right will be everything else
     * Returns null if the varname could not be split for any reason */
    public static Pair<String, String> split(String varName) {
        int splitIndex = varName.indexOf('.');
        if (splitIndex < 0) {
            return null;
        }
        String left = varName.substring(0, splitIndex);
        String right = varName.substring(splitIndex + 1);
        if (left == null || left.length() == 0 || right == null || right.length() == 0) {
            return null;
        }
        return Pair.of(left, right);
    }
    
    /** Splits an aggregated varname (eg, SUM(sumvar)) into the aggregation type and aggregated var */
    public static Pair<Agg, String> splitAgg(String varName) {
        if (varName.toUpperCase().equals(Agg.COUNT.toString())) {
            return Pair.of(Agg.COUNT, "");
        }
        int splitIndex = varName.indexOf('(');
        if (splitIndex < 0) {
            return null;
        }
        try {
            String var = varName.substring(splitIndex+1, varName.length()-1);
            Agg agg = Agg.valueOf(varName.substring(0, splitIndex).toUpperCase());
            return Pair.of(agg, var);
        } catch (Exception e) {
            return null;
        }
    }

    /** Splits a filtered query or tracepoint (eg FIRST(Q1)) into the filter type and query/tracepoint name */
    public static Pair<Filter, String> splitFilter(String varName) {
        int splitIndex = varName.indexOf('(');
        if (splitIndex < 0) {
            return null;
        }
        try {
            String var = varName.substring(splitIndex+1, varName.length()-1);
            Filter filter = Filter.valueOf(varName.substring(0, splitIndex).toUpperCase());
            return Pair.of(filter, var);
        } catch (Exception e) {
            return null;
        }
    }

    /** Checks to see whether the provided varName is valid for referring to tracepoints, constructed variables, or aggregations. */
    public static boolean validVarName(String varName) {
        return !varName.contains(".");
    }

    /** Checks to see whether the provided varName is valid for referring to tracepoints, constructed variables, or aggregations. 
     * Throws an exception if not valid. */
    public static void validateVarName(String varName) throws PTQueryException {
        if (!validVarName(varName)) {
            throw new PTQueryException("Invalid var " + varName);
        }
    }
    
    /** The combiner function for multiple aggregations of a type */
    public static Agg combinerFor(Agg agg) {
        switch(agg) {
        case MIN: return Agg.MIN;
        case MAX: return Agg.MAX;
        default: return Agg.SUM;
        }
    }

}
