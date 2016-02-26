package edu.brown.cs.systems.retro.logging;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Random;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/**
 * Kicks off background tasks to record GC. This isn't inserted using a
 * pointcut, instead it is statically intialized by the XTraceResourceTracing
 * class.
 * 
 * @author jon
 */
public class GarbageCollection implements NotificationListener {

    private static final XTraceLogger logger = XTrace.getLogger(GarbageCollection.class);
    private static final long JVMStartTimeMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

    private final long taskid = new Random().nextLong();

    public static void register() {
        // Create an instance to receive events
        NotificationListener listener = new GarbageCollection();

        // Get the MBean server
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        // Get the collectors
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) {
            // Add the listener to the garbage collector
            try {
                server.addNotificationListener(bean.getObjectName(), listener, null, bean);
            } catch (InstanceNotFoundException e) {
                // ignore
            }
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        // Make sure it's the right notification type
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
            return;

        // Extract the GcInfo as appropriate, working around the OpenJDK GcInfo
        // cycles/milliseconds bug
        GarbageCollectorMXBean bean = (GarbageCollectorMXBean) handback;
        GarbageCollectionNotificationInfo info = getInfo(notification);
        GcInfo gc = getGcInfo(info, bean);

        // Extract the fields we want to include in the X-Trace report
        String startTime = Long.toString(JVMStartTimeMillis + gc.getStartTime());
        String duration = Long.toString(gc.getDuration());
        String action = info.getGcAction();
        String cause = info.getGcCause();
        String name = info.getGcName();

        XTraceReport report = XTraceReport.create();
        report.addStandardFields();
        report.builder.setTaskId(taskid);
        report.applyDecorators();
        report.setMessage("Garbage Collection Event");
        report.builder.addKey("Operation").addValue("GC");
        report.builder.addKey("GcStart").addValue(startTime);
        report.builder.addKey("GcDuration").addValue(duration);
        report.builder.addKey("GcAction").addValue(action);
        report.builder.addKey("GcCause").addValue(cause);
        report.builder.addKey("GcName").addValue(name);
        report.builder.addTags("GarbageCollection");
        report.builder.addTags(Utils.getProcessName());
        XTrace.getDefaultReporter().send(report);
    }

    private GarbageCollectionNotificationInfo getInfo(Notification notification) {
        CompositeData cd = (CompositeData) notification.getUserData();
        return GarbageCollectionNotificationInfo.from(cd);
    }

    private GcInfo getGcInfo(GarbageCollectionNotificationInfo info, GarbageCollectorMXBean bean) {
        if (bean instanceof com.sun.management.GarbageCollectorMXBean) {
            return ((com.sun.management.GarbageCollectorMXBean) bean).getLastGcInfo();
        }
        return info.getGcInfo();
    }

}
