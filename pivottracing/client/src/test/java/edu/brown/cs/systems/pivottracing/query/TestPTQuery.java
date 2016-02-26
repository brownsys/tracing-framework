package edu.brown.cs.systems.pivottracing.query;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.examples.Examples;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.Parser.PTQueryParserException;
import junit.framework.TestCase;

public class TestPTQuery extends TestCase {

    String[][][] validQueries = { { { "from x in t0", "join y in t1 on y -> x", "select x.timestamp" } },
            { { "from x in t0", "join y in t1 on y -> x", "select x.timestamp, y.timestamp" } },
            { { "from x in t0" }, { "from x in t0", "join y in q0 on y -> x", "select x.timestamp, y.x.timestamp" } },
            { { "from x in t0", "select x.timestamp" }, { "from x in t0", "join y in q0 on y -> x", "select x.timestamp, y.x.timestamp" } },
            { { "from y in t0", "select y.timestamp" }, { "from x in t0", "join y in q0 on y -> x", "select x.timestamp, y.y.timestamp" } },
            { { "from x in t0", "select x.timestamp, x.host" },
                    { "from z in t1", "where {}<5 z.timestamp", "join y in q0 on y -> z", "groupby z.host", "select z.host, SUM(y.x.timestamp)" }, },
            { { "from x in t0", "select x.timestamp, x.host" },
                    { "from z in t1", "where {}<5 z.timestamp", "join y in q0 on y -> z", "groupby z.host", "select z.host, SUM(y.x.timestamp), COUNT" }, }

    };

    String[][][] invalidQueries = { { { "from x in t0", "join y in t1" } }, // missing 'on y -> x'
            { { "from x in t0", "join y in t1 on x -> y" } }, // incorrect order 'x -> y'
            { { "from x in t0", "join y in t1 on x -> z" } }, // unknown variable z
            { { "from x in t0", "join y in t1 on z -> y" } }, // unknown variable z
            { { "from x in t0", "join x in t1 on x -> x" } }, // duplicate variable 'x'
            { { "from x in t0", "join y in t1 on y -> x", "select x.timestamp, x.y.timestamp" } }, // unknown variable x.y
            { { "from x in t0" }, { "from x in t0", "join y in q1 on y -> x", "select x.timestamp, y.x.timestamp" } }, // unknown query
            { { "from x in t0" }, { "from x in t0", "join y in q0 on y -> x", "select x.timestamp, x.y.timestamp" } }, // unknown variable x.y
            { { "from x in t0" }, { "from x in t0", "join y in q0 on y -> x", "select x.timestamp, y.timestamp" } }, // unknown variable y.timestamp
            { { "from x in t0", "select x.host" },
                { "from z in t1", "where {}<5 z.timestamp", "join y in q0 on y -> z", "groupby z.host", 
                "select z.host, SUM(z.timestamp), SUM(y.x.timestamp), COUNT" }, // y.x.timestamp not included in select
        }
    };

    @Test
    public void testValidQueries() throws PTQueryParserException, PTQueryException {
        for (String[][] queries : validQueries) {
            PivotTracingClient pt = Examples.client();
            for (int i = 0; i < queries.length; i++) {
                String q = StringUtils.join(queries[i], "\n");
                PTQuery ptq = pt.parse("q" + i, q);
            }
        }
    }

    @Test
    public void testInvalidQueries() throws PTQueryParserException, PTQueryException {
        for (int j = 0; j < invalidQueries.length; j++) {
            String[][] queries = invalidQueries[j];
            try {
                PivotTracingClient pt = Examples.client();
                for (int i = 0; i < queries.length; i++) {
                    String q = StringUtils.join(queries[i], "\n");
                    PTQuery ptq = pt.parse("q" + i, q);
                }
                fail(String.format("Queries %d did not throw expected exception", j));
            } catch (Exception e) {
            }
        }
    }
}
