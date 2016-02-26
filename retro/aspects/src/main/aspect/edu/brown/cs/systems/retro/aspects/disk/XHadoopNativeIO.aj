package edu.brown.cs.systems.retro.aspects.disk;

import java.io.FileDescriptor;

import edu.brown.cs.systems.retro.resources.DiskResource;

public aspect XHadoopNativeIO {
  
  FileDescriptor around(): call(FileDescriptor org.apache.hadoop.io.nativeio.NativeIO.Windows.createFile(..)) {
    DiskResource.Open.starting(null, thisJoinPointStaticPart);
    FileDescriptor ret = null;
    try {
       return ret = proceed();
    } finally {
      DiskResource.Open.finished(ret, thisJoinPointStaticPart);
    }
  }
}
