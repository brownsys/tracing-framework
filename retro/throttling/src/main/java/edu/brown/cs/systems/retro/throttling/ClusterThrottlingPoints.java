package edu.brown.cs.systems.retro.throttling;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.PerInstanceThrottlingPointSpecification;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingUpdate;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.UniformThrottlingPointSpecification;
import edu.brown.cs.systems.retro.aggregation.Callback;
import edu.brown.cs.systems.retro.aggregation.ClusterResources;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;

/**
 * API for controlling throttling points in the cluster
 * 
 * @author a-jomace
 *
 */
public class ClusterThrottlingPoints {

    private static ClusterState cluster = new ClusterState();

    /** @return the IDs of the known throttling points in the system */
    public static Collection<String> getThrottlingPoints() {
        return cluster.getThrottlingPoints();
    }

    /** @return the IDs of the known tenants */
    public static Collection<Integer> getTenants() {
        return cluster.getTenants();
    }

    /**
     * @param throttlingPointName
     *            the name of the throttling point
     * @return the number of known instances of the specified throttling point
     */
    public static long getThrottlingPointInstanceCount(String throttlingPointName) {
        return cluster.getThrottlingPointInstanceCount(throttlingPointName);
    }

    /**
     * @param throttlingPointName
     *            the name of the throttling point
     * @param tenantID
     *            the ID of the tenant
     * @return the number of instances of the specified throttling point that
     *         the tenant is currently utilizing
     */
    public static long getTenantThrottlingPointInstanceCount(String throttlingPointName, int tenantID) {
        return cluster.getTenantThrottlingPointInstanceCount(throttlingPointName, tenantID);
    }

    /**
     * Updates the throttling rates for tenants at the specified throttling
     * point.
     * 
     * If a tenant is not specified, it is assumed that the tenant should not be
     * throttled.
     * 
     * For each tenant, the cluster throttling point divides the tenant's rate
     * by the number of throttling point instances that the tenant is actually
     * touching. A tenant is touching a throttling point if it was reported in
     * the previous reporting interval for that throttling point
     * 
     * For example, if a tenant previously passed through 2 instances of a
     * throttling point (even though there may be >2 instances of the throttling
     * point in the cluster), then the rate will be divided only by 2.
     * 
     * @param throttlingPointName
     *            the name of the throttling point to update
     * @param globalTenantRates
     *            the global throttling rates for each tenant
     */
    public static void setThrottlingPointRates(String throttlingPointName, Map<Integer, Double> globalTenantRates) {
        ThrottlingPointSpecification.Builder spec = ThrottlingPointSpecification.newBuilder();
        for (Integer tenantId : globalTenantRates.keySet()) {
            Double globalRate = globalTenantRates.get(tenantId);
            if (tenantId != null && globalRate != null && globalRate > 0) {
                spec.addTenantID(tenantId);
                spec.addThrottlingRate(globalRate / Math.max(1, getTenantThrottlingPointInstanceCount(throttlingPointName, tenantId)));
            }
        }

        UniformThrottlingPointSpecification uspec = UniformThrottlingPointSpecification.newBuilder().setThrottlingPointName(throttlingPointName)
                .setThrottlingPointSpecification(spec).build();

        ThrottlingUpdate update = ThrottlingUpdate.newBuilder().addUniformThrottlingPointSpecifications(uspec).build();

        PubSub.publish(ConfigFactory.load().getString("retro.throttling.topic"), update);
    }

    public static void setThrottlingPointRates(String throttlingPointName, String host, int procId, Map<Integer, Double> tenantRates) {
        ThrottlingPointSpecification.Builder spec = ThrottlingPointSpecification.newBuilder();
        for (Integer tenantId : tenantRates.keySet()) {
            Double rate = tenantRates.get(tenantId);
            if (tenantId != null && rate != null && rate > 0) {
                spec.addTenantID(tenantId);
                spec.addThrottlingRate(rate);
            }
        }

        PerInstanceThrottlingPointSpecification pispec = PerInstanceThrottlingPointSpecification.newBuilder().setThrottlingPointName(throttlingPointName)
                .setHost(host).setProcid(procId).setThrottlingPointSpecification(spec).build();

        ThrottlingUpdate update = ThrottlingUpdate.newBuilder().addPerInstanceThrottlingPointSpecifications(pispec).build();

        PubSub.publish(ConfigFactory.load().getString("retro.throttling.topic"), update);
    }

    /** Clears all throttling at the specified throttling point */
    public static void clear(String throttlingPointName) {
        setThrottlingPointRates(throttlingPointName, Collections.EMPTY_MAP);
    }

    /** Clears all throttling in the cluster */
    public static void clearAll() {
        ThrottlingUpdate update = ThrottlingUpdate.newBuilder().setClearAll(true).build();
        PubSub.publish(ConfigFactory.load().getString("retro.throttling.topic"), update);
    }

    /**
     * Keeps track of the throttling points that exist in the system, how many
     * instances of point there are, etc.
     * 
     * Does NOT do controller-like tracking of rates, etc. The purpose of this
     * is only to keep track of the existence of throttling points
     * */
    private static class ClusterState extends Callback {

