package edu.brown.cs.systems.retro.aspects.disk;

import java.io.File;

import edu.brown.cs.systems.retro.resources.DiskResource;

/**
 * Matches all file methods that block on the disk
 * @author a-jomace
 *
 */
public aspect XFile {
  
  Object around(File f): target(f) && (call(* File+.delete(..)) ||
                                      call(* File+.exists(..)) ||
                                      call(* File+.is*(..)) ||
                                      call(* File+.length(..)) ||
                                      call(* File+.list*(..)) ||
                                      call(* File+.mkdir*(..)) ||
                                      call(* File+.set*(..)) ||
                                      call(* File+.get*(..)) ||
                                      call(* File+.rename*(..))) {
    DiskResource.FileOp.starting(f, thisJoinPointStaticPart);
    try {
      return proceed(f);
    } finally {
      DiskResource.FileOp.finished(f, thisJoinPointStaticPart);
    }
  }
}
