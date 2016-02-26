package edu.brown.cs.systems.utils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;
import edu.brown.cs.systems.tracing.Utils;

public class XTraceGCUtils {

    /** The garbage collector beans of the system. Created during static init */
    public static final GarbageCollectorMXBean[] gcbeans;
    static {
        List<GarbageCollectorMXBean> beanlist = ManagementFactory.getGarbageCollectorMXBeans();
        gcbeans = beanlist.toArray(new GarbageCollectorMXBean[beanlist.size()]);
    }

    /**
     * Registers the provided listener to receive GC event callbacks
     * 
     * @param callback
     *            a callback to call when a GC event occurs
     */
    public static void registerGCListener(XTraceGCCallback callback) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        XTraceGCCallbackNotificationListener listener = new XTraceGCCallbackNotificationListener(callback);
        for (GarbageCollectorMXBean bean : gcbeans) {
            try {
                server.addNotificationListener(bean.getObjectName(), listener, null, bean);
            } catch (InstanceNotFoundException e) {
                System.out.println("GC notifications registerListener Warning: " + e.getMessage());
            }
        }
    }

    /**
     * Ask each GC bean for its elapsed collection time
     */
    public static long calculateElapsedGC() {
        long timeMillis = 0;
        for (GarbageCollectorMXBean gcbean : gcbeans) {
            timeMillis += gcbean.getCollectionTime();
        }
        return timeMillis;
    }

    private static final GCSpammer spammer = new GCSpammer();

    /**
     * Turn on or off a thread that spams calls to System.GC
     * 
     * @param enabled
     *            turn the thread on or off
     */
    public static void spam(boolean enabled) {
        if (enabled)
            spammer.start();
        else
            spammer.stop();
    }

    public static class GCSpammer {
        private volatile boolean started = false;
        private volatile boolean enabled = false;
        private final Thread t = new Thread() {
            public void run() {
                try {
                    while (!t.isInterrupted()) {
                        if (enabled) {
                            System.gc();
                        } else {
                            Thread.sleep(1000);
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        private GCSpammer() {
        }

        public static GCSpammer create() {
            return new GCSpammer();
        }

        public synchronized void start() {
            if (!started) {
                started = true;
                t.start();
            }
            enabled = true;
        }

        public synchronized void stop() {
            enabled = false;
        }

        public synchronized void destroy() {
            if (started) {
                t.interrupt();
            }
            started = true;
            enabled = false;
        }
    }

    /**
     * Try to force GC once by repeatedly calling System.gc() until our weak
     * reference disappears This could cause multiple GCs until our object is
     * actually collected
     */
    public static void force() {
        /*
         * http://stackoverflow.com/questions/1481178/forcing-garbage-collection-
         * in-java "The jlibs library has a good utility class for garbage
         * collection. You can force garbage collection using a nifty little
         * trick with WeakReference objects." "RuntimeUtil.gc() from the jlibs:"
         */

        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<Object>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }

    /**
     * Users can extend this subclass and register it to receive GC callbacks
     * 
     * @author a-jomace
     *
     */
    public static abstract class XTraceGCCallback {
        /**
         * A method that is called whenever GC occurs
         * 
         * @param gc
         *            information about the GC that occurred
         */
        public abstract void onGC(GcInfo gc);
    }

    /**
     * Wraps XTraceGCCallbacks to handle pulling out GC event info
     */
    private static class XTraceGCCallbackNotificationListener implements NotificationListener {
        private final XTraceGCCallback callback;

        public XTraceGCCallbackNotificationListener(XTraceGCCallback callback) {
            this.callback = callback;
        }

        private static GarbageCollectionNotificationInfo getInfo(Notification notification) {
            CompositeData cd = (CompositeData) notification.getUserData();
            return GarbageCollectionNotificationInfo.from(cd);
        }

        private static GcInfo getGcInfo(GarbageCollectionNotificationInfo info, GarbageCollectorMXBean bean) {
            if (bean instanceof com.sun.management.GarbageCollectorMXBean) {
                return ((com.sun.management.GarbageCollectorMXBean) bean).getLastGcInfo();
            }
            return info.getGcInfo();
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            try {
                // Make sure it's the right notification type
                if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
                    return;

                // Extract the GcInfo as appropriate, working around the OpenJDK
                // GcInfo cycles/milliseconds bug
                GarbageCollectorMXBean bean = (GarbageCollectorMXBean) handback;
                GarbageCollectionNotificationInfo info = getInfo(notification);
                GcInfo gc = getGcInfo(info, bean);

                // Call the callback
                this.callback.onGC(gc);
            } catch (Exception e) {
                // swallow
            }
        }
    }

    private static final String gctopic = "gcspam";
    private static boolean gcspamcallbackregistered = false;

    public static synchronized void registerGCSpamCallback() {
        if (!gcspamcallbackregistered) {
            gcspamcallbackregistered = true;
            PubSub.subscribe(gctopic, new Subscriber<StringMessage>() {
                private final String myHostName = Utils.getHost();
                private final String myProcName = Utils.getProcessName();
                private final String myIdEnable = String.format("1%s=%s", myHostName, myProcName);
                private final String myIdDisable = String.format("0%s=%s", myHostName, myProcName);
                private final String disableAll = "disableAll";

                @Override
                protected void OnMessage(StringMessage message) {
                    String s = message.hasMessage() ? message.getMessage() : "";
                    if (myIdEnable.equals(s)) {
                        System.out.println("Spamming GC: start");
                        XTraceGCUtils.spam(true);
                    } else if (myIdDisable.equals(s)) {
                        System.out.println("Spamming GC: end");
                        XTraceGCUtils.spam(false);
                    } else if (disableAll.equals(s)) {
                        System.out.println("Spamming GC: clear all");
                        XTraceGCUtils.spam(false);
                    }
                }
            });
        }
    }

    public static void publishGCSpamReset() {
        PubSub.publish(gctopic, StringMessage.newBuilder().setMessage("disableAll").build());
    }

    public static void publishGCSpamCommand(String hostname, String procname, boolean enabled) {
        String cmd = String.format("%d%s=%s", enabled ? 1 : 0, hostname, procname);
        PubSub.publish(gctopic, StringMessage.newBuilder().setMessage(cmd).build());
    }

    public static void main(String[] args) throws InterruptedException {
        registerGCSpamCallback();
        long start;
        while (true) {
            start = System.currentTimeMillis();
            publishGCSpamCommand(Utils.getHost(), Utils.getProcessName(), true);
            while (System.currentTimeMillis() - start < 5000) {
                System.out.println(XTraceGCUtils.calculateElapsedGC());
                Thread.sleep(100);
            }
            start = System.currentTimeMillis();
            publishGCSpamCommand(Utils.getHost(), Utils.getProcessName(), false);
            while (System.currentTimeMillis() - start < 5000) {
                System.out.println(XTraceGCUtils.calculateElapsedGC());
                Thread.sleep(100);
            }
        }
    }

}
