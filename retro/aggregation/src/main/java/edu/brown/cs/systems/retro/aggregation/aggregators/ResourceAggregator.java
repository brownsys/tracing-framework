package edu.brown.cs.systems.retro.aggregation.aggregators;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Resource;

/**
 * The base class for resource aggregation.
 * 
 * This implementation is thread-safe and is implemented with an emphasis on
 * performance for aggregating statistics.
 * 
 * @author Jonathan Mace
 *
 */
public abstract class ResourceAggregator {

    public final Resource.Type type;
    private long window_start_time = System.currentTimeMillis();

    private final ResourceReport prototype;
    private final ImmediateReport prototype2;

    /**
     * Create a new aggregator for a resource that does have an ID as specified
     * 
     * @param type
     *            the type of this resource
     * @param resourceid
     *            the id of this resource
     * @param enabled
     *            if false, this aggregator actually does nothing
     */
    protected ResourceAggregator(Resource.Type type, String resourceid) {
        this.type = type;

        // Create the ResourceReport prototype
        ResourceReport.Builder builder = ResourceReport.newBuilder();
        builder.setMachine(Utils.getHost()).setProcessID(Utils.getProcessID()).setProcessName(Utils.getProcessName());
        builder.setResource(type).setStart(System.currentTimeMillis());
        if (resourceid != null)
            builder.setResourceID(resourceid);
        prototype = builder.buildPartial();

        // Create the ImmediateReport prototype
        ImmediateReport.Builder builder2 = ImmediateReport.newBuilder();
        builder2.setMachine(Utils.getHost()).setProcessID(Utils.getProcessID()).setProcessName(Utils.getProcessName()).setResource(type);
        if (resourceid != null)
            builder2.setResourceID(resourceid);
        prototype2 = builder2.buildPartial();
    }

    public ImmediateReport getImmediateReportPrototype() {
        return prototype2;
    }

    /**
     * @return true if aggregation is turned on for this resource (and will
     *         subsequently be reported)
     */
    public abstract boolean enabled();

    /**
     * Create a new aggregator for a resource that doesn't have an ID
     * 
     * @param type
     *            the type of this resource
     */
    protected ResourceAggregator(Resource.Type type) {
        this(type, null);
    }

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ConcurrentHashMap<Integer, TenantOperationAggregator> usageByTenant = new ConcurrentHashMap<Integer, TenantOperationAggregator>(16, 0.75f, 1);

    /**
     * Indicate that the specified tenant started or is about to start consuming
     * this resource
     * 
     * @param tenantclass
     *            The ID of the tenant
     */
    public void starting(int tenantclass) {
        starting(null, tenantclass);
    }