        private Map<String, ThrottlingPoint> throttlingPoints = new HashMap<String, ThrottlingPoint>();

        public ClusterState() {
            ClusterResources.subscribeToThrottlingPointReports(this);
        }

        public synchronized Collection<String> getThrottlingPoints() {
            return new HashSet<String>(throttlingPoints.keySet());
        }

        public synchronized Collection<Integer> getTenants() {
            Set<Integer> tenants = new HashSet<Integer>();
            for (ThrottlingPoint point : throttlingPoints.values()) {
                tenants.addAll(point.getTenants());
            }
            return tenants;
        }

        public synchronized long getThrottlingPointInstanceCount(String throttlingPointName) {
            ThrottlingPoint point = throttlingPoints.get(throttlingPointName);
            if (point != null)
                return point.instanceCount();
            else
                return 0;
        }

        public synchronized long getTenantThrottlingPointInstanceCount(String throttlingPointName, int tenantID) {
            ThrottlingPoint point = throttlingPoints.get(throttlingPointName);
            if (point != null)
                return point.instanceCount(tenantID);
            else
                return 0;
        }

        @Override
        protected synchronized void OnMessage(ResourceReport report) {
            if (report.getResource() != Type.THROTTLINGPOINT)
                return;

            String name = report.getResourceID();
            ThrottlingPoint point = throttlingPoints.get(name);
            if (point == null)
                throttlingPoints.put(name, point = new ThrottlingPoint());

            point.update(report);
        }

    }

    /**
     * Keeps track of existence of known instances of a throttling point and
     * tenants using each instance.
     * 
     * An instance will expire after 5 seconds of no communication
     */
    private static class ThrottlingPoint {

        private class Tenant {
            private Set<Instance> instances = new HashSet<Instance>();

            private void unlink() {
                for (Instance i : instances)
                    i.tenants.remove(this);
                instances.clear();
            }
        }

        private class Instance {
            private Set<Tenant> tenants = new HashSet<Tenant>();

            private void unlink() {
                for (Tenant t : tenants)
                    t.instances.remove(this);
                tenants.clear();
            }
        }

        private LoadingCache<Integer, Instance> instanceCache;
        private LoadingCache<Integer, Tenant> tenantCache;

        private ThrottlingPoint() {
            // When a tenant times out, remove its presence from all known
            // throttling point instances
            RemovalListener<Integer, Tenant> removeTenant = new RemovalListener<Integer, Tenant>() {
                public void onRemoval(RemovalNotification<Integer, Tenant> n) {
                    n.getValue().unlink();
                }
            };

            // When an instance times out, remove its presence from all tenants
            RemovalListener<Integer, Instance> removeInstance = new RemovalListener<Integer, Instance>() {
                public void onRemoval(RemovalNotification<Integer, Instance> n) {
                    n.getValue().unlink();
                }
            };

            // Creates a new Instance
            CacheLoader<Integer, Instance> createInstance = new CacheLoader<Integer, Instance>() {
                public Instance load(Integer i) throws Exception {
                    return new Instance();
                }
            };

            // Creates a new Tenant
            CacheLoader<Integer, Tenant> createTenant = new CacheLoader<Integer, Tenant>() {
                public Tenant load(Integer i) throws Exception {
                    return new Tenant();
                }
            };

            this.instanceCache = CacheBuilder.newBuilder().concurrencyLevel(1).expireAfterAccess(5, TimeUnit.SECONDS).removalListener(removeInstance)
                    .build(createInstance);
            this.tenantCache = CacheBuilder.newBuilder().concurrencyLevel(1).expireAfterAccess(5, TimeUnit.SECONDS).removalListener(removeTenant)
                    .build(createTenant);
        }

        /**
         * Updates the tenants that are using the instance Updates the instances
         * that each tenant is using Refreshes the tenants and instance present
         * in the report
         */
        public synchronized void update(ResourceReport report) {
            int instanceID = (report.getMachine() + "-" + report.getProcessID()).hashCode();
            Instance i = instanceCache.getUnchecked(instanceID);

            // First, just remove all of the old tenants (they will be re-added)
            for (Tenant t : i.tenants) {
                t.instances.remove(i);
            }
            i.tenants.clear();

            // Now, add all of the new tenants to the instance
            for (TenantOperationReport tenantReport : report.getTenantReportList()) {
                int tenantID = tenantReport.getTenantClass();
                Tenant t = tenantCache.getUnchecked(tenantID);
                t.instances.add(i);
                i.tenants.add(t);
            }

            // Explicitly clean up if needed
            instanceCache.cleanUp();
            tenantCache.cleanUp();
        }

        /** Returns the total number of instances of this throttling point */
        public synchronized long instanceCount() {
            return instanceCache.size();
        }

        /**
         * Returns the total number of instances of this throttling point that
         * the specified tenant is utilizing
         */
        public synchronized int instanceCount(int tenantID) {
            Tenant t = tenantCache.asMap().get(tenantID);
            return t == null ? 0 : t.instances.size();
        }

        public synchronized Collection<Integer> getTenants() {
            return tenantCache.asMap().keySet();
        }

    }

}
