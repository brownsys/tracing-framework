package edu.brown.cs.systems.retro.aspects.cpu;

import java.nio.channels.Selector;
import java.util.concurrent.locks.Condition;

import edu.brown.cs.systems.retro.resources.Execution;

public aspect Sleeping {
  
  public pointcut AllMethods(): call(* Thread+.sleep(..)) ||
                                call(* Object+.wait(..)) ||
                                call(* Condition+.await*(..)) ||
                                call(* Selector+.select(..));
  
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
