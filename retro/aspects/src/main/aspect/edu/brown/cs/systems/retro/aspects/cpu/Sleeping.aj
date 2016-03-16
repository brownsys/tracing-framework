package edu.brown.cs.systems.retro.aspects.cpu;

import java.nio.channels.Selector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;

import edu.brown.cs.systems.retro.resources.Execution;

public aspect Sleeping {
  
  public pointcut AllMethods(): call(* Thread+.sleep(..)) ||
                                call(* Object+.wait(..)) ||
                                call(* Condition+.await*(..)) ||
                                call(* Selector+.select(..)) ||
                                call(* Future+.get(..)) ||
                                call(* CompletionService+.poll(long,..)) ||
                                call(* CompletionService+.take(..)) ||
                                call(* BlockingQueue+.poll(long,..)) ||
                                call(* BlockingQueue+.take(..)) ||
                                call(* Thread+.join(..)) ||
                                call(* scala.concurrent.Awaitable+.ready(..)) ||
                                call(* scala.concurrent.Awaitable+.result(..)) ||
                                call(* scala.concurrent.Await+.ready(..)) ||
                                call(* scala.concurrent.Await+.result(..)) ||
                                call(* scala.concurrent.Await$+.ready(..)) ||
                                call(* scala.concurrent.Await$+.result(..));
  
  /**
   * Log whenever a thread sleeps or waits
   */
  Object around(): AllMethods() {
    Execution.Sleep.starting(thisJoinPointStaticPart);
    try {
      return proceed();
    } finally {
      Execution.Sleep.finished(thisJoinPointStaticPart);
    }    
  }

}
