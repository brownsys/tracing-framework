package edu.brown.cs.systems.pivottracing.agent.advice.output;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.AgentInfo;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.EmitSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPIImpl.EmitImpl;

public class EmitTuples extends EmitImpl {
    
    public final EmitSpec emitSpec;
    public final TupleSpec spec;
    public final int tupleSize;
    public final BlockingQueue<List<Object[]>> pending = Queues.newLinkedBlockingQueue();

    public EmitTuples(EmitSpec emitSpec, TupleSpec spec) {
        this.emitSpec = emitSpec;
        this.spec = spec;
        this.tupleSize = spec.getVarCount();
    }

    public void emit(List<Object[]> tuples) {
        pending.add(tuples);
    }

    @Override
    public QueryResults getResults(AgentInfo agentInfo, long timestamp) {
        // Drain all pending results
        List<List<Object[]>> pendingResults = Lists.newArrayList();
        pending.drainTo(pendingResults);

        // Construct the output message
        QueryResults.Builder b = QueryResults.newBuilder();
        b.setEmit(emitSpec);
        b.setAgent(agentInfo);
        b.setTimestamp(timestamp);

        // Add the tuples
        for (Object[] tuple : Iterables.concat(pendingResults)) {
            if (tuple.length == tupleSize) {
                ResultsTuple.Builder tupleBuilder = b.addTupleBuilder();
                for (Object o : tuple) {
                    tupleBuilder.addValue(String.valueOf(o));
                }
            }
        }

        return b.build();
    }
}