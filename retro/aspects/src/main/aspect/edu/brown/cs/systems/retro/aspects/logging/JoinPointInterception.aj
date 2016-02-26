package edu.brown.cs.systems.retro.aspects.logging;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.Flushable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.QueueResource;

/**
 * Intercepts any methods on known wrapper classes to simply save the join point, 
 * so that the proxy classes can accurately report where their call came from
 * 
 * @author Jonathan Mace
 */
public aspect JoinPointInterception {
  
  public pointcut Methods():
  (
      call(* Closeable+.close*(..)) ||
      call(* InputStream+.read*(..)) ||
      call(* OutputStream+.write*(..)) ||
      call(* Flushable+.flush*(..)) ||
      call(* RandomAccessFile+.read*(..)) ||
      call(* RandomAccessFile+.write*(..)) ||
      call(* DataInput+.read*(..)) ||
      call(* DataOutput+.write*(..)) ||
      call(* FilterInputStream+.read*(..)) ||
      call(* FilterOutputStream+.write*(..)) ||
      call(* QueueResource.*(..)) ||
      call(* ReadLock+.*(..)) ||
      call(* WriteLock+.*(..)) || 
      call(* *.getChannel(..)) ||
      call(* FileChannel+.*(..)) ||
      call(* Thread+.start(..))
      );
  
  before(): Methods() {
    JoinPointTracking.Caller.set(thisJoinPointStaticPart);
  }
  
  after(): Methods() {
    JoinPointTracking.Caller.clear();
  }

}
