# Adding Baggage to your System #

## Introduction ##

This tutorial gives an overview of how to instrument your distributed system to add Baggage propagation.  The goal of this tutorial is to get an understanding of what Baggage is and isn't, how it works under the covers, and what effort is required on the part of system developers to add Baggage to their systems.

We will also show a few examples of patterns for Baggage propagation, and end with recommendations for how to tackle instrumentation for a new system.

#### Overview: End-to-End Tracing ####

First, consider an example, illustrated in the diagram below.  Here, we show a distributed database system, HBase (similar to Google's BigTable), and a distributed file system, HDFS (similar to the Google File System).  HBase runs web servers on many machines.  Each machine can receive remote procedure calls (RPCs) issued by clients (①).  Clients can, for example, read or write data to database tables.  Incoming requests will be queued up (②) and eventually processed by one of a set of worker threads (③).  In order to process the request, HBase must read or write data from a database table, which is actually stored in the distributed file system.  HBase issues an RPC request to HDFS (④); like HBase, incoming requests to HDFS are queued up (⑤) and eventually processed by a set of worker threads (⑥).  Upon completion, HDFS responds to HBase (⑦); HBase processes the response (⑧) and then sends its own response back to the client (⑨, ⑩).

![Baggage Flow](../images/baggage_flow.png)

When we talk about the *end-to-end execution of a request*, we mean the full path of the request starting from the client, going through HBase and HDFS, and returning back to the client.

#### Baggage ####

The goal of Baggage is to provide a per-request container of key-value pairs that follows a request's execution.  The goal is to allow any component along the path of the request to add key-value pairs to the baggage; those key-value pairs will then be propagated alongside the request wherever it goes in future.  For example, when the request executes in HBase, we might want to add the key-value pair "User": "Jon" to the request's baggage.  If so, then we want this key-value pair to subsequently be accessible when the request arrives at HDFS; when it returns to HBase after processing at HDFS completes; and even at the client when HBase returns its final response.

Baggage propagation is challenging because we do not know how to easily automate it.  It requires developers to go in to the source code of the system and make changes to support Baggage.  For example, the RPC call from the client to HBase, and in the RPC call from HBase to HDFS, the wire format of the call must be modified to now include Baggage, and the source code of the servers must be modified to serialize and deserialize baggage.  In the thread pools and request queues in HBase and HDFS, baggage must be included in the request context that is queued up.

Our implementation of Baggage aims to make it as easy as possible for developers to add instrumentation to their system.  This tutorial goes over some common patterns and how to instrument them with the Baggage API.

## 1. Active Baggage ##

For now, we assume that at any point in time, a thread is only doing work for one request.  For example, the worker threads in the HDFS and HBase illustration each only process one request at a time.  However, multiple threads could be processing different requests concurrently, and a single thread might process many requests one-by-one during its lifetime.

Each thread has an *active baggage* stored in a thread-local variable which corresponds to the baggage for the request currently being executed by that thread.  Baggage is implicitly present and empty by default; no initialization is required to start interacting with the current thread's baggage.  The Baggage interaction API (see [Baggage](../../tracingplane/client)) provides the means to add, remove, and manipulate the key-value pairs stored in a thread's active baggage.

**Discarding Baggage** When execution for a request completes, the active baggage for the thread might need to be discarded.  For example, a worker thread might implement a simple loop that dequeues from a queue of request contexts.  In this scenario, the active baggage must be *discarded* (①) when each request completes execution:

```
 class MyThreadPool {
	BlockingQueue<RequestContext> requestQueue;

	class RequestContext {
		...
	}

	public void enqueue(RequestContext request) {
		requestQueue.add(request);
	}

	public void workerThreadMain() {
		while (!Thread.currentThread().isInterrupted()) {
			RequestContext currentRequest = requestQueue.take();
			this.processRequest(currentRequest);
+			Baggage.discard();     // ①: Baggage is discarded when request finishes processing
		}
	}
 }
```

## 2. Passing Baggage Between Threads ##

Requests rarely have single-threaded execution; instead, many systems use multiple threads, queues, thread pools, callbacks, and other asynchronous execution mechanisms.  For all of these, Baggage must be correctly propagated alongside the request.

**Attaching Baggage to Contexts** Often, threads enqueue request contexts (or similarly, callbacks) into queues to be executed.  When this happens, Baggage must be included with the request context, so that the worker thread can resume the request's baggage.  In the following example code, we add a `DetachedBaggage` field to the RequestContext class (②), we save the baggage with requests when they are enqueued (③), and resume the baggage of a request when it is dequeued (④):

```
 class MyThreadPool {
	BlockingQueue<RequestContext> requestQueue;

	class RequestContext {
+		DetachedBaggage baggage;	// ②: add a field to store baggage with request context
		...
	}

	public void enqueue(RequestContext request) {
+		request.baggage = Baggage.stop();	// ③: save baggage when we enqueue a request
		requestQueue.add(request);
	}

	public void workerThreadMain() {
		while (!Thread.currentThread().isInterrupted()) {
			RequestContext currentRequest = requestQueue.take();
+			Baggage.start(currentRequest.baggage);	// ④: restore baggage when we resume the request
			this.processRequest(currentRequest);
			Baggage.discard();
		}
	}
 }
```

** Request Branching ** Sometimes, an execution splits into multiple concurrent branches.  For example, a request might enqueue *multiple* request contexts (⑤), such as the following:

```
 class FanOutRequest {

	public void processFanOutRequest() {
		...
		for (int i = 0; i < 10; i++) {
			threadPool.enqueue(new ChunkRequest());		// ⑤: enqueue multiple concurrent requests for concurrent processing
		}
		...
	}

 }
```
When an execution splits like this, it is **insufficient** to use `Baggage.stop()` in our enqueue method, because then only the first request we enqueue would have meaningful baggage.  Instead we must use `Baggage.fork()`, which will create a copy of the baggage.  Now, every request enqueued will have its own baggage:
```
 class MyThreadPool {
 	...

	public void enqueue(RequestContext request) {
+		request.baggage = Baggage.fork();	// ⑥: fork current baggage when we enqueue instead of stopping, because we might not be finished in the current thread
		requestQueue.add(request);
	}

	...
 }
```

** Creating New Threads** Another example of when we need to copy baggage is if a request creates a new thread.  When that happens, the first thread must make a copy of its current baggage (⑦) and save it in a field accessible to the second thread (⑧).  When the second thread starts execution, it activates the baggage given to it by the first thread (⑨):
```
class MyLongRequest {

	class SecondaryThread {
+		DetachedBaggage baggage;	// ⑧: second thread has field for storing creator's baggage

		@Override
		public void run() {
			Baggage.start(baggage);		// ⑨: second thread activates baggage when it begins executing.
			...
		}
	}
	
	public void processRequest() {
		...

+		SecondaryThread second = new SecondaryThread();
+		second.baggage = Baggage.fork();	// ⑦: first thread copies its current baggage
+		second.start();
		...
	}
}
```

## 3. Merging Baggage ##

After request has branched into multiple concurrent executions, it often later merges back into a single execution, for example, when a thread joins.  When this happens, each branch of the joining execution will have its own baggage instance.  After joining, we expect the baggage contents to be merged.  For example, in the previous example, if the primary thread subsequently calls `Thread.join()`, then we want the baggage from the secondary thread to be merged back with the primary thread (⑩).  In order to do this, the secondary thread must also save its final baggage in a field accessible to the primary thread (⑪).  After the primary thread's call to Thread.join() returns, it must then merge the baggage (⑫).

```
class MyLongRequest {

	class SecondaryThread {
		DetachedBaggage baggage;
+		DetachedBaggage endBaggage;		// ⑪: after completion, the secondary thread stores its baggage in this field

		@Override
		public void run() {
			Baggage.start(baggage);
			...
+			endBaggage = Baggage.stop();	// ⑩: remember final baggage for joining back with primary thread
		}
	}
	
	public void processRequest() {
		...

		SecondaryThread second = new SecondaryThread();
		second.baggage = Baggage.fork();
		second.start();
		...
		...
+		second.join();
+		Baggage.join(second.endBaggage);	// ⑫: merge the secondary thread's baggage back in
	}
}
```

## 4. Serializing Baggage ##

When communication over the network occurs, it is usually necessary to propagate baggage over the network too.  DetachedBaggage has serialization methods to convert baggage into bytes for inclusion in the network.  When the network call is made, the baggage should be also sent over the network.  For example, if we respond to an RPC call with a message `MyRpcResponse`, we might choose to first write the baggage to the wire before then writing the response.  To do this, we would have to fork the current baggage (⑬) then write it to the wire (⑭).  We would also have to modify the corresponding code that receives the RpcResponse to read the baggage bytes (⑮) and join the baggage (⑯).

```
class NetworkUtils {
	
	public void sendResponse(SocketOutputStream out, MyRpcResponse response) {
+		byte[] baggageBytes = Baggage.fork().toByteArray();		// ⑬: fork and serialize baggage
+		out.write(baggageBytes.length);
+		out.write(baggageBytes);	// ⑭: write length-prefixed baggage to wire

		byte[] responseBytes = response.serialize();
		out.write(responseBytes.length);
		out.write(responseBytes);
	}

	public MyRpcResponse recvResponse(SocketInputStream in) {
+		int baggageLength = in.read();
+		byte[] baggageBytes = IOUtils.readFully(baggageLength);		// ⑮: receive length-prefixed baggage from wire
+		Baggage.join(baggageBytes);		// ⑯: join the received baggage with the current baggage

		int responseLength = in.read();
		byte[] responseBytes = IOUtils.readFully(responseLength);
		return MyRpcResponse.parseFrom(responseBytes);
	}	
}

}
```

## 5. Protocol Buffers ##

Many systems use Google's protocol buffers for reading and writing network messages.  One way to add baggage to network calls is to extend protocol buffers messages to add a field for Baggage bytes.  For example, in HDFS we extended the protocol buffers header message to optionally include baggage (⑰).  We also modified the appropriate places in the code where messages were [created](https://github.com/brownsys/hadoop/blob/7984801e05587a62e9033579abeb4cbe73c72c39/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/ProtoUtil.java#L182) for writing to the network (⑱) and [deserialized](https://github.com/brownsys/hadoop/blob/7984801e05587a62e9033579abeb4cbe73c72c39/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java#L298) after receiving from the network (⑲):

```
RpcHeader.proto:
	message RpcRequestHeaderProto {
		optional RpcKindProto rpcKind = 1;
		optional OperationProto rpcOp = 2;
		...
+		optional bytes baggage = 7; 	// ⑰: field to include baggage bytes
	}


ProtoUtil.java:
	public class ProtoUtil {
		public static RpcRequestHeaderProto makeRpcRequestHeader(RpcKind rpcKind, OperationProto operation, ...) {
		    RpcRequestHeaderProto.Builder result = RpcRequestHeaderProto.newBuilder();
		    result.setRpcKind(convert(rpcKind))
		    result.setRpcOp(operation);
		    ...
+		    result.setBaggage(Baggage.fork().toByteString());		// ⑱: include current baggage in RPC request

		    return result.build();
		  }
	}


Client.java:
	public class Client {

		void checkResponse(RpcResponseHeaderProto header) throws IOException {
		    if (header == null) {
		      throw new EOFException("Response is null.");
		    }
		    
+		    Baggage.start(header.getBaggage());		// ⑲: resume baggage included in RPC response
		}

	}
```

## 6. Automatic Instrumentation ##

Instrumentation can often be done automatically if source code matches some common patterns.  For example, copying baggage to a new thread as outlined above is an obvious pattern to capture.  Patterns like this can be captured automatically using source-code rewriting tools like AspectJ.  We have included aspects that automatically instrument several common patterns.  In the [Auto-Baggage](../../tracingplane/aspects) section, we give more details on how to use these aspects.  The aspects capture the following patterns:

### Automatically modified classes ###

**Runnable** Adds a `baggage` field to all Runnables.  When the runnable is created, the current thread's baggage is forked and saved in the baggage field.  When `run()` is called, the runnable's baggage is swapped with the active baggage.  When run ends, the runnable's baggage is swapped back to the baggage field, and the previously-active baggage is put back.

**Callable** Adds a `callable` field to all Callables.  When the callable is created, the current thread's baggage is forked and saved in the baggage field.  When `call()` is called, the callable's baggage is swapped with the active baggage.  When call ends, the callable's baggage is swapped back to the baggage field, and the previously-active baggage is put back.

**Thread** Adds a 'baggage' field to all Thread instances.  When the thread is created, the current thread's baggage is forked and saved in the baggage field.  When `run()` is called, the thread's baggage is started as the active baggage.  When run ends, the thread's active baggage is saved back into the baggage field.  After any other thread calls `join()` on the thread, it will also join the final baggage of the thread.

**ExecutorService** Wraps returned futures so that when `Future.get()` is called, it also joins the final baggage of the Runnable or Callable that was submitted to the ExecutorService.

**CompletionService** Wraps returned futures so that when `Future.get()` is called, it also joins the final baggage of the Runnable or Callable that was submitted to the CompletionService. 

**BlockingQueue** When elements are added to the queue via `add`, `offer`, or `put`, the enqueueing thread's baggage will be forked.  When the element is dequeued via `take`, the dequeueing thread will join the element's baggage.  Unlike the other automatically modified classes, `BlockingQueue` is not instrumented by default -- it must be enabled by using the `@InstrumentQueues` and `@InstrumentedQueueElement` annotations as outlined below.

### Classes modified with annotations ###

**@BaggageInheritanceDisabled** All Runnables, Callables, and Threads are instrumented by default.  This might not be desirable for some instances.  Adding the `@BaggageInheritanceDisabled` annotation to select classes will prevent this behavior for those classes.

**@InstrumentedQueueElement** Any class can be given the `@InstrumentedQueueElement` annotation.  This will add a 'baggage' field to the class.

**@InstrumentQueues** Any class can be given the `@InstrumentQueues` annotation.  This will enable instances of `BlockingQueue` within the class to be instrumented with queue instrumentation.  Queues are not automatically instrumented unless they exist in a class that has this annotation.





