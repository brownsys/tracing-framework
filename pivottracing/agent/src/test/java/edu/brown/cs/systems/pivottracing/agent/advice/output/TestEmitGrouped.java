package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsGroup;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.agent.PTAgent;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitGrouped;
import junit.framework.TestCase;

public class TestEmitGrouped extends TestCase {
    
    public static ResultBuilder groupby(Object... groupby) {
        return new ResultBuilder(groupby);
    }
    
    public static class ResultBuilder {
        ResultsGroup.Builder b = ResultsGroup.newBuilder();
        public ResultBuilder(Object... groupBy) {
            for (Object o : groupBy) {
                b.addGroupBy(String.valueOf(o));
            }
        }
        public ResultsGroup aggregate(long... aggregates) {
            for (long l : aggregates) {
                b.addAggregation(l);
            }
            return b.build();
        }
    }

    @Test
    public void testEmitGroupedSimple() {
        int groupCount = 2;
        int aggCount = 4;
        
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
            new Object[] { "hello", "goodbye", 1, 2, 3, 4 }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(1, results.getGroupCount());
        
        ResultsGroup.Builder expect = ResultsGroup.newBuilder();
        expect.addGroupBy("hello").addGroupBy("goodbye").addAggregation(1).addAggregation(1).addAggregation(3).addAggregation(4);
        assertEquals(expect.build(), results.getGroup(0));
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());
    }

    @Test
    public void testEmitGroupedAggregates() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1, 2, 1, 1 },
                new Object[] { "hello", "friend", 8, 2, 5, 2 },
                new Object[] { "hello", "goodbye", 1, 100, 3, 4 },
                new Object[] { "hello", "goodbye", 7, 2, 3, 4 },
                new Object[] { "hello", "goodbye", 1, 2, 3, 1 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { "hello", "goodbye", 1, 2, 5, 4 }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(3, results.getGroupCount());
        
        Set<ResultsGroup> actual = Sets.newHashSet(results.getGroupList());
        Set<ResultsGroup> expect = Sets.newHashSet(
                groupby("hello", "goodbye").aggregate(11, 5, 1, 4),
                groupby("hello", "friend").aggregate(8, 1, 5, 2),
                groupby("friend", "goodbye").aggregate(16, 2, 5, 2)
        );
        assertEquals(expect, actual);
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());
    }

    @Test
    public void testEmitGroupedAggregates2() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1, 2, 1, 1 },
                new Object[] { "hello", "goodbye", 1, 100, 3, 4 }
        );

        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 7, 2, 3, 4 },
                new Object[] { "hello", "goodbye", 1, 2, 3, 1 },
                new Object[] { "hello", "goodbye", 1, 2, 5, 4 }
        );

        emit1.emit(tuples1);
        emit1.emit(tuples2);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(1, results.getGroupCount());
        
        ResultsGroup.Builder expect = ResultsGroup.newBuilder();
        expect.addGroupBy("hello").addGroupBy("goodbye").addAggregation(11).addAggregation(5).addAggregation(1).addAggregation(4);
        assertEquals(expect.build(), results.getGroup(0));
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());
    }
    
    @Test
    public void testNullGroupby() {        
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { null, "goodbye", 1, 2, 1, 1 },
                new Object[] { null, "friend", 8, 2, 5, 2 },
                new Object[] { null, "goodbye", 1, 100, 3, 4 },
                new Object[] { null, "goodbye", 7, 2, 3, 4 },
                new Object[] { null, "goodbye", 1, 2, 3, 1 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { null, "goodbye", 1, 2, 5, 4 }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(3, results.getGroupCount());
        
        Set<ResultsGroup> actual = Sets.newHashSet(results.getGroupList());
        Set<ResultsGroup> expect = Sets.newHashSet(
                groupby("null", "goodbye").aggregate(11, 5, 1, 4),
                groupby("null", "friend").aggregate(8, 1, 5, 2),
                groupby("friend", "goodbye").aggregate(16, 2, 5, 2)
        );
        assertEquals(expect, actual);
    }
    
    @Test
    public void testNullAggregate() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { null, "goodbye", null, 2, 1, 1 },
                new Object[] { null, "friend", 8, 2, null, 2 },
                new Object[] { null, "goodbye", 1, null, 3, 4 }, // null count is ok
                new Object[] { null, "goodbye", 7, null, 3, 4 }, // null count is ok
                new Object[] { null, "goodbye", 1, 2, null, 1 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { "friend", "goodbye", 8, 2, 5, 2 },
                new Object[] { null, "goodbye", 1, 2, null, 4 }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(2, results.getGroupCount());
        
        Set<ResultsGroup> actual = Sets.newHashSet(results.getGroupList());
        Set<ResultsGroup> expect = Sets.newHashSet(
                groupby("friend", "goodbye").aggregate(16, 2, 5, 2),
                groupby("null", "goodbye").aggregate(8,2,3,4)
        );
        assertEquals(expect, actual);        
    }
    
    @Test
    public void testWrongLength() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1, 2, 1, }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(0, results.getGroupCount());        

        
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1, 2, 1, 3, 5 }
        );
        emit1.emit(tuples2);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(0, results.getGroupCount());        
    }
    
    @Test
    public void testStringAggs() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        GroupBySpec.Builder gspec1 = GroupBySpec.newBuilder().addGroupBy("g1").addGroupBy("g2");
        gspec1.addAggregateBuilder().setName("a1").setHow(Agg.SUM);
        gspec1.addAggregateBuilder().setName("a2").setHow(Agg.COUNT);
        gspec1.addAggregateBuilder().setName("a3").setHow(Agg.MIN);
        gspec1.addAggregateBuilder().setName("a4").setHow(Agg.MAX);
        
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setGroupBySpec(gspec1).build();
        
        EmitGrouped emit1 = new EmitGrouped(emitspec1, gspec1.build());
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", "1", "2", "1", "1" },
                new Object[] { "hello", "friend", "8", "2", "5", "2" },
                new Object[] { "hello", "goodbye", "1", "100", "3", "4" },
                new Object[] { "hello", "goodbye", "7", "2", "3", "4" },
                new Object[] { "hello", "goodbye", "1", "2", 3, 1 },
                new Object[] { "friend", "goodbye", 8, 2, "5", 2 },
                new Object[] { "friend", "goodbye", 8, "2", "5", 2 },
                new Object[] { "hello", "goodbye", 1, 2, 5, 4 }
        );
        
        emit1.emit(tuples1);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(3, results.getGroupCount());
        
        Set<ResultsGroup> actual = Sets.newHashSet(results.getGroupList());
        Set<ResultsGroup> expect = Sets.newHashSet(
                groupby("hello", "goodbye").aggregate(11, 5, 1, 4),
                groupby("hello", "friend").aggregate(8, 1, 5, 2),
                groupby("friend", "goodbye").aggregate(16, 2, 5, 2)
        );
        assertEquals(expect, actual);
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());      
    }

}
