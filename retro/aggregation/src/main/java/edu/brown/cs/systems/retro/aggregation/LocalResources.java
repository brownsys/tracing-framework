package edu.brown.cs.systems.retro.aggregation;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.CPUAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.DiskAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.DiskCacheAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSBackgroundAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.LockAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.NetworkAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.QueueAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;
import edu.brown.cs.systems.retro.aggregation.aggregators.ThrottlingPointAggregator;
import edu.brown.cs.systems.retro.aggregation.reporters.NullReporter;
import edu.brown.cs.systems.retro.aggregation.reporters.PubSubReporter;
import edu.brown.cs.systems.retro.aggregation.reporters.Reporter;

/**
 * The front-end API for clients reporting their resource usage
 */
public class LocalResources {

    private static volatile Reporter reporter = null;

    private static volatile DiskAggregator disk = null;

    private static volatile NetworkAggregator uplink = null;
    private static volatile NetworkAggregator downlink = null;
    private static volatile NetworkAggregator loopup = null;
    private static volatile NetworkAggregator loopdown = null;

    private static volatile HDFSAggregator hdfs = null;
    private static volatile HDFSBackgroundAggregator hdfsbackground = null;
    private static volatile CPUAggregator cpu = null;
    private static volatile DiskCacheAggregator diskcache = null;
    private static final ConcurrentHashMap<String, QueueAggregator> queues = new ConcurrentHashMap<String, QueueAggregator>(4, 0.75f, 1);
    private static final ConcurrentHashMap<String, LockAggregator> locks = new ConcurrentHashMap<String, LockAggregator>(16, 0.75f, 1);
    private static final ConcurrentHashMap<String, ThrottlingPointAggregator> throttlingpoints = new ConcurrentHashMap<String, ThrottlingPointAggregator>(4,
            0.75f, 1);

    /** @return this process's reporter, creating it if necessary */
    public static Reporter getReporter() {
        if (reporter == null) {
            synchronized (LocalResources.class) {
                if (reporter == null) {
                    if (!ResourceReportingSettings.REPORTING_ENABLED)
                        reporter = new NullReporter();
                    else
                        reporter = new PubSubReporter(ResourceReportingSettings.CONFIG);
                }
            }
        }
        return reporter;
    }

    /** @return this process's disk aggregator, creating it if necessary */
    public static DiskAggregator getDiskAggregator() {
        if (disk == null)
            synchronized (LocalResources.class) {
                if (disk == null) {
                    long small_read_work = ResourceReportingSettings.CONFIG.getLong("resource-reporting.aggregation.small-read");
                    long small_read_seek = ResourceReportingSettings.CONFIG.getLong("resource-reporting.aggregation.seek-threshold");
                    disk = new DiskAggregator(small_read_work, small_read_seek);
                    getReporter().register(disk);
                }
            }
        return disk;
    }

    /** @return this process's disk cache aggregator, creating it if necessary */
    public static DiskCacheAggregator getDiskCacheAggregator() {
        if (diskcache == null)
            synchronized (LocalResources.class) {
                if (diskcache == null) {
                    diskcache = new DiskCacheAggregator(ResourceReportingSettings.DISK_CACHE_THRESHOLD);
                    getReporter().register(diskcache);
                }
            }
        return diskcache;
    }

    /** @return this process's CPU aggregator, creating it if necessary */
    public static CPUAggregator getCPUAggregator() {
        if (cpu == null)
            synchronized (LocalResources.class) {
                if (cpu == null) {
                    cpu = new CPUAggregator();
                    getReporter().register(cpu);
                }
            }
        return cpu;
    }

    /** @return this process's HDFS aggregator, creating it if necessary */
    public static HDFSAggregator getHDFSAggregator() {
        if (hdfs == null)
            synchronized (LocalResources.class) {
                if (hdfs == null) {
                    hdfs = new HDFSAggregator();
                    getReporter().register(hdfs);
                }
            }
        return hdfs;
    }

    /**
     * @return this process's HDFS background process aggregator, creating it if
     *         necessary
     */
    public static HDFSBackgroundAggregator getHDFSBackgroundAggregator() {
        if (hdfsbackground == null)
            synchronized (LocalResources.class) {
                if (hdfsbackground == null) {
                    hdfsbackground = new HDFSBackgroundAggregator();
                    getReporter().register(hdfsbackground);
                }
            }
        return hdfsbackground;
    }

    /**
     * @return this process's network uplink aggregator, creating it if
     *         necessary
     */
    public static NetworkAggregator getNetworkUplinkAggregator() {
        if (uplink == null)
            synchronized (LocalResources.class) {
                if (uplink == null) {
                    uplink = NetworkAggregator.createUplinkAggregator();
                    getReporter().register(uplink);
                }
            }
        return uplink;
    }

