package edu.brown.cs.systems.retro.aspects.disk;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import edu.brown.cs.systems.retro.resources.DiskResource;
import edu.brown.cs.systems.retro.wrappers.FileInputStreamWrapper;

public aspect XFileInputStream {
  
  /** Matches any calls made to new FileInputStream(String) from user code. */
  Object around(String s) throws FileNotFoundException: args(s) && call(FileInputStream.new(String)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object file = null;
    try {
      return file = new FileInputStreamWrapper(s, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(file, thisJoinPointStaticPart);
    }
  }

  /** Matches any calls made to new FileInputStream(File) from user code. */
  Object around(File f) throws FileNotFoundException: args(f) && call(FileInputStream.new(File)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object file = null;
    try {
      return file = new FileInputStreamWrapper(f, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(file, thisJoinPointStaticPart);
    }
  }

  /** Matches any calls made to new FileInputStream(FileDescriptor) from user code. */
  Object around(FileDescriptor fd): args(fd) && call(FileInputStream.new(FileDescriptor+)) {
    return new FileInputStreamWrapper(fd, thisJoinPointStaticPart);
  }

}
