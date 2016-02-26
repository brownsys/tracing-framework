package edu.brown.cs.systems.retro.aspects.disk;

import java.io.FileDescriptor;

import edu.brown.cs.systems.retro.resources.DiskResource;


public aspect XFileDescriptor {

  /** Matches any calls made to FileDescriptor.sync() from user code */
  void around(FileDescriptor f): target(f) && call(void FileDescriptor.sync()) {
    DiskResource.Sync.starting(f, thisJoinPointStaticPart);
    try {
        proceed(f);
    } finally {
        DiskResource.Sync.finished(f, thisJoinPointStaticPart);
    }
  }

}
