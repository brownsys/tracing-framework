package edu.brown.cs.systems.pivottracing;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class PivotTracingConfig {
    
    private static final Config CONFIG = ConfigFactory.load();
    public static final String COMMANDS_TOPIC = CONFIG.getString("pivot-tracing.pubsub.commands_topic");
    public static final String STATUS_TOPIC = CONFIG.getString("pivot-tracing.pubsub.status_topic");
    public static final String RESULTS_TOPIC = CONFIG.getString("pivot-tracing.pubsub.results_topic");

}
