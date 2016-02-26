package edu.brown.cs.systems.pivottracing.agent.advice;

import java.util.List;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AggVar;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.FilterSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;

public class AdviceUtils {

    
    public static List<String> varNames(PackSpec spec) throws InvalidAdviceException {
        if (spec.hasTupleSpec()) {
            return varNames(spec.getTupleSpec());
        } else if (spec.hasFilterSpec()) {
            return varNames(spec.getFilterSpec());
        } else if (spec.hasGroupBySpec()) {
            return varNames(spec.getGroupBySpec());
        } else {
            throw new InvalidAdviceException(spec, "Unknown bag type");
        }
    }
    
    public static List<String> varNames(UnpackSpec spec) throws InvalidAdviceException {
        if (spec.hasTupleSpec()) {
            return varNames(spec.getTupleSpec());
        } else if (spec.hasFilterSpec()) {
            return varNames(spec.getFilterSpec());
        } else if (spec.hasGroupBySpec()) {
            return varNames(spec.getGroupBySpec());
        } else {
            throw new InvalidAdviceException(spec, "Unknown bag type");
        }
    }
    
    public static List<String> varNames(EmitSpec spec) throws InvalidAdviceException {
        if (spec.hasTupleSpec()) {
            return varNames(spec.getTupleSpec());
        } else if (spec.hasGroupBySpec()) {
            return varNames(spec.getGroupBySpec());
        } else {
            throw new InvalidAdviceException(spec, "Unknown bag type");
        }
    }
    
    public static List<String> varNames(TupleSpec spec) {
        return spec.getVarList();
    }
    
    public static List<String> varNames(FilterSpec spec) {
        return spec.getVarList();
    }
    
    public static List<String> varNames(GroupBySpec spec) {
        List<String> names = Lists.newArrayList(spec.getGroupByList());
        for (AggVar agg : spec.getAggregateList()) {
            names.add(agg.getName());
        }
        return names;
    }

}