    /**
     * Indicate that the specified tenant started or is about to start consuming
     * this resource
     * 
     * @param op
     *            An operation that the tenant has or is about to start. Can be
     *            null.
     * @param tenantclass
     *            The ID of the tenant
     */
    public void starting(Resource.Operation op, int tenantclass) {
        if (enabled()) {
            int hash = hashCode(op, tenantclass);
            lock.readLock().lock();
            try {
                TenantOperationAggregator usage = usageByTenant.get(hash);
                if (usage == null) {
                    TenantOperationAggregator created = new TenantOperationAggregator(op, tenantclass);
                    usage = usageByTenant.putIfAbsent(hash, created);
                    usage = usage == null ? created : usage;
                }
                usage.started();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Indicate that the specified tenant finished or is about to finish
     * consuming this resource
     * 
     * @param tenantclass
     *            The ID of the tenant
     * @param work
     *            The amount of work that was performed; semantics are
     *            resource-dependent
     * @param latency
     *            The wallclock time spent consuming this resource; semantics
     *            are resource-dependent
     */
    public void finished(int tenantclass, long work, long latency) {
        finished(null, tenantclass, work, latency);
    }

    /**
     * Indicate that the specified tenant finished or is about to finish
     * consuming this resource
     * 
     * @param op
     *            the operation that was performed
     * @param tenantclass
     *            The ID of the tenant
     * @param work
     *            The amount of work that was performed; semantics are
     *            resource-dependent
     * @param latency
     *            The wallclock time spent consuming this resource; semantics
     *            are resource-dependent
     */
    public void finished(Resource.Operation op, int tenantclass, long work, long latency) {
        if (enabled()) {
            int hash = hashCode(op, tenantclass);
            lock.readLock().lock();
            try {
                TenantOperationAggregator usage = usageByTenant.get(hash);
                if (usage == null) {
                    TenantOperationAggregator created = new TenantOperationAggregator(op, tenantclass);
                    usage = usageByTenant.putIfAbsent(hash, created);
                    usage = usage == null ? created : usage;
                }
                usage.finished(work, latency);
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Indicate that the specified operation was both started and finished
     * 
     * @param op
     *            the operation that was performed
     * @param tenantclass
     *            The ID of the tenant
     * @param work
     *            The amount of work that was performed; semantics are
     *            resource-dependent
     * @param latency
     *            The wallclock time spent consuming this resource; semantics
     *            are resource-dependent
     */
    public void startedAndFinished(Resource.Operation op, int tenantclass, long work, long latency) {
        if (enabled()) {
            int hash = hashCode(op, tenantclass);
            lock.readLock().lock();
            try {
                TenantOperationAggregator usage = usageByTenant.get(hash);
                if (usage == null) {
                    TenantOperationAggregator created = new TenantOperationAggregator(op, tenantclass);
                    usage = usageByTenant.putIfAbsent(hash, created);
                    usage = usage == null ? created : usage;
                }
                usage.started();
                usage.finished(work, latency);
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    private static final int OPCOUNT = Resource.Operation.values().length;

    private int hashCode(Resource.Operation op, int tenantclass) {
        if (op == null)
            return tenantclass * (OPCOUNT + 1);
        else
            return tenantclass * (OPCOUNT + 1) + (op.ordinal() + 1);
    }

    /**
     * Returns a report or null if none to be reported
     * 
     * @return
     */
    public ResourceReport getReport() {
        if (!enabled())
            return null;

        // First, copy out the usage map and reset it;
        lock.writeLock().lock();
        Map<Integer, TenantOperationAggregator> usageByTenant;
        try {
            usageByTenant = this.usageByTenant;
            this.usageByTenant = new ConcurrentHashMap<Integer, TenantOperationAggregator>(Math.max(16, 3 * usageByTenant.size() / 2), 0.75f, 1);
        } finally {
            lock.writeLock().unlock();
        }

        // Set the window time and return if there's nothing to report
        if (usageByTenant.size() == 0) {
            window_start_time = System.currentTimeMillis();
            return null;
        }

        // Create a builder for the report from the prototype and set the window
        // start and end times
        ResourceReport.Builder builder = ResourceReport.newBuilder().mergeFrom(prototype);
        builder.setStart(window_start_time).setEnd(window_start_time = System.currentTimeMillis());

        // Add the tenant reports
        for (TenantOperationAggregator tenantAggregator : usageByTenant.values())
            builder.addTenantReport(tenantAggregator.getReport());

        // Return the build report
        return builder.build();
    }

    @Override
    public String toString() {
        return prototype.getResource() + "-" + prototype.getResourceID();
    }

    private static class Utils {

        private static Class<?> MainClass;
        private static String ProcessName;
        private static Integer ProcessID;
        private static String Host;

        public static Class<?> getMainClass() {
            if (MainClass == null) {
                Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
                for (StackTraceElement[] currStack : stacks) {
                    if (currStack.length == 0)
                        continue;
                    StackTraceElement lastElem = currStack[currStack.length - 1];
                    if (lastElem.getMethodName().equals("main")) {
                        try {
                            String mainClassName = lastElem.getClassName();
                            MainClass = Class.forName(mainClassName);
                        } catch (ClassNotFoundException e) {
                            // bad class name in line containing main?!
                            // shouldn't happen
                            e.printStackTrace();
                        }
                    }
                }
            }
            return MainClass;
        }

        public static String getProcessName() {
            if (ProcessName == null) {
                Class<?> mainClass = getMainClass();
                if (mainClass == null)
                    ProcessName = "";
                else
                    ProcessName = mainClass.getSimpleName();
            }
            return ProcessName;
        }

        public static int getProcessID() {
            if (ProcessID == null) {
                String procname = ManagementFactory.getRuntimeMXBean().getName();
                ProcessID = Integer.parseInt(procname.substring(0, procname.indexOf('@')));
            }
            return ProcessID;
        }

        public static String getHost() {
            if (Host == null) {
                try {
                    Host = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    Host = "unknown";
                }
            }
            return Host;
        }
    }

}
