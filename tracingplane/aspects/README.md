# TracingPlane - Automatic Tracing Instrumentation

## Overview ##

Instrumentation can often be done automatically if source code matches some common patterns.  For example, copying baggage to a new thread as outlined above is an obvious pattern to capture.  Patterns like this can be captured automatically using source-code rewriting tools like AspectJ.  This project provides some AspectJ aspects to automatically propagate Baggage across thread and runnable boundaries.  Modified classes are listed below.

### Getting Started ###

To use the automatic instrumentation aspects, you must use AspectJ's compiler to *weave* these aspects into your system's source code.  This will modify all the places in your system's source code that matches the patterns, and will add our code in those places.  You can add the following to your project's pom.xml to invoke AspectJ when you build:

	<dependencies>
        <dependency>
            <groupId>edu.brown.cs.systems</groupId>
            <artifactId>tracingplane-aspects</artifactId>
            <version>4.0</version>
        </dependency>
    </dependencies>

    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>aspectj-maven-plugin</artifactId>
        <version>1.5</version>
        <configuration>
            <aspectLibraries>
                <aspectLibrary>
                    <groupId>edu.brown.cs.systems</groupId>
                    <artifactId>tracingplane-aspects</artifactId>
                </aspectLibrary>
            </aspectLibraries>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>compile</goal>
                </goals>
                <configuration>
                    <source>1.7</source>
                    <target>1.7</target>
                    <complianceLevel>1.7</complianceLevel>
                    <Xjoinpoints>synchronization</Xjoinpoints>
                    <showWeaveInfo>false</showWeaveInfo>
                    <verbose>false</verbose>
                </configuration>
            </execution>
        </executions>
    </plugin>

In addition to the automatic instrumentation aspects, other projects also supply aspects that can be automatically added to your system, including X-Trace, Retro, and Pivot Tracing.  See the example [pom.xml](../../docs/pom.xml) for the full list of dependencies and aspect libraries.

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
