## TracingPlane - Automatic Tracing Instrumentation

This project provides some AspectJ aspects to automatically propagate Baggage across thread and runnable boundaries.

### InstrumentedRunnable

Runnables will be modified as follows:

1. If a thread creates a runnable, the thread's current baggage will be forked at the time the runnable is created, and stored in a field inside the runnable.
2. When the run method of the runnable is invoked, the baggage that was stored in the runnable will be attached to the current thread.
3. When the run method of the runnable completes, the thread's current baggage will be detached and stored in a field inside the runnable
4. After the runnable has run, a caller can cast the runnable to an `InstrumentedRunnable` and call `joinRunendContext()` to merge the baggage stored in the runnable back into the current thread's baggage.

### InstrumentedThread

Threads will be modified as follows:

1. When a new Thread object is created, its arguments are checked to see whether the thread is being handed an `InstrumentedRunnable` (and the thread itself is also checked to see whether it is an InstrumentedRunnable).  If so, an InstrumentedThread is created in place of an ordinary thread.
2. When Thread.join is called, the thread is checked to see whether it is an InstrumentedThread.  If so, the `joinRunendContext()` of the instrumented thread is called after `Thread.join` returns.

### BaggageInheritanceDisabled

If you want to use the automatic instrumentation, but want to disable it for a select few Runnables or Threads, the `@BaggageInheritanceDisabled` annotation will prevent the runnable or thread from being instrumented.