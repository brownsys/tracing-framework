package edu.brown.cs.systems.retro.aggregation.reporters;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;

/**
 * Periodically sends resource reports
 * 
 * @author a-jomace
 */
public class PubSubReporter implements Runnable, Reporter {
    
    // Runs threads as daemon threads, but ensures runnables finish before exiting
    private static final ScheduledExecutorService executor = MoreExecutors.getExitingScheduledExecutorService(
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1), 2, TimeUnit.SECONDS);

    private final Config config;
    private final String immediateReportTopic;
    private final Map<ResourceAggregator, String> resources = new HashMap<ResourceAggregator, String>();

    public PubSubReporter(Config config) {
        this.config = config;
        this.immediateReportTopic = getImmediateReportTopic(config);

        // Get the reporting interval from the config
        long interval = config.getLong("resource-reporting.reporting.interval");

        // Determine the first reporting interval - as much as possible, align
        // with clock boundaries
        long initial_delay = 2 * interval - (System.currentTimeMillis() % interval);

        // Schedule at a fixed rate
        executor.scheduleAtFixedRate(this, initial_delay, interval, TimeUnit.MILLISECONDS);
        System.out.println("Resource reporting executor started");
        
        Runtime.getRuntime().addShutdownHook(new Thread(this)); // Also publish once on shutdown
    }

    @Override
    public synchronized void register(ResourceAggregator aggregator) {
        if (!resources.containsKey(aggregator)) {
            String topic = getTopic(config, aggregator.type);
            register(topic, aggregator);
        }
    }

    public synchronized void register(String topic, ResourceAggregator aggregator) {
        if (!resources.containsKey(aggregator)) {
            if (topic != null) {
                resources.put(aggregator, topic);
                System.out.println("ZmqReporter: " + aggregator + " -> " + topic);
            }
        }

    }

    public static String getTopic(Config config, Resource.Type type) {
        String suffix = "default";

        switch (type) {
        case DISK:
            suffix = "disk";
            break;
        case DISKCACHE:
            suffix = "disk-cache";
            break;
        case NETWORK:
            suffix = "network";
            break;
        case LOCKING:
            suffix = "locks";
            break;
        case CPU:
            suffix = "cpu";
            break;
        case HDFSREQUEST:
            suffix = "hdfs";
            break;
        case QUEUE:
            suffix = "queue";
            break;
        case THROTTLINGPOINT:
            suffix = "throttlingpoint";
            break;
        default:
            break;
        }

        String property = "resource-reporting.reporting.zmq.topics." + suffix;
        try {
            return config.getString(property);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getImmediateReportTopic(Config config) {
        try {
            return config.getString("resource-reporting.reporting.zmq.topics.immediate");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public final void run() {
        synchronized (this) {
            for (ResourceAggregator r : resources.keySet()) {
                if (r.enabled()) {
                    String topic = resources.get(r);
                    try {
                        ResourceReport report = r.getReport();
                        if (report != null)
                            PubSub.publish(topic, report);
                    } catch (Exception e) {
                        System.out.println("Exception sending resource report on topic " + topic);
                        System.out.println(r);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * publishes the immediate report on the immediate report topic. Not
     * published inband, instead, immediately submitted to executor to build and
     * send the report
     */
    public void reportImmediately(final ImmediateReport.Builder report) {
        if (report != null && immediateReportTopic != null) {
            Runnable doReport = new Runnable() {
                public void run() {
                    try {
                        PubSub.publish(immediateReportTopic, report.build());
                    } catch (Exception e) {
                        System.out.println("Exception sending immediate report on topic " + immediateReportTopic);
                    }
                }
            };
            executor.submit(doReport);
        }
    }

}
