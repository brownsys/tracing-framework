package edu.brown.cs.systems.retro.aspects.disk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import edu.brown.cs.systems.retro.resources.DiskResource;
import edu.brown.cs.systems.retro.wrappers.FileChannelWrapper;
import edu.brown.cs.systems.retro.wrappers.RandomAccessFileWrapper;

/**
 * RandomAccessFile is handled by a wrapper class, RandomAccessFileProxy
 * 
 * @author a-jomace
 */
public aspect XRandomAccessFile {
  
  Object around(String a, String b) throws FileNotFoundException: args(a, b) && call(RandomAccessFile+.new(String, String)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new RandomAccessFileWrapper(a, b, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }
  
  Object around(File f, String a) throws FileNotFoundException: args(f, a) && call(RandomAccessFile+.new(File, String)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new RandomAccessFileWrapper(f, a, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }
  
  FileChannel around(RandomAccessFile f): target(f) && call(FileChannel RandomAccessFile+.getChannel()) {
    return new FileChannelWrapper(f, proceed(f), thisJoinPointStaticPart);
  }
}
