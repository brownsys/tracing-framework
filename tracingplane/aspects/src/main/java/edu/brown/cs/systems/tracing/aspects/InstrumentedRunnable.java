package edu.brown.cs.systems.tracing.aspects;

public interface InstrumentedRunnable {
    public void rememberConstructorContext();

    public void rememberRunendContext();

    public void rejoinConstructorContext();

    public void joinRunendContext();
    
    public void cancelContexts();
}
