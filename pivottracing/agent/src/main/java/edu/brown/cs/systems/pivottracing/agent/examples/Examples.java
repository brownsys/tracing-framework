package edu.brown.cs.systems.pivottracing.agent.examples;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PivotTracingUtils;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.AdviceSpec;
import edu.brown.cs.systems.pivottracing.agent.Advice;
import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.pivottracing.agent.advice.AdviceImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;

public class Examples {
    
    public static void main(String[] args) throws InvalidAdviceException {
        
        ByteString id = PivotTracingUtils.bagId((short)1, (short)4);
        
//        AdviceSpec.Builder b = AdviceSpec.newBuilder();
//        b.getObserveBuilder().addVar("test1").addVar("test2");
//        b.getEmitBuilder().addVar(AggVar.newBuilder().setName("test2").build())
//                          .addVar(AggVar.newBuilder().setName("test1").build())
//                          .setOutputId(id);
//        b.addWhereBuilder().setPredicate("\"hello\".equals(\"{}\")").addReplacementVariables("test1");
//        
//        AdviceImpl i = new AdviceImpl(b.build());
//        
//        i.Advise("hello", "mate");
        

        
        AdviceSpec.Builder b2 = AdviceSpec.newBuilder();
        b2.getObserveBuilder().addVar("x").addVar("y");
        b2.addLetBuilder().setVar("z").setExpression("{}*{}+2").addReplacementVariables("x").addReplacementVariables("y");
        b2.getEmitBuilder().setOutputId(id).getTupleSpecBuilder().addVar("z").addVar("x");
        
        Advice i = new AdviceImpl(b2.build(), PivotTracing.agent().baggageApi, PivotTracing.agent().emitApi);
        
        i.advise("3", "5");
    }

}
