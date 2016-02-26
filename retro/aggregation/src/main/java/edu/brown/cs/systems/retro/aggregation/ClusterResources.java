package edu.brown.cs.systems.retro.aggregation;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;
import edu.brown.cs.systems.retro.aggregation.reporters.PubSubReporter;

/**
 * The front end API to subscribe to to resource usage reports from clients
 */
public class ClusterResources {

    public static void subscribeToAll(Callback callback) {
        subscribeToCPUReports(callback);
        subscribeToDiskReports(callback);
        subscribeToDiskCacheReports(callback);
        subscribeToHDFSRequestReports(callback);
        subscribeToLockReports(callback);
        subscribeToNetworkReports(callback);
        subscribeToQueueReports(callback);
        subscribeToThrottlingPointReports(callback);
    }

    /** Subscribes to reports of cpu usage */
    public static void subscribeToCPUReports(Callback callback) {
        subscribe(Type.CPU, callback);
    }

    /** Subscribes to reports of disk usage */
    public static void subscribeToDiskReports(Callback callback) {
        subscribe(Type.DISK, callback);
    }

    /** Subscribes to reports of disk cache usage */
    public static void subscribeToDiskCacheReports(Callback callback) {
        subscribe(Type.DISKCACHE, callback);
    }

    /** Subscribes to reports of network usage */
    public static void subscribeToNetworkReports(Callback callback) {
        subscribe(Type.NETWORK, callback);
    }

    /** Subscribes to reports of lock resource usage */
    public static void subscribeToLockReports(Callback callback) {
        subscribe(Type.LOCKING, callback);
    }

    /** Subscribes to reports of completed HDFS requests */
    public static void subscribeToHDFSRequestReports(Callback callback) {
        subscribe(Type.HDFSREQUEST, callback);
    }

    /** Subscribes to reports of queue usage */
    public static void subscribeToQueueReports(Callback callback) {
        subscribe(Type.QUEUE, callback);
    }

    /** Subscribes to reports of throttling point usage */
    public static void subscribeToThrottlingPointReports(Callback callback) {
        subscribe(Type.THROTTLINGPOINT, callback);
    }

    /**
     * Subscribes to out-of-band reports that are published separately from
     * resource aggregation
     */
    public static void subscribeToImmediateReports(ImmediateReportCallback callback) {
        PubSub.subscribe(PubSubReporter.getImmediateReportTopic(ResourceReportingSettings.CONFIG), callback);
    }

    private static void subscribe(Resource.Type resource, Callback callback) {
        PubSub.subscribe(PubSubReporter.getTopic(ResourceReportingSettings.CONFIG, resource), callback);
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("No resources specified. Subscribing to all");
            System.out.println("Allowed resources: disk, network, locks, hdfs, cpu, queue");
            System.out
                    .println("Example usage: mvn exec:java -Dexec.mainClass=\"edu.brown.cs.systems.resourcereporting\" -Dexec.args=\"disk network locks hdfs\"");
        }

        // Create the callback function
        Callback callback = new Callback() {
            @Override
            protected void OnMessage(ResourceReport report) {
                System.out.println(report);
            }
        };

        // Create the immediate report callback function
        ImmediateReportCallback cb2 = new ImmediateReportCallback() {

            @Override
            protected void OnMessage(ImmediateReport message) {
                System.out.println(message);
            }
        };

        if (args.length == 0) {
            ClusterResources.subscribeToAll(callback);
            ClusterResources.subscribeToImmediateReports(cb2);
        }

        for (String arg : args) {
            if ("cpu".equals(arg))
                ClusterResources.subscribeToCPUReports(callback);
            else if ("disk".equals(arg))
                ClusterResources.subscribeToDiskReports(callback);
            else if ("network".equals(arg))
                ClusterResources.subscribeToNetworkReports(callback);
            else if ("locks".equals(arg))
                ClusterResources.subscribeToLockReports(callback);
            else if ("hdfs".equals(arg))
                ClusterResources.subscribeToHDFSRequestReports(callback);
            else if ("queue".equals(arg))
                ClusterResources.subscribeToQueueReports(callback);
            else if ("throttlingpoint".equals(arg))
                ClusterResources.subscribeToThrottlingPointReports(callback);
            else if ("immediate".equals(arg))
                ClusterResources.subscribeToImmediateReports(cb2);
            else {
                System.out.println("Unknown resource: " + arg);
                continue;
            }
            System.out.println("Subscribed to resource " + arg);
        }

        // Wait for a user interrupt
        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait();
        }
    }

}
