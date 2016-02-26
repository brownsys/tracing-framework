package edu.brown.cs.systems.retro.perf.utils;

import java.io.File;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;

public class Instrumented implements AspectJPerfAPI {

    public void setMetadata(byte[] ctx) {
        Baggage.start(ctx);
    }

    public void setMetadata(DetachedBaggage ctx) {
        Baggage.start(ctx);
    }

    public void joinMetadata(byte[] ctx) {
        Baggage.join(ctx);
    }

    public void joinMetadata(DetachedBaggage ctx) {
        Baggage.join(ctx);
    }

    public void unsetMetadata() {
        Baggage.stop();
    }

    public boolean fileExists(File f) {
        return f.exists();
    }

}
