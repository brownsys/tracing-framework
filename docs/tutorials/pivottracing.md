# Getting Started: Pivot Tracing

Once HDFS is up and running, we can dynamically install some Pivot Tracing queries.  The `pivottracing/client` project provides some simple command line utilities that we will use.  The executables will be located in the `target/appassembler/bin` folder.  First, open up a terminal and run the `print-agent-status` executable.

	target/appassembler/bin/print-agent-status

This will ping all connected Pivot Tracing agents every 10 seconds and ask them for their current status.   You should see something like the following as output:

	2016-03-03 13:19:38 INFO  PubSubClient:110 - Subscribing topic PTstatus
	2016-03-03 13:19:38 INFO  PubSubClient:259 - Attempting connection to 127.0.0.1:5563 with 0 pending messages
	2016-03-03 13:19:38 INFO  PubSubClient:182 - Sending existing subscriptions
	agent {
	  procName: "NameNode"
	  procId: 7831
	  host: "localhost"
	}
	dynamicInstrumentationEnabled: true

	agent {
	  procName: "DataNode"
	  procId: 7873
	  host: "localhost"
	}
	dynamicInstrumentationEnabled: true

Now we will install an example query.  We are going to install the following Pivot Tracing query:

	From incr In DataNodeMetrics.incrBytesRead
	Join getloc In NN.GetBlockLocations On getloc -> incr
	GroupBy incr.host, getloc.src
	Select incr.host, getloc.src, SUM(incr.delta)

This query counts bytes read on HDFS DataNodes, and attributes them to source files.  Specifically, when a request passes through the GetBlockLocations method on the NameNode, Pivot Tracing will save the value of the `src` variable; then later, when the request passes through the `incrBytesRead` method on the DataNode, Pivot Tracing will associate the value of the `delta` variable with the previous `src` from the NameNode.

We will install this query by running the `example-hdfs-query` executable.

	target/appassembler/bin/example-hdfs-query


This will compile an query into instrumentation and publish to the Pivot Tracing agents.  They will dynamically modify some Java classes, reload them, and start outputting query results.  The example-hdfs-query executable will produce output like the following:

	Query: 

	From incr In DataNodeMetrics.incrBytesRead
	Join getloc In NN.GetBlockLocations On getloc -> incr
	GroupBy incr.host, getloc.src
	Select incr.host, getloc.src, SUM(incr.delta)

	============================================================
	Compiled query:

	QGETLOC = From NN.GetBlockLocations

	From incr In DataNodeMetrics.incrBytesRead
	Join getloc In QGETLOC On getloc -> incr
	GroupBy incr.host, getloc.src
	Select incr.host, getloc.src, SUM(incr.delta)

	============================================================
	Optimized query:

	QGETLOC = From NN.GetBlockLocations
	GroupBy src
	Select src

	From incr In DataNodeMetrics.incrBytesRead
	Join getloc In QGETLOC On getloc -> incr
	GroupBy incr.host, getloc.src
	Select incr.host, getloc.src, SUM(incr.delta)

	============================================================
	Query advice:

	A0 at NN.GetBlockLocations
	OBSERVE 0.src
	PACK BAG-0-0 0.src, 


	A1 at DataNodeMetrics.incrBytesRead
	OBSERVE 0.host, 1.delta
	UNPACK BAG-0-0 2.src, 
	EMIT Q-0 0.host, 2.src, SUM(1.delta)




	============================================================
	2016-03-03 14:21:08 INFO  PivotTracingClient:121 - Publishing command to PTcommands:
	update {
	  weave {
	    id: "\000\001\000\000"
	    advice {
	      observe {
	        var: "0.src"
	      }
	      pack {
	        bagId: "\000\001\000\000"
	        groupBySpec {
	          groupBy: "0.src"
	        }
	      }
	    }
	    tracepoint {
	      methodTracepoint {
	        className: "org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer"
	        methodName: "getBlockLocations"
	        paramClass: "java.lang.String"
	        paramClass: "long"
	        paramClass: "long"
	        where: RETURN
	        adviceArg {
	          literal: "src"
	        }
	      }
	    }
	  }
	  weave {
	    id: "\000\001\000\001"
	    advice {
	      observe {
	        var: "0.host"
	        var: "1.delta"
	      }
	      unpack {
	        bagId: "\000\001\000\000"
	        groupBySpec {
	          groupBy: "2.src"
	        }
	      }
	      emit {
	        outputId: "\000\001"
	        groupBySpec {
	          groupBy: "0.host"
	          groupBy: "2.src"
	          aggregate {
	            name: "1.delta"
	            how: SUM
	          }
	        }
	      }
	    }
	    tracepoint {
	      methodTracepoint {
	        className: "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics"
	        methodName: "incrBytesRead"
	        paramClass: "int"
	        where: ENTRY
	        adviceArg {
	          literal: "edu.brown.cs.systems.tracing.Utils.getHost()"
	        }
	        adviceArg {
	          literal: "new Integer(delta)"
	        }
	      }
	    }
	  }
	}

	2016-03-03 14:21:08 INFO  PubSubClient:259 - Attempting connection to 127.0.0.1:5563 with 0 pending messages
	Query sent to agents

