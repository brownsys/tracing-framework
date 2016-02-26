package edu.brown.cs.systems.retro.aspects.locks.full;

import edu.brown.cs.systems.retro.aspects.locks.LockInstrumentation;

/**
 * Instruments subclasses of the java.util.concurrent.locks.Lock interface
 * 
 * Needs XjoinPoints synchronization put into the pom
 * 
 * @author a-jomace
 */
public aspect XSynchronization {
  
  /** Matches any synchronized block */
  before(Object o): args(o) && lock() {
    LockInstrumentation.logRequest(o, thisJoinPointStaticPart);
  }
  
  /** Matches any synchronized block */
  after(Object o): args(o) && lock() {
    LockInstrumentation.logAcquire(o, thisJoinPointStaticPart);
  }
  
  /** Matches any synchronized block */
  before(Object o): args(o) && unlock() {
    LockInstrumentation.logRelease(o, thisJoinPointStaticPart);
  }

  void around(Object o): target(o) && call(void Object+.wait(..)) && LockInstrumentation.canTrace() {
    LockInstrumentation.preWait(o, thisJoinPointStaticPart);
    try {
      proceed(o);
    } finally {
      LockInstrumentation.postWait(o, thisJoinPointStaticPart);
    }
  }

}
