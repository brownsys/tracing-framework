## Pivot Tracing Client

This project contains the client library for Pivot Tracing.  The client library provides APIs for defining tracepoints, constructing and parsing queries, compiling queries to advice, and installing queries by publishing them to the Pivot Tracing agents.

**Tracepoints** Pivot Tracing queries refer to *tracepoints*.  A tracepoint is a location in the application source code where instrumentation can run.  Pivot Tracing supports tracepoints for almost any point in code -- the entry or return of a function, or any specific line number.  For example, `DataNodeMetrics.incrBytesRead` refers to the following method:

	void org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics.incrBytesRead(int delta)

**Queries** The main client interaction with Pivot Tracing is with the use of *queries*.  A query refers to variables present at tracepoints.  The following query causes each machine to aggregate the `delta` argument each time `incrBytesRead` is invoked, grouping by the host name:

    From incr In DataNodeMetrics.incrBytesRead
    GroupBy incr.host
    Select incr.host, SUM(incr.delta)

**Installation** The client library provides functions for installing queries and receiving query results.  It also provides tools for querying the state of currently-installed instrumentation

**Configuration** The client library supports the following configuration options:

pivot-tracing {
	pubsub = {
	   server = ${pubsub.server}
	   commands_topic = "PTcommands"
	   results_topic = "PTresults"
	   status_topic = "PTstatus"
	}
}