package edu.brown.cs.systems.retro.aspects.locks.hdfs;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.brown.cs.systems.retro.wrappers.ReentrantReadWriteLockWrapper;

/**
 * Targets specific HDFS locks of interest
 * 
 * @author a-jomace
 * 
 */
public aspect XHDFS {
  
  /** Matches FSNamesystem and FSDirectory RWlocks */
  Object around(): within(org.apache.hadoop.hdfs.server.namenode.FSNamesystem) && call(ReentrantReadWriteLock.new()) {
    return new ReentrantReadWriteLockWrapper(thisJoinPointStaticPart);
  }

  /** Matches FSNamesystem and FSDirectory RWlocks */
  Object around(boolean b): args(b) && within(org.apache.hadoop.hdfs.server.namenode.FSNamesystem) && call(ReentrantReadWriteLock.new(boolean)) {
    return new ReentrantReadWriteLockWrapper(b, thisJoinPointStaticPart);
  }

//  /** Matches any FSEditLog synchronized locking */
//  before(Object o): args(o) && within(org.apache.hadoop.hdfs.server.namenode.FSEditLog+) && lock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logRequest(o, thisJoinPointStaticPart);
//  }
//
//  after(Object o): args(o) && within(org.apache.hadoop.hdfs.server.namenode.FSEditLog+) && lock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logAcquire(o, thisJoinPointStaticPart);
//  }
//
//  after(Object o): args(o) && within(org.apache.hadoop.hdfs.server.namenode.FSEditLog+) && unlock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logRelease(o, thisJoinPointStaticPart);
//  }
//
//  void around(Object o): target(o) && within(org.apache.hadoop.hdfs.server.namenode.FSEditLog+) && call(void Object+.wait(..)) && LockInstrumentation.canTrace() {
//    LockInstrumentation.preWait(o, thisJoinPointStaticPart);
//    try {
//      proceed(o);
//    } finally {
//      LockInstrumentation.postWait(o, thisJoinPointStaticPart);
//    }
//  }
//
//  /** Matches any DatanodeDescriptor synchronized locking */
//  before(Object o): args(o) && within(org.apache.hadoop.hdfs.protocol.DatanodeInfo+) && lock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logRequest(o, thisJoinPointStaticPart);
//  }
//
//  after(Object o): args(o) && within(org.apache.hadoop.hdfs.protocol.DatanodeInfo+) && lock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logAcquire(o, thisJoinPointStaticPart);
//  }
//
//  after(Object o): args(o) && within(org.apache.hadoop.hdfs.protocol.DatanodeInfo+) && unlock() && LockInstrumentation.canTrace() {
//    LockInstrumentation.logRelease(o, thisJoinPointStaticPart);
//  }
//
//  void around(Object o): target(o) && within(org.apache.hadoop.hdfs.protocol.DatanodeInfo+) && call(void Object+.wait(..)) && LockInstrumentation.canTrace() {
//    LockInstrumentation.preWait(o, thisJoinPointStaticPart);
//    try {
//      proceed(o);
//    } finally {
//      LockInstrumentation.postWait(o, thisJoinPointStaticPart);
//    }
//  }

}
