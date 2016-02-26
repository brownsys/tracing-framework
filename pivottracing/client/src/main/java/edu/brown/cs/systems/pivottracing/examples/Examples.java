package edu.brown.cs.systems.pivottracing.examples;

import org.apache.commons.lang3.StringUtils;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.agent.WeaveProtos.MethodTracepointSpec.Where;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.query.Parser.PTQueryParserException;
import edu.brown.cs.systems.pivottracing.tracepoint.MethodTracepoint;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;

public class Examples {
    
    /** Get a client loaded with the example tracepoints */
    public static PivotTracingClient client() {
        PivotTracingClient pt = new PivotTracingClient();
        for (Tracepoint t : tracepoints) {
            pt.addTracepoint(t);
        }
        return pt;
    }
    
    public static Tracepoint[] tracepoints = {
            new MethodTracepoint("t0", Where.ENTRY, "pt.test", "TestMethod", "java.lang.String", "int"),
            new MethodTracepoint("t1", Where.ENTRY, "pt.test", "TestMethod2", "java.lang.String", "int"),
            new MethodTracepoint("t2", Where.ENTRY, "pt.test", "TestMethod3", "java.lang.String", "int")
    }; 
    
    public static String[] examples = {
            "From x In t0"
            + "WHERE {}==\"MyHost\" x.host"
            + "Select x.host, x.timestamp"
    };
    
    public static void main(String[] args) throws PTQueryParserException, PTQueryException {
        PivotTracingClient pt = Examples.client();
        String[][] queries = {
                { "from x in t0", "groupby x.host", "select x.host, COUNT" },
                { "from z in t1", "join y in q0 on y -> z", "groupby z.host", "select z.host, max(y.COUNT)" },
        };
        pt.parse("q0", StringUtils.join(queries[0], "\n"));
        for (int i = 1; i < queries.length; i++) {
            String query = StringUtils.join(queries[i], "\n");
            PTQuery q = pt.parse("q"+i, query);
            System.out.println(q);
            System.out.println("\n--------------\n");
            System.out.println(q.Optimize());
            System.out.println("\n--------------\n");
        }
    }

}
