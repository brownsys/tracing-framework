package edu.brown.cs.systems.retro.throttling.throttlingpoints;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.ThrottlingPointAggregator;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.ratelimiters.RateLimiter;

/**
 * A throttling point that throttles each tenant based on a provided rate.
 * 
 * @author a-jomace
 *
 */
public class SimpleThrottlingPoint implements ThrottlingPoint {

    private final String name;
    private final ThrottlingPointAggregator aggregator;
    private final ConcurrentMap<Integer, RateLimiter> limiters;
    private double defaultRate = Double.MAX_VALUE;

    public SimpleThrottlingPoint(String name) {
        this.name = name;
        this.aggregator = LocalResources.getThrottlingPointAggregator(name);
        this.limiters = new ConcurrentHashMap<Integer, RateLimiter>();
    }

    private RateLimiter get(int tenantId) {
        RateLimiter limiter = limiters.get(tenantId);
        if (limiter == null) {
            synchronized (this) {
                limiter = limiters.get(tenantId);
                if (limiter == null) {
                    limiter = new RateLimiter(defaultRate);
                    limiters.put(tenantId, limiter);
                }
            }
        }
        return limiter;
    }

    @Override
    public void throttle() {
        // Get the rate limiter for the current tenant
        int tenantID = Retro.getTenant();
        RateLimiter limiter = get(tenantID);
        if (!limiter.tryAcquire()) {
            // No permits immediately available
            aggregator.throttling(tenantID);
            aggregator.throttled(tenantID, (long) (1000000000L * limiter.acquireUninterruptibly())); // limiter.acquire
                                                                                                     // returns
                                                                                                     // time
                                                                                                     // spent
                                                                                                     // waiting,
                                                                                                     // in
                                                                                                     // seconds.
                                                                                                     // convert
                                                                                                     // to
                                                                                                     // nanoseconds
        } else {
            // Permits are immediately available
            aggregator.didNotThrottle(tenantID);
        }
    }

    /**
     * Sets the throttling rate for a tenant
     * 
     * @param tenantId
     *            The tenant ID to set the throttling rate for
     * @param rate
     *            The rate to be set
     */
    private synchronized void setTenantRate(int tenantId, double rate) {
        if (rate <= 0 || Double.isNaN(rate) || Double.isInfinite(rate))
            rate = defaultRate;

        RateLimiter limiter = limiters.get(tenantId);
        if (limiter == null) // Create a new rate limiter
            limiters.put(tenantId, new RateLimiter(rate));
        else
            // Update existing rate limiter
            limiter.setRate(rate);
    }

    @Override
    public synchronized void update(ThrottlingPointSpecification spec) {
        // Pull out the tenant IDs for tenants that currently are rate limited
        HashSet<Integer> remainingTenants = new HashSet<Integer>(limiters.keySet());

        // Update the limits as specified.
        for (int i = 0; i < spec.getTenantIDCount(); i++) {
            int tenantId = spec.getTenantID(i);
            setTenantRate(tenantId, spec.getThrottlingRate(i));
            remainingTenants.remove(tenantId);
        }

        // Update the default rate if it is specified
        double defaultRate = spec.hasDefaultThrottlingRate() ? spec.getDefaultThrottlingRate() : Double.MAX_VALUE;
        if (defaultRate <= 0 || Double.isNaN(defaultRate) || Double.isInfinite(defaultRate))
            defaultRate = Double.MAX_VALUE;
        this.defaultRate = defaultRate;

        // If a tenant does not have a rate specified, then set to the default
        // rate
        for (Integer tenantId : remainingTenants) {
            setTenantRate(tenantId, defaultRate);
        }
    }

    @Override
    public synchronized void clearRates() {
        defaultRate = Double.MAX_VALUE;
        for (RateLimiter limiter : limiters.values()) {
            limiter.setRate(defaultRate);
        }
    }

}
