package edu.brown.cs.systems.retro.aspects.locks;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.resources.MonitorLock;

/**
 * Java monitor locks and reentrant locks can allow multiple acquires and releases,
 * so these utility methods keep track of acquires
 * 
 * The generic lock instrumentation is only used to profile the specific locks
 * that should be instrumented, because Java contains many, many locks
 * 
 * @author a-jomace
 */
public aspect LockInstrumentation {

  public pointcut canTrace(): if(true);

  public static void logRequest(Object o, JoinPoint.StaticPart jp) {
    if (Transaction.Request(o))
      MonitorLock.acquiring(o, jp);
  }

  public static void logAcquire(Object o, JoinPoint.StaticPart jp) {
    if (Transaction.Acquire(o)) {
      Transaction t = Transaction.get(o);
      MonitorLock.acquired(o, t.request, t.acquire, jp);
    }
  }

  public static void preWait(Object o, JoinPoint.StaticPart jp) {
    if (Transaction.Wait(o)) {
      Transaction t = Transaction.get(o);
      MonitorLock.released(o, t.request, t.acquire, t.release, jp);
    }
  }

  public static void postWait(Object o, JoinPoint.StaticPart jp) {
    if (Transaction.Waited(o)) {
      Transaction t = Transaction.get(o);
      MonitorLock.acquiring(o, jp);
      MonitorLock.acquired(o, t.request, t.acquire, jp);
    }
  }

  public static void logRelease(Object o, JoinPoint.StaticPart jp) {
    if (Transaction.Release(o)) {
      Transaction t = Transaction.remove(o);
      MonitorLock.released(o, t.request, t.acquire, t.release, jp);
    }
  }

  private static class Transaction {
    private static ThreadLocal<Map<Object, Transaction>> perthread = new ThreadLocal<Map<Object, Transaction>>() {
      @Override
      public Map<Object, Transaction> initialValue() {
        return new HashMap<Object, Transaction>();
      }
    };
    public int count = 0;
    public long request = System.nanoTime();
    public long acquire = -1;
    public long release = -1;

    public static boolean Request(Object o) {
      Transaction t = get(o);
      boolean first = false;
      if (t == null) {
        t = new Transaction();
        first = true;
      }
      t.count++;
      put(o, t);
      return first;
    }

    public static boolean Acquire(Object o) {
      Transaction t = get(o);
      boolean first = false;
      if (t != null && t.acquire == -1 && t.count > 0) {
        t.acquire = System.nanoTime();
        first = true;
      }
      return first;
    }

    public static boolean Wait(Object o) {
      Transaction t = get(o);
      boolean valid = false;
      if (t != null && t.count > 0 && t.acquire != -1) {
        t.release = System.nanoTime();
        valid = true;
      }
      return valid;
    }

    public static boolean Waited(Object o) {
      Transaction t = get(o);
      boolean valid = false;
      if (t != null && t.count > 0 && t.acquire != -1) {
        t.request = System.nanoTime();
        t.acquire = t.request;
        valid = true;
      }
      return valid;
    }

    public static boolean Release(Object o) {
      Transaction t = get(o);
      boolean last = false;
      if (t != null) {
        t.count--;
        if (t.count == 0) {
          t.release = System.nanoTime();
          last = true;
        }
      }
      return last;
    }

    public static Transaction get(Object o) {
      return perthread.get().get(o);
    }

    public static void put(Object o, Transaction t) {
      perthread.get().put(o, t);
    }

    public static Transaction remove(Object o) {
      return perthread.get().remove(o);
    }
  }
}
