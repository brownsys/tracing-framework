# Getting Started

This section describes how to get started with a pre-instrumented version of HDFS that has all of the instrumentation libraries added to it.  We will run a version of Hadoop 2.7.2 instrumented with Baggage, and run an example X-Trace, Retro, and Pivot Tracing application.

### Prerequisites

The tracing framework requires:
* [Maven 3](https://maven.apache.org/download.cgi)
* [Protocol Buffers 2.5](https://github.com/google/protobuf/releases/tag/v2.5.0)
* [AspectJ](https://eclipse.org/aspectj/downloads.php)

After cloning the [git repository](https://github.com/brownsys/tracing-framework), build and install with the following command:

    mvn clean package install -DskipTests

Next, download our [pre-instrumented fork of Hadoop 2.7.2](https://github.com/brownsys/hadoop) (use branch `brownsys-pivottracing-2.7.2`)

Build and install Hadoop using the normal approach:

    mvn clean package install -Pdist -DskipTests -Dmaven.javadoc="skip"

If you encounter any problems while building, try building the non-instrumented version of Hadoop (branch-2.7.2) to determine whether it is a Hadoop build issue or an instrumentation issue.

### Running PubSub

All of the instrumentation libraries use a pub sub system to communicate.  Agents running in the instrumented Hadoop will receive commands over pubsub, and send their output back over the pubsub.

The following command will start an X-Trace server which also includes a pub sub server:

	tracingplane/pubsub/target/appassembler/bin/server

### Start HDFS

Now start HDFS as normal.  You should expect to see messages in the output stream along the lines of the following:

	Pivot Tracing initialized
	Resource reporting executor started
	ZmqReporter: QUEUE-disk -> queue
	ZmqReporter: DISK- -> disk
	ZmqReporter: CPU- -> cpu
	/META-INF/lib/libthreadcputimer.dylib extracted to temporary file /var/folders/d3/38f0syys5yjcvynz8p4n3t4w0000gn/T/jni_file_2410736199844535966.dll

### ...