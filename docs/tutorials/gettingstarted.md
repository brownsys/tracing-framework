# Getting Started: HDFS

This section describes how to get started with a pre-instrumented version of HDFS that has all of the instrumentation libraries added to it.  We will run a version of Hadoop 2.7.2 instrumented with Baggage, and run an example X-Trace, Retro, and Pivot Tracing application.

Clone the [git repository](https://github.com/brownsys/tracing-framework)

	git clone git@github.com:brownsys/tracing-framework.git

From the directory you cloned into, build and install with the following command:

    mvn clean package install -DskipTests

Next, download our [pre-instrumented fork of Hadoop 2.7.2](https://github.com/brownsys/hadoop)

	git clone git@github.com:brownsys/hadoop.git
	
We want to use branch `brownsys-pivottracing-2.7.2`, which should be the default branch.

Build and install Hadoop using the following command:

    mvn clean package install -Pdist -DskipTests -Dmaven.javadoc.skip="true"

If you encounter any problems while building, try building the non-instrumented branch `branch-2.7.2`.  This will determine whether it is a Hadoop build issue, or a problem with our extra instrumentation.

### Running PubSub

All of the instrumentation libraries use a pub sub system to communicate.  Agents running in the instrumented Hadoop will receive commands over pubsub, and send their output back over the pubsub.  The pubsub project is in `tracingplane/pubsub`

The following command will start an X-Trace server which also includes a pub sub server:

	tracingplane/pubsub/target/appassembler/bin/server

To check which Java process are running, use the `jps` command.  This is an easy way to check if something has crashed.  You should expect to see a process called `XTraceServer`.

### Start HDFS

Now start HDFS as normal.  You should expect to see messages in the output stream along the lines of the following:

	Pivot Tracing initialized
	Resource reporting executor started
	ZmqReporter: QUEUE-disk -> queue
	ZmqReporter: DISK- -> disk
	ZmqReporter: CPU- -> cpu
	/META-INF/lib/libthreadcputimer.dylib extracted to temporary file /var/folders/d3/38f0syys5yjcvynz8p4n3t4w0000gn/T/jni_file_2410736199844535966.dll

Again, check which Java processes are running using the `jps` command.  In addition to `XTraceServer`, you should expect to see `NameNode` and `DataNode`