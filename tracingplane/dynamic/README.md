## TracingPlane - Dynamic Instrumentation

This project contains code for doing dynamic instrumentation from inside a Java process.  It is used by Pivot Tracing to rewrite classes in response to queries from users.

The dynamic instrumentation is implemented in two different ways:

* `JVMAgent` -- the JVMAgent is the default dynamic instrumentation agent.  It works without any additional command line flags or parameters.  However, it's possible that it doesn't work for older JVMs or on some platforms.  The JVM agent reloads classes by attaching a java agent at runtime to the process.
* `JDWPAgent` -- the JDWPAgent uses Javassist's `HotSwapper` feature and relies on the JVM running in debug mode.  To run the JVM in debug mode, you must add the following command line parameter: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0`.  By default, the library attempts to connect using a JVMAgent.  To prefer using a JDWPAgent, set the `dynamic-instrumentation.use_jdwp` configuration value to `true`.

The main parts of interest in this project are:

### DynamicInstrumentation

The static API call `DynamicInstrumentation.get()` returns a `DynamicManager` instance for the process, based on the configured values and whether it's successfully able to connect.  It will log warning messages if it is unable to set up an agent, but will still return a DynamicManager, it just won't do anything.

### DynamicModification

The `DynamicModification` interface is how clients can specify changes to a class.  A client can implement the interface, then call `DynamicManager.add` to add the modification.  A dynamic modification must implement two methods:

* `affects()` the modification must return a collection with the fully qualified names of all Java classes that the modification affects.
* `apply(ClassPool pool)` provides a [Javassist class pool](https://jboss-javassist.github.io/javassist/html/javassist/ClassPool.html) for it to make modifications to whatever classes it needs to.  For an example, see the `MethodRewriteModification` in the `pivottracing/agent` project.

A modification must make sure that it returns all class names it will modify in the `affects()` method, otherwise the modifications will not be made to the classes left out.

### DynamicManager

The `DynamicManager` class manages all installed modifications and applies dynamic instrumentation as requested by clients.  It keeps track of all the modifications made so far, and makes sure that any time a class is reloaded, all registered modifications will be applied.  This approach enables multiple applications to rewrite and reload classes without conflicting with each other or accidentally reverting another client's dynamic modifications.

* `add`, `addAll`, `remove`, and `removeAll` enable clients to add and remove modifications to classes.  The methods won't actually be rewritten or reloaded until the `install()` method is called.  If any of the pending modifications are invalid, exceptions will be thrown.
* `refresh(String className)` will signal the DynamicManager to reapply modifications to a class the next time `install()` is called.
* `reset(String className)` will remove all modifications to a class, reloading the original class definition the next time `install()` is called.
* `cancelPending()` will remove any pending modifications since the previous call to `install()`
* `clear()` will remove all modifications, reloading all original class definitions the next time `install()` is called.

### Tests

Most of the tests for dynamic instrumentation are for now located in the `pivottracing/agent` project.


### Config

Dynamic Instrumentation has the following default configuration values:

	dynamic-instrumentation {
		use_jdwp = false		// If true, will try to use JDWP dynamic instrumentation instead of agent lib.  JDWP is tentatively deprecated
	}