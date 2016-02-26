package edu.brown.cs.systems.retro.perf.utils;

import java.io.File;

import edu.brown.cs.systems.baggage.DetachedBaggage;

public interface AspectJPerfAPI {

    public void setMetadata(byte[] ctx);

    public void setMetadata(DetachedBaggage ctx);

    public void joinMetadata(byte[] ctx);

    public void joinMetadata(DetachedBaggage ctx);

    public void unsetMetadata();

    public boolean fileExists(File f);

}
