package edu.brown.cs.systems.pivottracing.agent;

import org.junit.Test;

import junit.framework.TestCase;

public class TestDynamicTracepoint extends TestCase {
    
    public static long blah = 1;
    public static long z;
    
    public void f1(String x, String y, long z)  {
        for (int i = 0; i < 100; i++) {
            TestDynamicTracepoint.blah = System.currentTimeMillis();
        }
    }
    
    public void f2(String m, long p) {
        for (int i = 0; i < 100; i++) {
            TestDynamicTracepoint.blah = System.currentTimeMillis();
        }
    }

    @Test
    public void testInstallAdvice() {
//        PTAgentForTest test = new PTAgentForTest();
//        
//        f1("a", "b", 10);
//        
//        test.results.check();
//        
//
//        AdviceSpec advice1 = AdviceTest.newAdvice().observe("x").pack("bag1", "x").spec();
//        AdviceSpec advice2 = AdviceTest.newAdvice().observe("p").emit("bag1", "x", "p", Agg.SUM).spec();
//
//        TracepointSpec t1 = MethodTracepoints.forClass(TestDynamicTracepoint.class)
//                .method("f1")
//                .args(String.class, String.class, "long")
//                .export("z")
//                .build();
//        
//        TracepointSpec t2 = MethodTracepoints.forClass(TestDynamicTracepoint.class)
//                .method("f2")
//                .args(String.class, "long")
//                .export("p")
//                .build();
//        
////        System.out.println(t1);
//
//        WeaveSpec weave1 = WeaveSpec.newBuilder().setId(ByteString.copyFromUtf8("w1")).setAdvice(advice1).addTracepoint(t1).build();
//        WeaveSpec weave2 = WeaveSpec.newBuilder().setId(ByteString.copyFromUtf8("w2")).setAdvice(advice2).addTracepoint(t2).build();
//        
//        PivotTracingUpdate update = PivotTracingUpdate.newBuilder().addWeave(weave1).addWeave(weave2).build();
//
////        System.out.println("Done");
//        test.agent.install(update);
        
        
        
        // TODO
        
    }

}
