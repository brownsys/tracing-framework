package edu.brown.cs.systems.retro.aspects.network;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import edu.brown.cs.systems.retro.resources.Network;

/**
 * Have yet to figure out how to nicely proxy socket channels, for now this messy approach will have to do
 * 
 * @author a-jomace
 */
public aspect XSocketChannel {
  
  /** Matches any calls made to SocketChannel.connect() from user code */
  boolean around(SocketChannel c): target(c) && target(SocketChannel+) &&
         (call(boolean SocketChannel+.connect(..)) || 
          call(public static SocketChannel SocketChannel+.open(SocketAddress+)) ||
          call(boolean SocketChannel+.finishConnect())) {
    Network.Connect.starting(c, thisJoinPointStaticPart);
    try {
      return proceed(c);
    } finally {
      Network.Connect.finished(c, thisJoinPointStaticPart);
    }
  }
  
  /** Matches any calls made to SocketChannel.read from user code */
  /** Advise the abstract read(ByteBuffer) method */
  int around(SocketChannel c, ByteBuffer dst): target(c) && target(SocketChannel+) && args(dst) && call(int ReadableByteChannel+.read(ByteBuffer)) {
    Network Read = Network.Read(c);
    Read.starting(c, thisJoinPointStaticPart);
    int numRead = 0;
    try {
      return numRead = proceed(c, dst);
    } finally {
      Read.finished(c, numRead, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract read(ByteBuffer[]) method */
  long around(SocketChannel c, ByteBuffer[] dsts): target(c) && target(SocketChannel+) && args(dsts) && call(long ScatteringByteChannel+.read(ByteBuffer[])) {
    Network Read = Network.Read(c);
    Read.starting(c, thisJoinPointStaticPart);
    long numRead = 0;
    try {
      return numRead = proceed(c, dsts);
    } finally {
      Read.finished(c, numRead, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract read(ByteBuffer[], int, int) method */
  long around(SocketChannel c, ByteBuffer[] dsts, int off, int len): target(c) && target(SocketChannel+) && args(dsts, off, len) && call(long ScatteringByteChannel+.read(ByteBuffer[], int, int)) {
    Network Read = Network.Read(c);
    Read.starting(c, thisJoinPointStaticPart);
    long numRead = 0;
    try {
      return numRead = proceed(c, dsts, off, len);
    } finally {
      Read.finished(c, numRead, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract write(ByteBuffer) method */
  int around(SocketChannel c, ByteBuffer src): target(c) && target(SocketChannel+) && args(src) && call(int WritableByteChannel+.write(ByteBuffer)) {
    Network Write = Network.Write(c);
    Write.starting(c, thisJoinPointStaticPart);
    int numWritten = 0;
    try {
      return numWritten = proceed(c, src);
    } finally {
      Write.finished(c, numWritten, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract write(ByteBuffer[]) method */
  long around(SocketChannel c, ByteBuffer[] srcs): target(c) && target(SocketChannel+) && args(srcs) && call(long GatheringByteChannel+.write(ByteBuffer[])) {
    Network Write = Network.Write(c);
    Write.starting(c, thisJoinPointStaticPart);
    long numWritten = 0;
    try {
      return numWritten = proceed(c, srcs);
    } finally {
      Write.finished(c, numWritten, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract write(ByteBuffer[], offset, length) method */
  long around(SocketChannel c, ByteBuffer[] srcs, int off, int len): target(c) && target(SocketChannel+) && args(srcs, off, len) && call(long GatheringByteChannel+.write(ByteBuffer[], int, int)) {
    Network Write = Network.Write(c);
    Write.starting(c, thisJoinPointStaticPart);
    long numWritten = 0;
    try {
      return numWritten = proceed(c, srcs, off, len);
    } finally {
      Write.finished(c, numWritten, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract transferFrom(ReadableByteChannel, long, long) method */
  long around(FileChannel c, SocketChannel src, long pos, long count): target(c) && target(SocketChannel+) && args(src, pos, count) && call(long SocketChannel+.transferFrom(ReadableByteChannel, long, long)) {
    Network Read = Network.Read(src);
    Read.starting(c, thisJoinPointStaticPart);
    long numRead = 0;
    try {
      return numRead = proceed(c, src, pos, count);
    } finally {
      Read.finished(c, numRead, thisJoinPointStaticPart);
    }
  }
  
  /** Advise the abstract transferTo(long, long, WritableByteChannel) method */
  long around(FileChannel c, long pos, long count, SocketChannel target): target(c) && target(SocketChannel+) && args(pos, count, target) && call(long SocketChannel+.transferTo(long, long, WritableByteChannel)) {
    Network Write = Network.Write(target);
    Write.starting(c, thisJoinPointStaticPart);
    long numWritten = 0;
    try {
      return numWritten = proceed(c, pos, count, target);
    } finally {
      Write.finished(c, numWritten, thisJoinPointStaticPart);
    }
  }
  
}
