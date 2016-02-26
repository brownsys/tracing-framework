package edu.brown.cs.systems.retro.perf;

import org.junit.Test;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.retro.perf.utils.AspectJPerfAPI;
import edu.brown.cs.systems.retro.perf.utils.AspectJPerfTest;

/**
 * Perf test of ETrace API calls
 */
public class XTraceAPICallsPerf extends AspectJPerfTest {

    private static final String name = "AspectJ CPU tracking perf";
    private static final String description = "Tests AspectJ cost of adding CPU tracking to XTrace get and set operations";

    public XTraceAPICallsPerf() {
        super(name, description);
    }

    @Test
    public void testXTraceAPICalls() {
        setBytes();
        setContext();
        setNull();
        joinBytes();
        joinContext();
        joinNull();
        stop();
        printResults();
    }

    private void setNull() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                api.setMetadata((DetachedBaggage) null);
            }
        };
        doTest("ETrace.SetCurrentTraceContext(null)", "set no context", r);
    }

    private void setBytes() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                Baggage.stop();
                api.setMetadata(bytes);
            }
        };
        doTest("ETrace.SetCurrentTraceContext(byte[])", "set xtrace context from network bytes", r);
    }

    private void setContext() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                Baggage.stop();
                api.setMetadata(context);
            }
        };
        doTest("ETrace.SetCurrentTraceContext(Context)", "set xtrace context from another thread or object", r);
    }

    private void joinBytes() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                Baggage.stop();
                api.joinMetadata(bytes);
            }
        };
        doTest("ETrace.ContinueTracing(byte[])", "join xtrace context from network bytes", r);

    }

    private void joinContext() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                Baggage.stop();
                api.joinMetadata(context);
            }
        };
        doTest("ETrace.ContinueTracing(Context)", "join xtrace context from another thread or object", r);
    }

    private void joinNull() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                api.joinMetadata((DetachedBaggage) null);
            }
        };
        doTest("ETrace.ContinueTracing(null)", "join no context", r);
    }

    private void stop() {
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                Baggage.start(context);
                api.unsetMetadata();
            }
        };
        doTest("ETrace.StopTracing()", "clear current xtrace context", r);
    }

}
