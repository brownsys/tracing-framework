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

### Configuring HDFS

The following instructions will configure a minimally working version of HDFS.  From the base directory for the Hadoop git repository, the built version of hadoop will be located in `hadoop-dist/target/hadoop-2.7.2`.  Within the build directory, `etc/hadoop` will contain the default config.  Copy this directory to somewhere outside of the HDFS build directory, otherwise any changes you make will be overwritten any time you build HDFS.  Edit `core-site.xml` to the following:

	<configuration>
	  <property>
	    <name>fs.default.name</name>
	    <value>hdfs://127.0.0.1:9000</value>
	  </property>
	</configuration>

Set the following two environment variables:

* `HADOOP_HOME` to the build directory, eg. `hadoop-dist/target/hadoop-2.7.2`
* `HADOOP_CONF_DIR` to the location of your copied HDFS config.

### Running PubSub

All of the instrumentation libraries use a pub sub system to communicate.  Agents running in the instrumented Hadoop will receive commands over pubsub, and send their output back over the pubsub.  The pubsub project is in `tracingplane/pubsub`

The following command will start an X-Trace server which also includes a pub sub server:

	tracingplane/pubsub/target/appassembler/bin/server

To check which Java process are running, use the `jps` command.  This is an easy way to check if something has crashed.  You should expect to see a process called `StandaloneServer`.

### Start HDFS

From the base directory for the Hadoop git repository, the built version of hadoop will be located in `hadoop-dist/target/hadoop-2.7.2`.  Within the build directory, `bin` contains various command line utilities, while `sbin` contains some useful scripts for starting and stopping processes.  Before starting HDFS, we must format its data dir:

	${HADOOP_HOME}/bin/hdfs --config $HADOOP_CONF_DIR namenode -format

Then start an HDFS NameNode and HDFS DataNode:

    ${HADOOP_HOME}/sbin/hadoop-daemon.sh start namenode
    ${HADOOP_HOME}/sbin/hadoop-daemon.sh start datanode

You should expect to see messages in the output stream along the lines of the following:

	Pivot Tracing initialized
	Resource reporting executor started
	ZmqReporter: QUEUE-disk -> queue
	ZmqReporter: DISK- -> disk
	ZmqReporter: CPU- -> cpu
	/META-INF/lib/libthreadcputimer.dylib extracted to temporary file /var/folders/d3/38f0syys5yjcvynz8p4n3t4w0000gn/T/jni_file_2410736199844535966.dll

Again, check which Java processes are running using the `jps` command.  In addition to `XTraceServer`, you should expect to see `NameNode` and `DataNode`
For reference, HDFS runs a Web UI by default at [localhost:50070](http://localhost:50070).

### Optional: Start HDFS with multiple DataNodes

A more interesting HDFS setup will have you running one NameNode process and multiple DataNode processes.  This can be done on the same machine, however, you must specify configurations for each datanode you start (otherwise they might try to use the same data directory, for example).  The following script will start multiple datanodes.  Change the `NUM_DATANODES` and `BASE_DATA_DIR` variables as appropriate.


    NUM_DATANODES=3;
    BASE_DATA_DIR=/Users/jon/deploy

    echo "===== Starting HDFS with $NUM_DATANODES datanodes =====";
    ${HADOOP_HOME}/sbin/hadoop-daemon.sh start namenode;

    for i in $(seq $NUM_DATANODES) 
    do
      export HADOOP_LOG_DIR=$BASE_DATA_DIR/logs/datanode_$i
      export HADOOP_PID_DIR=$BASE_DATA_DIR/pid/datanode_$i
      export HADOOP_OPTS="\
        -Dhadoop.tmp.dir=$BASE_DATA_DIR/data/datanode_$i \
        -Ddfs.datanode.address=0.0.0.0:5001$i \
        -Ddfs.datanode.http.address=0.0.0.0:5008$i \
        -Ddfs.datanode.ipc.address=0.0.0.0:5002$i"
      ${HADOOP_HOME}/sbin/hadoop-daemon.sh --script bin/hdfs start datanode $HADOOP_OPTS
    done