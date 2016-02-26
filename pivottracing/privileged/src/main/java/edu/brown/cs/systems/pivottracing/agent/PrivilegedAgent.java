package edu.brown.cs.systems.pivottracing.agent;

/** PT Agent interface.  Privileged so that privileged classes can invoke advice */
public interface PrivilegedAgent {
    
    public void Advise(int adviceId, Object[] observed);

}
