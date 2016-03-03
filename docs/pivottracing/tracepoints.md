## Pivot Tracing Client: Tracepoints

Tracepoints typically correspond to method invocations, and can be uniquely identified by their method signature.  For example, the `DataNodeMetrics.incrBytesRead` tracepoint specifically refers to the method:

	void org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics.incrBytesRead(int delta)

The `MethodTracepoint` class represents such a tracepoint.  To create a tracepoint for this method, we can invoke the static API:

	MethodTracepoint hdfs_datanode_incrBytesRead = Tracepoints.method( "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics", "incrBytesRead", "int" );

This is shorthand for the following:

    MethodTracepoint hdfs_datanode_incrBytesRead = new MethodTracepoint(
        "DataNodeMetrics.incrBytesRead",                                  // name for the tracepoint
        Where.ENTRY,                                                      // where in the method
        "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics", // class name
        "incrBytesRead",                                                  // method name
        "int"                                                             // method parameter types
    );

#### Exporting variables

Suppose we want to write a query that refers to the `delta` parameter of `incrBytesRead`.  When defining a tracepoint, we must explicitly export any variables that we want to use in a query like this.  To do this, we call the `addExport` method:

    hdfs_datanode_incrBytesRead.addExport("delta", "delta");

This defines a *query variable* called `delta`, whose value is derived from the *Java literal* delta.  

#### Default exports

Tracepoints export some variables by default, such as hostname, timestamp, etc.  They are equivalent to the following:

	hdfs_datanode_incrBytesRead.addExport( "host",       "edu.brown.cs.systems.tracing.Utils.getHost()"        );
	hdfs_datanode_incrBytesRead.addExport( "timestamp",  "System.currentTimeMillis()"                          );
	hdfs_datanode_incrBytesRead.addExport( "cpu",        "edu.brown.cs.systems.clockcycles.CPUCycles.get()"    );
	hdfs_datanode_incrBytesRead.addExport( "threadId",   "Thread.currentThread().getId()"                      );
	hdfs_datanode_incrBytesRead.addExport( "procId",     "edu.brown.cs.systems.tracing.Utils.getProcessID()"   );
	hdfs_datanode_incrBytesRead.addExport( "procName",   "edu.brown.cs.systems.tracing.Utils.getProcessName()" );

*(Be aware that this is research code, and we don't sanitize these Java literals, so as-is this is a serious security vulnerability)*

#### Special Variables

In addition the following Java literals can be used when adding a tracepoint export

* `$0` takes the value of `this`
* `$1, $2, $3, ...` take the values of the corresponding method arguments (and of course, they can also be referenced by name)
* `$_` takes the method return value if the tracepoint uses `Where.RETURN`.

See [this](http://jboss-javassist.github.io/javassist/tutorial/tutorial2.html) Javassist tutorial for more information on available variables.

#### Arrays and Collections

Pivot Tracing also provides handling for array and Collection variables.  For example, suppose we have the following class:

	class Users {
		Collection<String> activeUsers = Lists.newArrayList("Jon", "Jeff", "George");
		public void ping() {
		}
	}

Suppose `ping()` is called at a regular interval, and `activeUsers` is changing, and we want a query that keeps track of how often each user is in active users.  Consider the following query:

	FROM ping in Example.Ping
	GROUPBY ping.activeUser
	SELECT ping.activeUser, COUNT

Conceptually, each time `ping()` is invoked, we want to treat the invocation as multiple tuples, one tuple for each activeUser in the activeUsers collection.  Pivot Tracing tracepoints provice multi-exports to support collections and arrays:

	MethodTracepoint example_ping = Tracepoints.method("Users", "ping");
	example_ping.addMultiExport("activeUser", "activeUsers", "String");

Pivot Tracing will interpret the `activeUsers` variable as a collection or array, and pass each element to the configured query individually.

#### Hardcoded Tracepoints

Some systems can hard-code tracepoints, rather than relying on dynamic instrumentation.  A hardcoded tracepoint has a unique ID, and exports zero or more variables.  The following call will get a hardcoded tracepoint called "myMethod_entry".

	Tracepoint t = HardcodedTracepoint.get("myMethod_entry", "a", "b");

See [Pivot Tracing Agent](../../pivottracing/agent) for more details on hardcoded tracepoints.