package edu.brown.cs.systems.pivottracing;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AggVar;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.FilterSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.LetSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.WhereSpec;

public class PivotTracingUtils {
    
    public static String replace(String s, List<String> replacementVars) {
        for (String r : replacementVars) {
            s = s.replaceFirst("\\{\\}", r);
        }
        return s;
    }
    
    /** Get the baggage key where tuples for the specified query/advice will be stored */
    public static ByteString bagId(short queryId, short adviceId) {
        return ByteString.copyFrom(ByteBuffer.allocate(4).putShort(queryId).putShort(adviceId).array());
    }
    
    /** Decode the query ID from a bag id */
    public static short queryId(ByteString bagId) {
        return bagId.asReadOnlyByteBuffer().getShort();
    }
    
    /** Decode the advice ID from a bag id */
    public static short adviceId(ByteString bagId) {
        return bagId.asReadOnlyByteBuffer().getShort(2);
    }
    
    public static String tupleString(Object[] tuple) {
        return String.format("<%s>", StringUtils.join(tuple, ", "));
    }
    
    public static String varsToString(List<AggVar> vars) {
        List<String> strs = Lists.newArrayList();
        for (AggVar v : vars) {
            if (v.hasHow()) {
                strs.add(String.format("%s(%s)", v.getHow(), v.getName()));
            } else {
                strs.add(v.getName());
            }
        }
        return StringUtils.join(strs, ", ");
    }
    
    public static String bagIdToString(ByteString bagId) {
        return String.format("BAG-%d-%d", queryId(bagId), adviceId(bagId));
    }
    
    public static String specToString(AdviceSpec spec) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("OBSERVE %s\n", StringUtils.join(spec.getObserve().getVarList(), ", ")));
        for (UnpackSpec u : spec.getUnpackList()) {
            if (u.hasFilterSpec()) {
                b.append(String.format("UNPACK-%s %s %s\n", u.getFilterSpec().getFilter(), bagIdToString(u.getBagId()), vars(u.getFilterSpec())));
            } else if (u.hasTupleSpec()) {
                b.append(String.format("UNPACK %s %s\n", bagIdToString(u.getBagId()), vars(u.getTupleSpec())));
            } else if (u.hasGroupBySpec()) {
                b.append(String.format("UNPACK %s %s\n", bagIdToString(u.getBagId()), vars(u.getGroupBySpec())));
            }
        }
        for (LetSpec l : spec.getLetList()) {
            b.append(String.format("LET %s = %s\n", l.getVar(), replace(l.getExpression(), l.getReplacementVariablesList())));
        }
        for (WhereSpec w : spec.getWhereList()) {
            b.append(String.format("WHERE %s\n", replace(w.getPredicate(), w.getReplacementVariablesList())));
        }
        if (spec.hasEmit()) {
            EmitSpec emit = spec.getEmit();
            b.append(String.format("EMIT Q-%d ", queryId(emit.getOutputId())));
            if (emit.hasGroupBySpec()) {
                b.append(vars(emit.getGroupBySpec()));
            } else {
                b.append(vars(emit.getTupleSpec()));
            }
            b.append("\n");
        } else {
            PackSpec p = spec.getPack();
            if (p.hasFilterSpec()) {
                b.append(String.format("PACK-%s %s %s\n", p.getFilterSpec().getFilter(), bagIdToString(p.getBagId()), vars(p.getFilterSpec())));
            } else if (p.hasTupleSpec()) {
                b.append(String.format("PACK %s %s\n", bagIdToString(p.getBagId()), vars(p.getTupleSpec())));
            } else if (p.hasGroupBySpec()) {
                b.append(String.format("PACK %s %s\n", bagIdToString(p.getBagId()), vars(p.getGroupBySpec())));
            }
        }
        return b.toString();
    }
    
    public static String vars(TupleSpec spec) {
        return StringUtils.join(spec.getVarList(), ", ");
    }
    
    public static String vars(FilterSpec spec) {
        return StringUtils.join(spec.getVarList(), ", ");
    }
    
    public static String vars(GroupBySpec spec) {
        return StringUtils.join(Iterables.concat(spec.getGroupByList(), Lists.newArrayList(varsToString(spec.getAggregateList()))), ", ");
    }
    
    public static <T> Object[] toArray(T arrayOrCollection) {
        if (arrayOrCollection instanceof Collection) {
            return ((Collection)arrayOrCollection).toArray();
        } else {
            return (Object[]) arrayOrCollection;
        }
    }

}
