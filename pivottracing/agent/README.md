## Pivot Tracing - Agent

The Pivot Tracing Agent is a daemon that runs inside all Pivot Tracing-enabled processes.  The agent receives commands to install queries, and publishes query results for all installed queries.  For example, the pre-instrumented version of HDFS is already configured to start a Pivot Tracing agent when the NameNode and DataNode processes start up.

To add a pivot tracing agent to your own project use the `pivottracing-agent` dependency:

    <dependency>
        <groupId>edu.brown.cs.systems</groupId>
        <artifactId>pivottracing-agent</artifactId>
        <version>4.0</version>
    </dependency>

The agent must be initialized when your process starts up, in order to connect to the pubsub server and listen for incoming commands.

	PivotTracing.initialize();

If you are using any of the AspectJ automatic instrumentation in your projects, the `PivotTracingInit.aj` aspect in `pivottracing/aspects` will automatically add a call to `PivotTracing.initialize()` to all main methods.  The [example pom.xml](../../docs/pom.html) demonstrates how to apply the automatic instrumentation.

#### Baggage

The Pivot Tracing Agent depends on Baggage instrumentation being present in order to evaluate happened-before joins.  If baggage is not present, or if it is disabled by setting `pivot-tracing.agent.use_baggage=false`, queries will still be invoked.  However, any queries that try to UNPACK tuples from the Baggage will receive no tuples, and thereby produce no output.  Single-tracepoint queries will continue to function without issue.

#### Dynamic Instrumentation

The Pivot Tracing Agent uses the Tracing Plane's [dynamic instrumentation](../../tracingplane/dynamic) library for rewriting and reloading classes to install queries.  The default JavaAgent based instrumentation should work for most JVMs.  However, if you find that on your particular platform it does not work, try adding the following command line arguments to use JDWP-based instrumentation instead:

	-Ddynamic-instrumentation.use_jdwp="true" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0

Dynamic Instrumentation can be disabled by setting `pivot-tracing.agent.use_dynamic=false`.  Queries based on dynamic instrumentation will no longer be invoked.  However, you will still be able to invoke queries that use hardcoded tracepoints.

#### Hardcoded Tracepoints

If you do not wish to use dynamic instrumentation for your queries, or if there are certain tracepoints that are common and you do not wish to incur class reload overheads, you may opt to use hardcoded tracepoints instead.  A hardcoded tracepoint must be registered with Pivot Tracing when the process starts up by invoking `PivotTracing.registerHardcodedTracepoint(String id, String... exports)`.  This function requires a unique ID for the tracepoint, along with variable names that the tracepoint exports.  The method returns a HardcodedTracepoint instance that should be saved for use later.  You must manually call its `Advise(Object... exports)` method any time it should be invoked, passing the variables corresponding to the configured exports.  The following shows example usage:

    class MyClass {
        private static final HardcodedTracepoint TP_MYMETHOD_ENTRY = PivotTracing.registerHardcodedTracepoint("myMethod_entry", "a", "b");
        private static final HardcodedTracepoint TP_MYMETHOD_EXIT = PivotTracing.registerHardcodedTracepoint("myMethod_entry", "a", "b", "ret");
        
        public void MyMethod(String a, long b) {
            TP_MYMETHOD_ENTRY.Advise(a, b);
            ... // method does things
            String retVal = ...
            ...
            TP_MYMETHOD_EXIT.Advise(a, b, retVal);
        }
    }

Internally, the call to Advise will do nothing if no queries have been installed at that tracepoint.  If a query is installed for the tracepoint, the appropriate arguments will be passed to the query's advice.  In the client library, to write a query at this tracepoint, invoke the following:

	Tracepoint t = HardcodedTracepoint.get("myMethod_entry", "a", "b");
	PTQuery.From(t).Select(..);

#### Query Results Aggregation

Query results are aggregated locally within a process rather than being published immediately.  The default reporting interval is 1 second.  By default, if a query has no results, it does not send a report, but this can be disabled.

#### Configuration

The Pivot Tracing Agent uses the following default configuration values:

	pivot-tracing {
		agent = {
			use_baggage = true			// Regardless of whether the system is baggage enabled or not, setting to false will disable using it
			use_dynamic = true			// Should the agent use dynamic instrumentation?
			report_interval_ms = 1000   // Report every 1 second
			emit_if_no_results = false	// If no output tuples, should we emit an empty message anyway
		}
	}

