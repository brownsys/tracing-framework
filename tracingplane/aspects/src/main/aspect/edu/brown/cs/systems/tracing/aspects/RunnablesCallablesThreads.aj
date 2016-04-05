package edu.brown.cs.systems.tracing.aspects;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;

import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;

/** Instruments potentially asynchronous executions -- runnables, callables, and threads. Special-case handling for
 * threads since they can themselves be a runnable, or they can wrap a runnable */
public aspect RunnablesCallablesThreads {

    /* ================================================================================================================
     * 
     * InstrumentedExecution is added to anything like runnables or callables that want added instrumentation */

    public interface InstrumentedExecution {
        RunContext observeExecutionRunContext();
        public RunContext getOrCreateExecutionRunContext();
        public void setInstrumentedExecutionRunContext(RunContext context);
    }

    public volatile RunContext InstrumentedExecution.instrumentedExecutionRunContext;

    public RunContext InstrumentedExecution.observeExecutionRunContext() {
        return instrumentedExecutionRunContext;
    }

    public RunContext InstrumentedExecution.getOrCreateExecutionRunContext() {
        if (instrumentedExecutionRunContext == null) {
            instrumentedExecutionRunContext = new RunContext();
        }
        return instrumentedExecutionRunContext;
    }

    public void InstrumentedExecution.setInstrumentedExecutionRunContext(RunContext context) {
        instrumentedExecutionRunContext = context;
    }

    /* ================================================================================================================
     * 
     * Runnables and callables should be instrumented */

    declare parents: (!@BaggageInheritanceDisabled Runnable)+ implements InstrumentedExecution;
    declare parents: (!@BaggageInheritanceDisabled Callable)+ implements InstrumentedExecution;

    /* ================================================================================================================
     * 
     * Wrap calls to create just a thread (not subclasses) and wrap the thread so that we can access the runnable. This
     * is necessary because upon completion, the implementation of thread discards its contained runnable, so it cannot
     * be accessed by reflection at that point. */

    Object around(Runnable target): args(target) && call(Thread.new(Runnable+)) {
        return new WrappedThread(target);
    }

    Object around(Runnable target, String name): args(target, name) && call(Thread.new(Runnable+, String+)) {
        return new WrappedThread(target, name);
    }

    Object around(ThreadGroup group, Runnable target): args(group, target) && call(Thread.new(ThreadGroup+, Runnable+)) {
        return new WrappedThread(group, target);
    }

    Object around(ThreadGroup group, Runnable target, String name): args(group, target, name) && call(Thread.new(ThreadGroup+, Runnable+, String+)) {
        return new WrappedThread(group, target, name);
    }

    Object around(ThreadGroup group, Runnable target, String name, long stackSize): args(group, target, name, stackSize) && call(Thread.new(ThreadGroup+, Runnable+, String+, long)) {
        return new WrappedThread(group, target, name, stackSize);
    }

    /* ================================================================================================================
     * 
     * If a thread contains an instrumented runnable, it should inherit the execution context from that runnable rather
     * than creating its own. This must occur lexically before the general constructor instrumentation so that AspectJ
     * applies it fist */

    after() returning(Thread t) : call(Thread+.new(..)) {
        // System.out.println("Create new thread: " + thisJoinPoint);
        if (t instanceof InstrumentedExecution) {
            Runnable target = TracingplaneAspectUtils.getRunnable(t);
            if (target != null && target instanceof InstrumentedExecution) {
                InstrumentedExecution it = (InstrumentedExecution) t;
                InstrumentedExecution itarget = (InstrumentedExecution) target;
                it.setInstrumentedExecutionRunContext(itarget.getOrCreateExecutionRunContext());
            }
        }
    }

    /* ================================================================================================================
     * 
     * Standard basic instrumentation: save context in constructor, restore in run/call method, save at end of run/call
     * method */

    after() returning(InstrumentedExecution r): call(Runnable+.new(..)) || call(Callable+.new(..)) {
        // System.out.println("Create new runnable: " + thisJoinPoint);
        r.getOrCreateExecutionRunContext().ForkCurrentBaggageIfNecessary(thisJoinPointStaticPart);
    }

    before(InstrumentedExecution r): this(r) && (execution(void Runnable+.run(..)) || execution(* Callable+.call(..))) {
        r.getOrCreateExecutionRunContext().BeginExecution(thisJoinPointStaticPart);
    }

    after(InstrumentedExecution r): this(r) && (execution(void Runnable+.run(..)) || execution(* Callable+.call(..))) {
        r.getOrCreateExecutionRunContext().EndExecution(thisJoinPointStaticPart);
    }

    /* ================================================================================================================
     * 
     * Special case: shutdown hooks are ignored */

    before(Runnable r): call(* *.addShutdownHook(Runnable+,..)) && args(r,..) {
        if (r instanceof InstrumentedExecution) {
            ((InstrumentedExecution) r).getOrCreateExecutionRunContext().Discard();
        } else if (r instanceof Thread) {
            Runnable target = TracingplaneAspectUtils.getRunnable((Thread) r);
            if (target instanceof InstrumentedExecution) {
                ((InstrumentedExecution) target).getOrCreateExecutionRunContext().Discard();
            }
        }
    }

    /* ================================================================================================================
     * 
     * Threads: once a thread completes, we want to join with its execution context. */

    after(Thread t): target(t) && call(* Thread+.join(..)) {
        if (!t.isAlive() && t instanceof InstrumentedExecution) {
            ((InstrumentedExecution) t).getOrCreateExecutionRunContext().JoinSavedBaggageIfPossible(thisJoinPointStaticPart);
        }
    }

    /* ================================================================================================================
     * 
     * Instrument executor services to wrap the returned futures */

    Future around(InstrumentedExecution task): args(task,..) && 
                       (call(Future ExecutorService+.submit(Callable+)) ||
                        call(Future ExecutorService+.submit(Runnable+,..))) {
        return new WrappedFuture(proceed(task), task.getOrCreateExecutionRunContext());
    }

    // Not at all the best way to do this, but hooking in to completion service futures requires a surprisingly large amount of plumbing
    private static final Map<Future, WrappedFuture> futureLookupMap = new WeakHashMap<Future, WrappedFuture>();
    
    Future around(InstrumentedExecution task): args(task,..) && 
                       (call(Future CompletionService+.submit(Callable+)) ||
                        call(Future CompletionService+.submit(Runnable+,..))) {
        Future future = proceed(task);
        WrappedFuture wrapped = new WrappedFuture(future, task.getOrCreateExecutionRunContext());
        synchronized(futureLookupMap) {
            futureLookupMap.put(future, wrapped);
        }
        return wrapped;
    }
    
    // Inspect the future to find the underlying callable or runnable, and wrap it
    Future around(): call(Future CompletionService+.take()) {
        Future future = proceed();
        WrappedFuture wrapped;
        synchronized(futureLookupMap) {
            wrapped = futureLookupMap.remove(future);
        }
        return wrapped == null ? future : wrapped;
    }
    
    void around(WrappedFuture future, InstrumentedExecution r, Executor e): target(future) && args(r, e) && call(void ListenableFuture+.addListener(Runnable+, Executor+)) {
        r.setInstrumentedExecutionRunContext(future.instrumented);
        ((ListenableFuture) future.instrumented).addListener((Runnable) r, e);
    }

    /* ================================================================================================================
     * 
     * Futures: once a future completes and we get the return value, we want to join with its execution context */

    after(WrappedFuture f): target(f) && call(* Future+.get(..)) {
        if (f != null && f.instrumented != null) {
            f.instrumented.JoinSavedBaggageIfPossible(thisJoinPointStaticPart);
        }
    }

}