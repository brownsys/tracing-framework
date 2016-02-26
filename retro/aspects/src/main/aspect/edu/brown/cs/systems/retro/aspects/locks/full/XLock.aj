package edu.brown.cs.systems.retro.aspects.locks.full;

import java.util.concurrent.locks.Lock;

import edu.brown.cs.systems.retro.aspects.locks.LockInstrumentation;

/**
 * Instruments subclasses of the java.util.concurrent.locks.Lock interface
 * @author a-jomace
 */
public aspect XLock {
  
  /** Matches any calls made to Lock.unlock() from user code */
  before(Lock l): target(l) && target(Lock+) && call(* Lock+.lock(..)) && LockInstrumentation.canTrace() {
    LockInstrumentation.logRequest(l, thisJoinPointStaticPart);
  }

  /** Matches any calls made to Lock.unlock() from user code */
  after(Lock l): target(l) && target(Lock+) && call(* Lock+.lock(..)) && LockInstrumentation.canTrace() {
    LockInstrumentation.logAcquire(l, thisJoinPointStaticPart);
  }

  /** Matches any calls made to Lock.unlock() from user code */
  before(Lock l): target(l) && target(Lock+) && call(* Lock+.unlock(..)) && LockInstrumentation.canTrace() {
    LockInstrumentation.logRelease(l, thisJoinPointStaticPart);
  }

}
