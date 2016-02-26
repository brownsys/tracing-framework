package edu.brown.cs.systems.pivottracing.query;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PivotTracingUtils;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.FilterSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.LetSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.ObserveSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.WhereSpec;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.WeaveSpec;
import edu.brown.cs.systems.pivottracing.query.Components.AggVar;
import edu.brown.cs.systems.pivottracing.query.Components.LetVar;
import edu.brown.cs.systems.pivottracing.query.Components.Var;
import edu.brown.cs.systems.pivottracing.query.Components.WhereCondition;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.tracing.ByteStrings;

/** Intermediary class between PT query and actual advice spec */
public class QueryAdvice {
    
    private static short queryIdSeed = 0; // Each query must have a unique ID
    
    private final short queryId; // Unique ID for this query, use short instead of int
    private short adviceIdSeed = 0; // Within a query, we might need to pack into several unique bags, one per advice
    
    public final Map<PTQuery, AdviceAtTracepoint> instances = Maps.newHashMap(); // Advice instance by query
    public final List<AdviceAtTracepoint> instanceList = Lists.newArrayList(); // List of instances
    
    /** Instance of an advice at a tracepoint */
    public class AdviceAtTracepoint {
        public final ByteString id;
        public final AdviceSpec adviceSpec;
        public final Tracepoint tracepoint;
        public AdviceAtTracepoint(ByteString id, AdviceSpec spec, Tracepoint tracepoint) {
            this.id = id;
            this.adviceSpec = spec;
            this.tracepoint = tracepoint;
        }
        public WeaveSpec getWeaveSpec() {
            WeaveSpec.Builder b = WeaveSpec.newBuilder();
            b.addAllTracepoint(tracepoint.getTracepointSpecsForAdvice(adviceSpec));
            b.setAdvice(adviceSpec);
            b.setId(id);
            return b.build();
        }
    }
    
    /** Generates advice for the provided query */
    private QueryAdvice(PTQuery query) {
        this.queryId = queryIdSeed++;
        addQuery(query, false);
    }
    
    /** Generate advice for the provided query for all referenced tracepoints */
    public static QueryAdvice generate(PTQuery query) {
        return new QueryAdvice(query);
    }
    
    /** Generate the weave specs for this query */
    public List<WeaveSpec> getWeaveSpecs() {
        List<WeaveSpec> specs = Lists.newArrayList();
        for (AdviceAtTracepoint a : instanceList) {
            specs.add(a.getWeaveSpec());
        }
        return specs;
    }
    
    /** Get the IDs of all advice for this query */
    public List<ByteString> getAdviceIds() {
        List<ByteString> ids = Lists.newArrayList();
        for (AdviceAtTracepoint a : instanceList) {
            ids.add(a.id);
        }
        return ids;
    }
    
    /** Get the ID of this query */
    public ByteString getQueryId() {
        return ByteStrings.copyFrom(queryId);
    }
    