In the terminal where you ran the `print-agent-status` executable, you will now see messages like the following:

	agent {
	  procName: "DataNode"
	  procId: 8448
	  host: "localhost"
	}
	dynamicInstrumentationEnabled: true
	woven {
	  id: "\000\001\000\000"
	  advice {
	    observe {
	      var: "0.src"
	    }
	    pack {
	      bagId: "\000\001\000\000"
	      groupBySpec {
	        groupBy: "0.src"
	      }
	    }
	  }
	  tracepoint {
	    methodTracepoint {
	      className: "org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer"
	      methodName: "getBlockLocations"
	      paramClass: "java.lang.String"
	      paramClass: "long"
	      paramClass: "long"
	      where: RETURN
	      adviceArg {
	        literal: "src"
	      }
	    }
	  }
	}
	woven {
	  id: "\000\001\000\001"
	  advice {
	    observe {
	      var: "0.host"
	      var: "1.delta"
	    }
	    unpack {
	      bagId: "\000\001\000\000"
	      groupBySpec {
	        groupBy: "2.src"
	      }
	    }
	    emit {
	      outputId: "\000\001"
	      groupBySpec {
	        groupBy: "0.host"
	        groupBy: "2.src"
	        aggregate {
	          name: "1.delta"
	          how: SUM
	        }
	      }
	    }
	  }
	  tracepoint {
	    methodTracepoint {
	      className: "org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics"
	      methodName: "incrBytesRead"
	      paramClass: "int"
	      where: ENTRY
	      adviceArg {
	        literal: "edu.brown.cs.systems.tracing.Utils.getHost()"
	      }
	      adviceArg {
	        literal: "new Integer(delta)"
	      }
	    }
	  }
	}

