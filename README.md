# Brown University Tracing Framework

Welcome to the Brown University Tracing Framework repository.  This repository contains multiple projects from the Brown University systems research group, such as X-Trace, Retro, and Pivot Tracing.

Full documentation is available at https://brownsys.github.io/tracing-framework

#### Getting Started

Head over to the [tutorials](http://brownsys.github.io/tracing-framework/docs/tutorials.html) section to begin!  

#### Projects Overview

The tracing framework comprises four main projects:

**Tracing Plane** is the underlying instrumentation library that the other projects are built on.  The Tracing Plane provides a generic end-to-end metadata propagation primitive called *Baggage*, that lets you dynamically propagate key-value pairs along the execution path of a request.  Baggage provides a primitive similar to thread local variables, but instead at the granularity of a request, as it traverses process, machine, and application boundaries.

**Pivot Tracing** is a dynamic monitoring framework for distributed systems.  Users can write high-level monitoring queries that are compiled down into instrumentation and automatically installed into live, running systems.  Pivot Tracing uses the Tracing Plane's Baggage primitive for correlating statistics from multiple places in the system.

**X-Trace** is a distributed causal logging framework.  Instead of logging to per-process and per-machine files, X-Trace collects logged messages centrally.  X-Trace uses the Tracing Plane's Baggage primitive to pass identifiers through the system, enabling causally related logging statements to be tied together.  X-Trace can automatically hook in to existing log4j/commons loggers.

**Retro** is a fine-grained resource consumption, attribution, and actuation library.  Retro instruments both system- and application-level resources, such as disk, network, cpu, locks, and queues.  Retro aggregates consumption statistics, broken down according to the high-level tenant consuming the resouce.  Retro provides hooks for throttling the requests of individual tenants.  Retro reports measurements globally, enabling centralized resource management policies that react to aggressive resource consumption by throttling the responsible tenants.  Retro uses the Tracing Plane's Baggage primitive to propagate tenant identifiers.

#### Instrumented Systems

These projects are all instrumentation libraries.  They are intended for use with a new or existing system.  We also have some pre-instrumented forks of some well known systems:
* [HDFS 2.7.2](https://github.com/brownsys/hadoop/tree/brownsys-pivottracing-2.7.2)
* Apache YARN 2.7.2 (*coming soon*)
* Hadoop MapReduce 2.7.2 (*coming soon*)
* HBase (*coming soon*)

[Note: we are currently porting instrumentation for these systems to the latest versions for release.  Check back soon]

*Note: This documentation is a work in progress.  For questions, please use the [pivot-tracing-users](https://groups.google.com/forum/#!forum/pivot-tracing-users) Google group.*