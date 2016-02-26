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
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.PerInstanceSchedulerSpecification;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.SchedulerSpecification;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.SchedulerUpdate;
import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.UniformSchedulerSpecification;
import edu.brown.cs.systems.retro.throttling.schedulers.MClockScheduler;
import edu.brown.cs.systems.retro.throttling.schedulers.NullScheduler;
import edu.brown.cs.systems.tracing.Utils;

/**
 * API for accessing throttling points for the current process
 * 
 * @author a-jomace
 *
 */
public class RetroSchedulers {

    private static final String DEFAULT_SCHEDULER_KEY = "retro.throttling.default-scheduler";
    private static final String CUSTOM_SCHEDULERS_KEY = "retro.throttling.schedulers";

    /** To force subscribe to updates */
    public static void init() {
    }

    // Receives updates via pubsub
    private static final SchedulerUpdateCallback updatesCallback = new SchedulerUpdateCallback();
    private static Map<String, SchedulerSpecification> latestSpecs = new HashMap<String, SchedulerSpecification>();

    private static final ConcurrentMap<String, Scheduler> schedulers = new ConcurrentHashMap<String, Scheduler>();

    /**
     * This method is used to create all actual scheduler implementations.
     * Change the behavior here to change whatever type of scheduler is actually
     * used.
     */
    private static Scheduler createPoint(String schedulerName) {
        Config config = ConfigFactory.load();

        // The default scheduler type to use
        String schedulerType = config.getString(DEFAULT_SCHEDULER_KEY);

        // Check to see whether this named throttling point is individually
        // configured
        String customConfigPath = ConfigUtil.joinPath(schedulerName);
        if (config.getConfig(CUSTOM_SCHEDULERS_KEY).hasPath(customConfigPath)) {
            schedulerType = config.getConfig(CUSTOM_SCHEDULERS_KEY).getString(customConfigPath);
        }

        // Get the instance and throw an exception if incorrectly configured
        Scheduler instance = getSchedulerInstance(schedulerType, schedulerName);
        if (instance == null) {
            throw new IllegalArgumentException("Invalid throttling point type for " + schedulerName + ": " + schedulerType);
        }
        return instance;
    }

    /**
     * Returns a scheduler of the specified type, or null if the type is invalid
     */
    private static Scheduler getSchedulerInstance(String schedulerType, String schedulerName) {
        if (schedulerType.equals("default")) {
            return new MClockScheduler(schedulerName, 3);
        } else if (schedulerType.equals("none")) {
            return new NullScheduler(schedulerName);
        } else if (schedulerType.startsWith("mclock")) {
            String[] splits = schedulerType.split("-");
            int concurrency = Integer.parseInt(splits[1]);
            return new MClockScheduler(schedulerName, concurrency);
        }
        return null;
    }

    /**
     * Create or retrieve the throttling point with the specified name
     * 
     * @param throttlingPointName
     *            The name of the throttling point to get
     * @return A ThrottlingPoint instance
     */
    public static Scheduler get(String schedulerName) {
        Scheduler scheduler = schedulers.get(schedulerName);
        if (scheduler == null) {
            synchronized (schedulers) {
                scheduler = schedulers.get(schedulerName);
                if (scheduler == null) {
                    scheduler = createPoint(schedulerName);
                    schedulers.put(schedulerName, scheduler);
                    refresh(schedulerName);
                }
            }
        }
        return scheduler;
    }

    public static synchronized void refresh(String pointName) {
        update(pointName, latestSpecs.get(pointName));

    }

    public static synchronized void update(String schedulerName, SchedulerSpecification spec) {
        // Update any schedulers with the given name to use this new spec
        if (schedulers.containsKey(schedulerName)) {
            Scheduler s = schedulers.get(schedulerName);
            if (spec == null) {
                s.clear();
            } else {
                s.update(spec);
            }
        }

        // Save the spec
        if (spec == null) {
            latestSpecs.remove(schedulerName);
        } else {
            latestSpecs.put(schedulerName, spec);
        }
    }

    public static synchronized void clearAll() {
        for (Scheduler s : schedulers.values()) {
            s.clear();
        }
        latestSpecs.clear();
    }

    private static synchronized void processUpdate(SchedulerUpdate update) {
        if (update == null)
            return;

        for (UniformSchedulerSpecification s : update.getUniformSpecList())
            update(s.getName(), s.getSpecification());

        for (PerInstanceSchedulerSpecification s : update.getInstanceSpecList())
            if (s.getHost().equals(Utils.getHost()) && s.getPid() == Utils.getProcessID())
                update(s.getName(), s.getSpecification());

        if (update.hasClearAll() && update.getClearAll() == true)
            clearAll();
    }

    /**
     * Handles throttling point rate updates sent via pubsub
     * 
     * @author a-jomace
     */
    private static class SchedulerUpdateCallback extends Subscriber<SchedulerUpdate> {

        public SchedulerUpdateCallback() {
            String topic = ConfigFactory.load().getString("retro.throttling.schedulertopic");
            PubSub.subscribe(topic, this);
        }

        @Override
        protected void OnMessage(SchedulerUpdate update) {
            processUpdate(update);
        }

    }

}