    /**
     * @return this process's network downlink aggregator, creating it if
     *         necessary
     */
    public static NetworkAggregator getNetworkDownlinkAggregator() {
        if (downlink == null)
            synchronized (LocalResources.class) {
                if (downlink == null) {
                    downlink = NetworkAggregator.createDownlinkAggregator();
                    getReporter().register(downlink);
                }
            }
        return downlink;
    }

    /**
     * @return this process's loopback uplink aggregator, creating it if
     *         necessary
     */
    public static NetworkAggregator getLoopbackUplinkAggregator() {
        if (loopup == null)
            synchronized (LocalResources.class) {
                if (loopup == null) {
                    loopup = NetworkAggregator.createLoopbackUplinkAggregator();
                    getReporter().register(loopup);
                }
            }
        return loopup;
    }

    /**
     * @return this process's loopback downlink aggregator, creating it if
     *         necessary
     */
    public static NetworkAggregator getLoopbackDownlinkAggregator() {
        if (loopdown == null)
            synchronized (LocalResources.class) {
                if (loopdown == null) {
                    loopdown = NetworkAggregator.createLoopbackDownlinkAggregator();
                    getReporter().register(loopdown);
                }
            }
        return loopdown;
    }

    /**
     * @return this process's aggregator for the specified lock object, creating
     *         the aggregator if necessary
     */
    public static LockAggregator getLockAggregator(String lockid) {
        LockAggregator lockAggregator = locks.get(lockid);
        if (lockAggregator == null) {
            LockAggregator candidateNewLockAggregator = new LockAggregator(lockid);
            lockAggregator = locks.putIfAbsent(lockid, candidateNewLockAggregator);
            if (lockAggregator == null) {
                lockAggregator = candidateNewLockAggregator;
                getReporter().register(lockAggregator);
            }
        }
        return lockAggregator;
    }

    /**
     * @return this process's aggregator for the specified queue object,
     *         creating the aggregator if necessary
     */
    public static QueueAggregator getQueueAggregator(String queueid) {
        QueueAggregator q = queues.get(queueid);
        if (q == null) {
            QueueAggregator candidate = new QueueAggregator(queueid);
            q = queues.putIfAbsent(queueid, candidate);
            if (q == null) {
                q = candidate;
                getReporter().register(q);
            }
        }
        return q;
    }

    /**
     * @return this process's aggregator for the specified throttling point,
     *         creating the aggregator if necessary
     */
    public static ThrottlingPointAggregator getThrottlingPointAggregator(String throttlingPointName) {
        ThrottlingPointAggregator tpoint = throttlingpoints.get(throttlingPointName);
        if (tpoint == null) {
            ThrottlingPointAggregator candidate = new ThrottlingPointAggregator(throttlingPointName);
            tpoint = throttlingpoints.putIfAbsent(throttlingPointName, candidate);
            if (tpoint == null) {
                tpoint = candidate;
                getReporter().register(tpoint);
            }
        }
        return tpoint;
    }

    /**
     * Instead of doing the aggregation thang, this reports immediately using
     * the reporter
     */
    public static void reportImmediately(ResourceAggregator aggregator, Resource.Operation op, int tenantclass, long work, long latency) {
        ImmediateReport.Builder builder = ImmediateReport.newBuilder().mergeFrom(aggregator.getImmediateReportPrototype());
        builder.setTimestamp(System.currentTimeMillis());
        builder.setOperation(op);
        builder.setTenantClass(tenantclass);
        builder.setWork(work);
        builder.setLatency(latency);
        getReporter().reportImmediately(builder);
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.out.println("publishing immediately");
            reportImmediately(getHDFSAggregator(), Operation.CREATE_SYMLINK, 28, 5, 10);
            Thread.sleep(1000);
            System.out.println("publishing disk read!");
            getDiskAggregator().read(55, 50, new Random().nextInt(100));
            Thread.sleep(1000);
            System.out.println("publishing network read!");
            getNetworkDownlinkAggregator().finished(Operation.READ, 22, 77, 200);
            Thread.sleep(1000);
            System.out.println("publishing network write!");
            getNetworkUplinkAggregator().finished(Operation.WRITE, 3, 4, 100);
            Thread.sleep(1000);
            System.out.println("publishing lock!");
            getLockAggregator("mylock").released(10, 100, 1000, 10000);
            Thread.sleep(1000);
            System.out.println("publishing hdfs read request completion report!");
            getHDFSAggregator().finished(10, Operation.READ, 100001234);
            Thread.sleep(1000);
        }
    }

}