    private void addQuery(PTQuery query, boolean pack) {
        AdviceSpec.Builder b = AdviceSpec.newBuilder();
        
        // Ensure that varnames are unique
        VarSet vs = new VarSet();
        
        
        // OBSERVE
        ObserveSpec.Builder ob = b.getObserveBuilder();
        for (Var v : query.observed.values()) {
            ob.addVar(vs.get(v));
        }
        
        // UNPACK
        for (PTQuery hbQuery : query.happenedBefore.values()) {
            UnpackSpec.Builder ub = b.addUnpackBuilder();
            
            // Generate HB query's advice with PACK
            addQuery(hbQuery, true);     
            
            // Unpack from HB query's bag
            ub.setBagId(instances.get(hbQuery).id);
            
            // Determine unpack type
            if (hbQuery instanceof PTQuery_Filter) {
                PTQuery_Filter fHbQuery = (PTQuery_Filter) hbQuery;
                FilterSpec.Builder fb = ub.getFilterSpecBuilder().setFilter(fHbQuery.filter);
                for (Var v : hbQuery.outputs(true)) {
                    fb.addVar(vs.get(v));
                }
            } else if (hbQuery instanceof PTQuery_GroupBy) {
                PTQuery_GroupBy gbHbQuery = (PTQuery_GroupBy) hbQuery;
                GroupBySpec.Builder gbb = ub.getGroupBySpecBuilder();
                for (Var v : gbHbQuery.outputs(true)) {
                    gbb.addGroupBy(vs.get(v));
                }
                for (AggVar v : gbHbQuery.aggregates(true)) {
                    gbb.addAggregateBuilder().setName(vs.get(v)).setHow(v.aggregationType);
                }
            } else {
                TupleSpec.Builder tb = ub.getTupleSpecBuilder();
                for (Var v : hbQuery.outputs(true)) {
                    tb.addVar(vs.get(v));
                }
            }
        }
        
        // LET
        for (LetVar lv : query.constructed.values()) {
            LetSpec.Builder lb = b.addLetBuilder();
            lb.setVar(vs.get(lv));
            lb.setExpression(lv.replacementExpression);
            for (Var v : lv.replacementVariables) {
                lb.addReplacementVariables(vs.get(v));
            }
        }
        
        // WHERE
        for (WhereCondition wc : query.conditions) {
            WhereSpec.Builder wb = b.addWhereBuilder();
            wb.setPredicate(wc.replacementExpression);
            for (Var v : wc.replacementVariables) {
                wb.addReplacementVariables(vs.get(v));
            }
        }
        
        // Generate output vars
        FilterSpec.Builder fb = null;
        GroupBySpec.Builder gbb = null;
        TupleSpec.Builder tb = null;
        
        if (query instanceof PTQuery_Filter) {
            fb = FilterSpec.newBuilder().setFilter(((PTQuery_Filter) query).filter);
            for (Var v : query.outputs(pack)) {
                fb.addVar(vs.get(v));
            }
        } else if (query instanceof PTQuery_GroupBy) {
            gbb = GroupBySpec.newBuilder();
            PTQuery_GroupBy gbQuery = (PTQuery_GroupBy) query;
            for (Var v : gbQuery.outputs(pack)) {
                gbb.addGroupBy(vs.get(v));
            }
            for (AggVar v : gbQuery.aggregates(pack)) {
                gbb.addAggregateBuilder().setName(vs.get(v.aggregatedVar)).setHow(v.aggregationType);
            }
        } else {
            tb = TupleSpec.newBuilder();
            for (Var v : query.outputs(pack)) {
                tb.addVar(vs.get(v));
            }
        }
        
        // Generate an advice ID
        ByteString id = PivotTracingUtils.bagId(queryId, adviceIdSeed++);
        
        if (pack) {
            // PACK: packing into the baggage
            PackSpec.Builder pb = b.getPackBuilder().setBagId(id);
            if (fb != null) pb.setFilterSpec(fb);
            else if (gbb != null) pb.setGroupBySpec(gbb);
            else if (tb != null) pb.setTupleSpec(tb);
        } else {
            // EMIT: emitting for global aggregation, use query id for output id
            EmitSpec.Builder eb = b.getEmitBuilder().setOutputId(getQueryId());
            if (gbb != null) eb.setGroupBySpec(gbb);
            else if (tb != null) eb.setTupleSpec(tb);
        }
        
        AdviceAtTracepoint i = new AdviceAtTracepoint(id, b.build(), query.source.tracepoint);
        instances.put(query, i);
        instanceList.add(i);
    }
    
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (AdviceAtTracepoint a : instanceList) {
            b.append(String.format("A%d at %s\n", PivotTracingUtils.adviceId(a.id), a.tracepoint.getName()));
            b.append(PivotTracingUtils.specToString(a.adviceSpec));
            b.append("\n");
            b.append("\n");
        }
        return b.toString();
    }
    
    /** Utility class to help uniquely naming variables */
    private static class VarSet {
        private Map<Var, String> varIds = Maps.newHashMap();
        public String get(Var v) {
            if (!varIds.containsKey(v)) {
                varIds.put(v, String.format("%s.%s", varIds.size(), v));
            }
            return varIds.get(v);
        }
    }
    
}
