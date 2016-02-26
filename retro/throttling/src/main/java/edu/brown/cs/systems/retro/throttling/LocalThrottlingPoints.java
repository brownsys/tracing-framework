package edu.brown.cs.systems.retro.throttling;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigUtil;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.PerInstanceThrottlingPointSpecification;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingUpdate;
import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.UniformThrottlingPointSpecification;
import edu.brown.cs.systems.retro.throttling.throttlingpoints.BatchedThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.throttlingpoints.SimpleThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.throttlingqueues.ThrottlingDelayQueue;
import edu.brown.cs.systems.retro.throttling.throttlingqueues.ThrottlingLockingQueue;
import edu.brown.cs.systems.tracing.Utils;

/**
 * API for accessing throttling points for the current process
 * 
 * @author a-jomace
 *
 */
public class LocalThrottlingPoints {

    private static final String DEFAULT_THROTTLINGPOINT_PROPERTY_KEY = "retro.throttling.default-throttlingpoint";
    private static final String DEFAULT_THROTTLINGQUEUE_PROPERTY_KEY = "retro.throttling.default-throttlingqueue";
    private static final String CUSTOM_THROTTLINGPOINT_KEY_PREFIX = "retro.throttling.throttlingpoints";
    private static final String CUSTOM_THROTTLINGQUEUE_KEY_PREFIX = "retro.throttling.throttlingqueues";

    /** To force subscribe to updates */
    public static void init() {
    }

    // Receives updates via pubsub
    private static final ThrottlingPointUpdateCallback updatesCallback = new ThrottlingPointUpdateCallback();
    private static Map<String, ThrottlingPointSpecification> latestSpecs = new HashMap<String, ThrottlingPointSpecification>();

    /**
     * This method is used to create all actual throttling point
     * implementations. Change the behavior here to change whatever type of
     * throttling point is actually used.
     */
    private static ThrottlingPoint createPoint(String throttlingPointName) {
        Config config = ConfigFactory.load();

        // The default throttling point type to use
        String throttlingPointType = config.getString(DEFAULT_THROTTLINGPOINT_PROPERTY_KEY);

        // Check to see whether this named throttling point is individually
        // configured
        String customConfigPath = ConfigUtil.joinPath(throttlingPointName);
        if (config.getConfig(CUSTOM_THROTTLINGPOINT_KEY_PREFIX).hasPath(customConfigPath)) {
            throttlingPointType = config.getConfig(CUSTOM_THROTTLINGPOINT_KEY_PREFIX).getString(customConfigPath);
        }

        // Get the instance and throw an exception if incorrectly configured
        ThrottlingPoint instance = getThrottlingPointInstance(throttlingPointType, throttlingPointName);
        if (instance == null) {
            throw new IllegalArgumentException("Invalid throttling point type for " + throttlingPointName + ": " + throttlingPointType);
        }
        return instance;
    }

    /**
     * Returns a throttling point of the specified type, or null if the type is
     * invalid
     */
    private static ThrottlingPoint getThrottlingPointInstance(String throttlingPointType, String throttlingPointName) {
        if (throttlingPointType.equals("simple") || throttlingPointType.equals("default")) {
            return new SimpleThrottlingPoint(throttlingPointName);
        } else if (throttlingPointType.startsWith("batched")) {
            String[] splits = throttlingPointType.split("-");
            String wrappedType = splits[1];
            int batchSize = Integer.parseInt(splits[2]);
            return new BatchedThrottlingPoint(getThrottlingPointInstance(wrappedType, throttlingPointName), batchSize);
        }
        return null;
    }

    /**
     * This method is used to create all actual throttling point
     * implementations. Change the behavior here to change whatever type of
     * throttling point is actually used.
     */
    private static <T> ThrottlingQueue<T> createQueue(String throttlingQueueName) {
        Config config = ConfigFactory.load();

        // The default throttling point type to use
        String throttlingQueueType = config.getString(DEFAULT_THROTTLINGQUEUE_PROPERTY_KEY);

        // Check to see whether this named throttling point is individually
        // configured
        String customConfigPath = ConfigUtil.joinPath(throttlingQueueName);
        if (config.getConfig(CUSTOM_THROTTLINGQUEUE_KEY_PREFIX).hasPath(customConfigPath)) {
            throttlingQueueType = config.getConfig(CUSTOM_THROTTLINGQUEUE_KEY_PREFIX).getString(customConfigPath);
        }

        // Get the instance and throw an exception if incorrectly configured
        ThrottlingQueue<T> instance = getThrottlingQueueInstance(throttlingQueueType, throttlingQueueName);
        if (instance == null) {
            throw new IllegalArgumentException("Invalid throttling queue type for " + throttlingQueueName + ": " + throttlingQueueType);
        }
        return instance;
    }

