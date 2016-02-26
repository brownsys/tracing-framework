package edu.brown.cs.systems.retro.aspects.disk;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import edu.brown.cs.systems.retro.resources.DiskResource;
import edu.brown.cs.systems.retro.wrappers.FileOutputStreamWrapper;


public aspect XFileOutputStream {
  
  /** Matches any calls made to new FileOutputStream(File) from user code. */
  Object around(File f) throws FileNotFoundException: args(f) && call(FileOutputStream.new(File)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new FileOutputStreamWrapper(f, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }

  /** Matches any calls made to new FileOutputStream(File, boolean) from user code. */
  Object around(File f, boolean b) throws FileNotFoundException: args(f, b) && call(FileOutputStream.new(File, boolean)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new FileOutputStreamWrapper(f, b, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }

  /** Matches any calls made to new FileOutputStream(FileDescriptor) from user code. */
  Object around(FileDescriptor fd): args(fd) && call(FileOutputStream.new(FileDescriptor)) {
    return new FileOutputStreamWrapper(fd, thisJoinPointStaticPart);
  }

  /** Matches any calls made to new FileOutputStream() from user code. */
  Object around(String s) throws FileNotFoundException: args(s) && call(FileOutputStream.new(String)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new FileOutputStreamWrapper(s, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }

  /** Matches any calls made to new FileOutputStream() from user code. */
  Object around(String s, boolean b) throws FileNotFoundException: args(s, b) && call(FileOutputStream.new(String, boolean)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    Object ret = null;
    try {
       return ret = new FileOutputStreamWrapper(s, b, thisJoinPointStaticPart);
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }

}
