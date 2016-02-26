package edu.brown.cs.systems.retro.aggregation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ResourceReportingSettings {

    public static final Config CONFIG = ConfigFactory.load();

    public static final int INTERVAL = CONFIG.getInt("resource-reporting.reporting.interval");

    public static final boolean AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.active");
    public static final boolean REPORTING_ENABLED = CONFIG.getBoolean("resource-reporting.reporting.active");

    public static final boolean DISK_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.disk");
    public static final boolean DISKCACHE_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.disk-cache");
    public static final boolean NETWORK_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.network");
    public static final boolean LOCKS_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.locks");
    public static final boolean CPU_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.cpu");
    public static final boolean HDFS_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.hdfs");
    public static final boolean QUEUE_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.queue");
    public static final boolean THROTTLINGPOINT_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.throttlingpoint");
    public static final boolean BATCH_AGGREGATION_ENABLED = CONFIG.getBoolean("resource-reporting.aggregation.enabled.batch");

    public static final int DISK_CACHE_THRESHOLD = CONFIG.getInt("resource-reporting.aggregation.disk-cache-threshold");
}