If any exceptions occurred in attempting to install the instrumentation, they will be included in the message here.  They will also show up in the HDFS output logs.  The following is an example error message (from a different query):

	problem {
	  name: "javassist.NotFoundException"
	  message: "edu.brown.cs.systems.hdfsworkloadgenerator.Client"
	  stacktrace: "javassist.NotFoundException: edu.brown.cs.systems.hdfsworkloadgenerator.Client\n\tat javassist.ClassPool.get(ClassPool.java:450)\n\tat edu.brown.cs.systems.pivottracing.dynamicinstrumentation.MethodRewriteModification.getMethod(MethodRewriteModification.java:62)\n\tat edu.brown.cs.systems.pivottracing.dynamicinstrumentation.MethodRewriteModification.getMethod(MethodRewriteModification.java:56)\n\tat edu.brown.cs.systems.pivottracing.dynamicinstrumentation.MethodRewriteModification.apply(MethodRewriteModification.java:43)\n\tat edu.brown.cs.systems.dynamicinstrumentation.Agent$Installation.modify(Agent.java:126)\n\tat edu.brown.cs.systems.dynamicinstrumentation.Agent$Installation.modifyAll(Agent.java:139)\n\tat edu.brown.cs.systems.dynamicinstrumentation.Agent.install(Agent.java:78)\n\tat edu.brown.cs.systems.dynamicinstrumentation.DynamicManager.install(DynamicManager.java:77)\n\tat edu.brown.cs.systems.pivottracing.agent.WeaveManager.install(WeaveManager.java:74)\n\tat edu.brown.cs.systems.pivottracing.agent.PTAgent.install(PTAgent.java:101)\n\tat edu.brown.cs.systems.pivottracing.agent.PTAgent$PubSubCommandSubscriber.OnMessage(PTAgent.java:141)\n\tat edu.brown.cs.systems.pivottracing.agent.PTAgent$PubSubCommandSubscriber.OnMessage(PTAgent.java:134)\n\tat edu.brown.cs.systems.pubsub.PubSubClient$Subscriber.OnMessage(PubSubClient.java:343)\n\tat edu.brown.cs.systems.pubsub.PubSubClient.OnMessage(PubSubClient.java:130)\n\tat edu.brown.cs.systems.pubsub.PubSubClient.access$000(PubSubClient.java:34)\n\tat edu.brown.cs.systems.pubsub.PubSubClient$ClientReader.OnMessage(PubSubClient.java:146)\n\tat edu.brown.cs.systems.pubsub.io.TopicReader.OnMessage(TopicReader.java:28)\n\tat edu.brown.cs.systems.pubsub.io.MessageReader.read(MessageReader.java:76)\n\tat edu.brown.cs.systems.pubsub.PubSubClient.ClientThreadMainLoop(PubSubClient.java:243)\n\tat edu.brown.cs.systems.pubsub.PubSubClient.ClientThreadRun(PubSubClient.java:264)\n\tat edu.brown.cs.systems.pubsub.PubSubClient.run(PubSubClient.java:285)\n"
	}


Also, if necessary, you can uninstall queries using the `uninstall-all-queries` executable.  For now, the `example-hdfs-query` executable should successfully install.

We will now run some HDFS requests and observe the query results.
In a new terminal, run the `print-query-results` utility.

	target/appassembler/bin/print-query-results

This will subscribe to the query results topic and print any query results sent by any Pivot Tracing agent.  Since HDFS is currently idle, you will only see the following::

	2016-03-03 13:51:09 INFO  PubSubClient:110 - Subscribing topic PTresults
	2016-03-03 13:51:09 INFO  PubSubClient:259 - Attempting connection to 127.0.0.1:5563 with 0 pending messages
	2016-03-03 13:51:09 INFO  PubSubClient:182 - Sending existing subscriptions

Now we will use the HDFS command line utility to copy a file into HDFS then read it out of HDFS.  The HDFS command line utility, `hdfs`, will be located in `hadoop-dist/target/hadoop-2.7.2/bin` inside the Hadoop git repository.  Run the following command:

	echo "Hello World" >> example.txt
	hdfs dfs -copyFromLocal example.txt /example.txt
	hdfs dfs -copyToLocal /example.txt example2.txt

This will create a file containing the text "Hello World", copy it into HDFS, then copy it out of HDFS.  The print-query-results executable will print something like the following:

	emit {
	  outputId: "\000\001"
	  groupBySpec {
	    groupBy: "0.host"
	    groupBy: "2.src"
	    aggregate {
	      name: "1.delta"
	      how: SUM
	    }
	  }
	}
	agent {
	  procName: "DataNode"
	  procId: 8448
	  host: "localhost"
	}
	timestamp: 1457033092668
	group {
	  groupBy: "localhost"
	  groupBy: "/example.txt"
	  aggregation: 16
	}



