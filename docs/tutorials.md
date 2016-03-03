## Tutorials

This section contains the following tutorials:

**Pre-requisites** ([link](tutorials/prerequisites.html)) This optional section steps over installing pre-requisites.  If you already have these installed, you may skip this.
The tracing framework requires Java 7, [Maven 3](https://maven.apache.org/download.cgi), [Protocol Buffers 2.5](https://github.com/google/protobuf/releases/tag/v2.5.0), [AspectJ](https://eclipse.org/aspectj/downloads.php).

**Getting Started: HDFS** ([link](tutorials/gettingstarted.html)) In this tutorial, we will download and install a modified version of HDFS 2.7.2 that has already been instrumented to add Baggage.

**Getting Started: Pivot Tracing** ([link](tutorials/pivottracing.html)) In this tutorial we will dynamically install a Pivot Tracing query into HDFS as it is running.

**Adding Baggage To Your System** ([link](tutorials/baggage.html)) In this tutorial we will discuss how to add Baggage to a new system (Baggage is a pre-requisite to X-Trace, Retro, and Pivot Tracing).  We will discuss some of the automatic instrumentation that makes it easy to add Baggage.

**Using Baggage Yourself** ([link](tutorials/tracingapplication.html)) In this tutorial we will discuss how you can use Baggage in an already-instrumented system.  We will discuss the BaggageContents API and give some examples of how to use it.