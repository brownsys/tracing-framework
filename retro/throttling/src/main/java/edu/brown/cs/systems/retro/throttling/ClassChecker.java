package edu.brown.cs.systems.retro.throttling;

import java.lang.reflect.Field;

public class ClassChecker {

    private static final class Nothing {
    }

    private Class<?> get(String clzName) {
        try {
            return Class.forName(clzName);
        } catch (ClassNotFoundException e) {
            return Nothing.class;
        }
    }

    private final Class<?> call;
    private final Class<?> callrunner;

    private final Field call_enqueue;
    private final Field callrunner_enqueue;

    public ClassChecker() {
        call = get("org.apache.hadoop.ipc.Server.Call");
        callrunner = get("org.apache.hadoop.hbase.ipc.CallRunner");

        Field f1 = null;
        try {
            f1 = call.getDeclaredField("enqueue");
        } catch (Exception e) {
            f1 = null;
        } finally {
            call_enqueue = f1;
        }

        Field f2 = null;
        try {
            f2 = callrunner.getDeclaredField("enqueue");
        } catch (Exception e) {
            f2 = null;
        } finally {
            callrunner_enqueue = f1;
        }
    }

    public boolean isHadoopIPCCall(Object o) {
        return call.isInstance(o);
    }

    public boolean isHBaseCallRunner(Object o) {
        return callrunner.isInstance(o);
    }

    public void setCallEnqueue(Object o, long enqueue) {
        if (call_enqueue != null) {
            try {
                call_enqueue.setLong(o, enqueue);
            } catch (Exception e) {
                System.out.println("WARNING: call hack screwed up");
            }
        }
    }

    public void setCallRunnerEnqueue(Object o, long enqueue) {
        if (callrunner_enqueue != null) {
            try {
                callrunner_enqueue.setLong(o, enqueue);
            } catch (Exception e) {
                System.out.println("WARNING: callrunner hack screwed up");
            }
        }
    }

}
