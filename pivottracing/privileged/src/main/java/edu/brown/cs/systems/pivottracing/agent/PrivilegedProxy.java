package edu.brown.cs.systems.pivottracing.agent;

/** A proxy to the Pivot Tracing agent so that privileged code can invoke PT advice.
 * Class must be added to the boot class path in order to work */
public class PrivilegedProxy {
    
    private static PrivilegedAgent agent = null;
    
    /** Register the pivot tracing agent */
    public static void Register(PrivilegedAgent agent) {
        PrivilegedProxy.agent = agent;
    }
    
    /** Proxied call to Advise in the Pivot Tracing agent */
    public static void Advise(int adviceId, Object[] observed) {
        if (agent != null) {
            agent.Advise(adviceId, observed);
        }
    }
    
    public static Object box(Object o) {
        return o;
    }
    
    public static Object box(boolean b) {
        return new Boolean(b);
    }
    
    public static Object box(byte b) {
        return new Byte(b);
    }
    
    public static Object box(char c) {
        return new Character(c);
    }
    
    public static Object box(short s) {
        return new Short(s);
    }
    
    public static Object box(int i) {
        return new Integer(i);
    }
    
    public static Object box(long l) {
        return new Long(l);
    }
    
    public static Object box(float f) {
        return new Float(f);
    }
    
    public static Object box(double d) {
        return new Double(d);
    }

}
