# Configuration

All of the tracing framework projects use the [Typesafe Config](https://github.com/typesafehub/config) for configuration.

Each project has a few configuration values you can set, defined in per-project `reference.conf` files located in the `src/main/resources` folder.  The full configuration is listed below.

To override a configuration value, create a file `application.conf` and place it somewhere on the classpath (for example, co-locate it with your usual Hadoop configuration files).  Anything specified in the application.conf file will override default configuration values.  One way you can do this with Hadoop is to set the `HADOOP_CLASSPATH` environment variable.

The two configuration options that are particularly relevant are:

 * `xstore.server.datastore.dir` specifies where the X-Trace server should persist the reports it receives (if you wish to use X-Trace).  By default, it will create a folder in the directory where X-Trace is started.  Consider setting this to something more permanent.  This configuration value is only necessary on the machine that runs the X-Trace server
 * `pubsub.server.hostname` specifies the hostname of the machine running the pub sub server.  All projects use pubsub for communication and must have the correct value for this.  By default, this is the local host, which is fine for a single-machine set up or just trying things out.

Most of the executables included in the tracing framework are built using the appassembler maven plugin.  The easiest way to override the configuration for these executables is to set the `CLASSPATH_PREFIX` variable to the folder containing your application.conf, eg `CLASSPATH_PREFIX=C:/xtraceconfig` or `CLASSPATH_PREFIX=/home/jon/xtraceconfig`.  (Note: do not add any trailing slashes, dots, or stars.  Java classpath is finnicky).  The appassembler plugin appends the value of this environment variable to its classpath.

The following are all configurations:

	// Dynamic Instrumentation Config
	dynamic-instrumentation {
		use_jdwp = false		// If true, will try to use JDWP dynamic instrumentation instead of agent lib.  JDWP is tentatively deprecated
	}

	// Pub Sub Config
	pubsub {
	  
	  server {
	  	hostname = "127.0.0.1"
	  	bindto = "0.0.0.0"
	  	port = 5563
	  }
	  
	  topics {
	  	control = "_"
	  }
	    
	}

	// Pivot Tracing Agent config
	pivot-tracing {
		agent = {
			use_baggage = true			// Regardless of whether the system is baggage enabled or not, setting to false will disable using it
			use_dynamic = true			// Should the agent use dynamic instrumentation?
			report_interval_ms = 1000   // Report every 1 second
		    emit_if_no_results = false	// If no output tuples, should we emit an empty message anyway
		}
	}

	// Pivot Tracing Common Config
	pivot-tracing {
		pubsub = {
		   server = ${pubsub.server}
		   commands_topic = "PTcommands"
		   results_topic = "PTresults"
		   status_topic = "PTstatus"
		}
	}

	// X-Trace Client Config
	xtrace {
		
		client {
			reporting {
				on					= true 		# is XTrace reporting globally enabled
				default				= true  	# for a logging class, it is on or off by default
				enabled				= [ "com.example.EnabledClass",		# list of agent names for whom logging is enabled,
											"randomEnabledAgentName" ]		#  overriding setting in xtrace.client.reporting.default
				disabled			= [ "com.example.DisabledClass",	# list of agent names for whom logging is disabled
											"randomDisabledAgentName" ]		#  overriding setting in xtrace.client.reporting.default
				discoverymode		= false		# discovery mode is super verbose. whenever a log statement is encountered, a task will be started.
			}
		}
		
	}

	// X-Trace Common Config
	xtrace {
		
		pubsub {
			topic = "xtrace"		# Topic on which X-Trace reports are published
		}
		
	}

	// X-Trace Server Config
	xtrace {
		
		server {
			bind-hostname = 0.0.0.0 # hostname to bind the webserver to
		
			database-update-interval-ms	= 1000
			
			webui {
				port					= 4080
			}
			
			datastore {
				dir						= "./xtrace-data"  	# location of xtrace storage
				buffer-size				= 65536  			# buffer size for each task writer
				cache-size				= 1000   			# number of file handles to cache for writing tasks
				cache-timeout			= 30000  			# cache eviction timeout for file handles
			}
		}
		
	}

	// Retro Reporting Config
	resource-reporting {

		# settings for the aggregation component of resource reporting.  consumption can be aggregated without reporting
		aggregation {
			active							= true					# aggregation is on by default
			disk-cache-threshold 			= 120000000 			# bytes per second threshold for a disk read to be logged as a disk cache read
			small-read						= 131072				# a small read is one that is <= 128kb
			seek-threshold					= 10000000				# a small read is a seek if it is slower than 10 ms (10,000,000 ns)
		
			# per-resource settings
			enabled {
				disk						= ${resource-reporting.aggregation.active}  # disk aggregation is enabled by default; can set to true or false
				disk-cache					= ${resource-reporting.aggregation.active}  # disk-cache aggregation is enabled by default; can set to true or false
				network						= ${resource-reporting.aggregation.active}  # network aggregation is enabled by default; can set to true or false
				cpu							= ${resource-reporting.aggregation.active}  # cpu aggregation is enabled by default; can set to true or false
				hdfs						= ${resource-reporting.aggregation.active}  # hdfs aggregation is enabled by default; can set to true or false
				locks						= ${resource-reporting.aggregation.active}  # locks aggregation is enabled by default; can set to true or false
				queue						= ${resource-reporting.aggregation.active}  # queue aggregation is enabled by default; can set to true or false
				throttlingpoint				= ${resource-reporting.aggregation.active}  # throttling point aggregation is enabled by default; can set to true or false
				batch						= ${resource-reporting.aggregation.active}  # batch aggregation is enabled by default; can set to true or false
			}
		}
		
		# settings for the reporting side.  reporting can be disabled or modified separately from aggregation
		reporting {
			active							= true					# reporting is on by default. default reporter is zmq
			interval						= 1000 					# reporting interval in milliseconds
			
			# reporting settings for the zmq reporter
			zmq {
				topics {
					default					= "default"				# default topic for reports if none configured
					immediate				= "immediate"			# topic for immediate reports to be published on
					disk					= "disk"				# topic on which to report disk usage reports
					disk-cache				= "diskcache"			# topic on which to report disk cache usage reports
					network					= "network"				# topic on which to report network usage reports
					cpu						= "cpu"					# topic on which to report cpu usage reports
					hdfs					= "hdfs"				# topic on which to report hdfs usage reports
					locks					= "locks"				# topic on which to report locks usage reports
					queue					= "queue"				# topic on which to report queue usage reports
					throttlingpoint			= "throttlingpoint"		# topic on which to report throttling point usage reports
					batch					= "batch"				# topic on which to report batch usage reports
				} 
			}
			
			# reporting settings for the file printer reporter
			printer {
				filename					= "hdfsreports.tsv"		# default filename for reports file
			}
		}
		
	}

	// Retro Resources Config
	resource-tracing {

		disk {
			sync-after-write 		= false		# set to true to sync to disk after every file write
			sync-threshold 			= 0			# number of bytes written before disk sync. only valid if sync-on-write is true.  if 0, will sync after every write
		}
		
		background {
			heartbeat				= -10		# tenant class for heartbeat background process; set to -1 to disable
			replication				= -11		# tenant class for replication background process; set to -1 to disable
			invalidate				= -12		# tenant class for deleting blocks from disk; set to -1 to disable
			finalize				= -13		# tenant class for finalizing blocks; set to -1 to disable
			recover					= -14 		# tenant class for recovering blocks; set to -1 to disable		
		}
		
		batch {
		    hbase {
		    	fshlog				= -20		# tenant class for hbase fshlog; set to -1 to disable
	    	}
		}
		
	}

	// Retro Throtting Config
	retro {
		throttling {
			topic = "throttlingupdates"					# topic on which the controller should publish throttling point rates
			schedulertopic = "schedulerupdates"			# topic on which the controller should publish scheduler rates
		
			default-throttlingpoint = "simple"			# type of throttling point to use.  valid choices: ["simple", "batched-<type>-<batchsize>", "default"]
			throttlingpoints {
				"point-example" = "batched-simple-5"	# configures the "point-example" throttling point to use the 'batched' throttling point type.
			}
			
			default-throttlingqueue = "locking"			# type of throttling queue to use.  valid choices: ["locking", "delay", "default"]
			throttlingqueues {
				"queue-example" = "delay"				# configures the "queue-example" throttling queue to use the "delay" throttling queue type.
			}
			
			default-scheduler = "mclock-3"				# default scheduler type to use. valid choices: ["mclock-<concurrency>", "default"]
			schedulers {
				"scheduler-example" = "mclock-5"		# configures the "schedulers-example" scheduler to use the "mclock" scheduler type with a concurrency of 5
			}
			
			debug {
				mclock = false							# set to true to print mclock debug messages
			}
		}
	}

	// Retro Visualization Server Config
	resource-reporting {
		visualization {
	  		webui-port = 4081
		}
	}