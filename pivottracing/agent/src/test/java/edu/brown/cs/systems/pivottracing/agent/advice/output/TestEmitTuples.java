package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.agent.PTAgent;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitTuples;
import junit.framework.TestCase;

public class TestEmitTuples extends TestCase {

    @Test
    public void testEmitTuplesSimple() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitTuples emit1 = new EmitTuples(emitspec1, tspec1);
        emit1.emit(Lists.<Object[]>newArrayList(new Object[] { "hello", "goodbye" }));
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(1, results.getTupleCount());
        assertEquals(0, results.getGroupCount());
        assertEquals(ResultsTuple.newBuilder().addValue("hello").addValue("goodbye").build(), results.getTuple(0));
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());
    }
    
    @Test
    public void testEmitTuplesWrongLength() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitTuples emit1 = new EmitTuples(emitspec1, tspec1);
        emit1.emit(Lists.<Object[]>newArrayList(new Object[] { "hello", "goodbye", "toolong!" }));
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(0, results.getTupleCount());
        assertEquals(0, results.getGroupCount());
    }
    
    @Test
    public void testEmitMultipleTuples() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitTuples emit1 = new EmitTuples(emitspec1, tspec1);
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye" },
                new Object[] { "five", "six" }
        );
        emit1.emit(tuples);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(2, results.getTupleCount());
        assertEquals(0, results.getGroupCount());
        for (int i = 0; i < tuples.size(); i++) {
            ResultsTuple.Builder rt = ResultsTuple.newBuilder();
            for (Object o : tuples.get(i)) {
                rt.addValue(String.valueOf(o));
            }
            assertEquals(rt.build(), results.getTuple(i));
        }
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());        
    }
    
    @Test
    public void testEmitMultipleTuples2() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitTuples emit1 = new EmitTuples(emitspec1, tspec1);
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye" },
                new Object[] { "five", "six" }
        );
        emit1.emit(tuples1);
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hel2lo", "go2odbye" },
                new Object[] { "fi2ve", "si2x" }
        );
        emit1.emit(tuples2);
        List<Object[]> tuples3 = Lists.<Object[]>newArrayList(
                new Object[] { "he3llo", "go3odbye" },
                new Object[] { "fi3ve", "s3ix" }
        );
        emit1.emit(tuples3);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(6, results.getTupleCount());
        assertEquals(0, results.getGroupCount());
        
        List<Object[]> allTuples = Lists.newArrayList(Iterables.concat(tuples1, tuples2, tuples3));
        for (int i = 0; i < allTuples.size(); i++) {
            ResultsTuple.Builder rt = ResultsTuple.newBuilder();
            for (Object o : allTuples.get(i)) {
                rt.addValue(String.valueOf(o));
            }
            assertEquals(rt.build(), results.getTuple(i));
        }
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());        
    }
    
    @Test
    public void testEmitNulls() {
        ByteString out1 = ByteString.copyFromUtf8("output query 1");
        TupleSpec tspec1 = TupleSpec.newBuilder().addVar("a").addVar("b").build();
        EmitSpec emitspec1 = EmitSpec.newBuilder().setOutputId(out1).setTupleSpec(tspec1).build();

        EmitTuples emit1 = new EmitTuples(emitspec1, tspec1);
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { null, "hello" },
                new Object[] { "five", "null" }
        );
        emit1.emit(tuples);
        
        AgentInfo myInfo = PTAgent.getAgentInfo();
        long timestamp = 100;
        
        QueryResults results = emit1.getResults(myInfo, timestamp);
        
        assertEquals(emitspec1, results.getEmit());
        assertEquals(myInfo, results.getAgent());
        assertEquals(timestamp, results.getTimestamp());
        assertEquals(2, results.getTupleCount());
        assertEquals(0, results.getGroupCount());
        for (int i = 0; i < tuples.size(); i++) {
            ResultsTuple.Builder rt = ResultsTuple.newBuilder();
            for (Object o : tuples.get(i)) {
                rt.addValue(String.valueOf(o));
            }
            assertEquals(rt.build(), results.getTuple(i));
        }
        
        QueryResults results2 = emit1.getResults(myInfo, timestamp);
        assertEquals(emitspec1, results2.getEmit());
        assertEquals(myInfo, results2.getAgent());
        assertEquals(timestamp, results2.getTimestamp());
        assertEquals(0, results2.getTupleCount());
        assertEquals(0, results2.getGroupCount());             
    }

}