    /**
     * Returns a throttling point of the specified type, or null if the type is
     * invalid
     */
    private static <T> ThrottlingQueue<T> getThrottlingQueueInstance(String throttlingQueueType, String throttlingQueueName) {
        if (throttlingQueueType.equals("locking") || throttlingQueueType.equals("default")) {
            return new ThrottlingLockingQueue<T>(throttlingQueueName);
        } else if (throttlingQueueType.equals("delay")) {
            return new ThrottlingDelayQueue<T>(throttlingQueueName);
        }
        return null;
    }

    private static final ConcurrentMap<String, ThrottlingPoint> throttlingPointCache = new ConcurrentHashMap<String, ThrottlingPoint>();
    private static final ConcurrentMap<String, ThrottlingQueue<?>> throttlingQueues = new ConcurrentHashMap<String, ThrottlingQueue<?>>();

    /**
     * Create or retrieve the throttling point with the specified name
     * 
     * @param throttlingPointName
     *            The name of the throttling point to get
     * @return A ThrottlingPoint instance
     */
    public static ThrottlingPoint getThrottlingPoint(String throttlingPointName) {
        ThrottlingPoint point = throttlingPointCache.get(throttlingPointName);
        if (point == null) {
            synchronized (throttlingPointCache) {
                point = throttlingPointCache.get(throttlingPointName);
                if (point == null) {
                    point = createPoint(throttlingPointName);
                    throttlingPointCache.put(throttlingPointName, point);
                    refresh(throttlingPointName);
                }
            }
        }
        return point;
    }

    /**
     * Creates a throttling queue for the provided queue id. Queues are NOT
     * cached and it is recommended not to create multiple queues with the same
     * ID
     */
    public static <T> ThrottlingQueue<T> getThrottlingQueue(String queueid) {
        synchronized (throttlingQueues) {
            if (throttlingQueues.containsKey(queueid)) {
                throw new RuntimeException("Cannot create more than one throttling queue with the same queue id");
            }
            ThrottlingQueue<T> queue = createQueue(queueid);
            throttlingQueues.put(queueid, queue);
            refresh(queueid);
            return queue;
        }
    }

    public static synchronized void refresh(String pointName) {
        update(pointName, latestSpecs.get(pointName));

    }

    public static synchronized void update(String pointName, ThrottlingPointSpecification spec) {
        if (spec == null)
            return;

        if (throttlingPointCache.containsKey(pointName)) {
            throttlingPointCache.get(pointName).update(spec);
        }
        if (throttlingQueues.containsKey(pointName)) {
            throttlingQueues.get(pointName).update(spec);
        }
        latestSpecs.put(pointName, spec);
    }

    public static synchronized void clearAll() {
        for (ThrottlingPoint p : throttlingPointCache.values()) {
            p.clearRates();
        }
        for (ThrottlingQueue<?> q : throttlingQueues.values()) {
            q.clearRates();
        }
        latestSpecs.clear();
    }

    private static synchronized void processUpdate(ThrottlingUpdate update) {
        if (update == null)
            return;

        for (UniformThrottlingPointSpecification s : update.getUniformThrottlingPointSpecificationsList())
            update(s.getThrottlingPointName(), s.getThrottlingPointSpecification());

        for (PerInstanceThrottlingPointSpecification s : update.getPerInstanceThrottlingPointSpecificationsList())
            if (s.getHost().equals(Utils.getHost()) && s.getProcid() == Utils.getProcessID())
                update(s.getThrottlingPointName(), s.getThrottlingPointSpecification());

        if (update.hasClearAll() && update.getClearAll() == true)
            clearAll();
    }

    /**
     * Handles throttling point rate updates sent via pubsub
     * 
     * @author a-jomace
     */
    private static class ThrottlingPointUpdateCallback extends Subscriber<ThrottlingUpdate> {

        public ThrottlingPointUpdateCallback() {
            String topic = ConfigFactory.load().getString("retro.throttling.topic");
            PubSub.subscribe(topic, this);
        }

        @Override
        protected void OnMessage(ThrottlingUpdate update) {
            processUpdate(update);
        }

    }

}
