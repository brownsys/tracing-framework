package edu.brown.cs.systems.xtrace;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class XTraceSettings {

    public static final Config CONFIG = ConfigFactory.load();

    public static final int WEBUI_PORT = CONFIG.getInt("xtrace.server.webui.port");

    public static final String PUBSUB_TOPIC = CONFIG.getString("xtrace.pubsub.topic");

    public static final int DATABASE_UPDATE_INTERVAL = CONFIG.getInt("xtrace.server.database-update-interval-ms");

    public static final String DATASTORE_DIRECTORY = CONFIG.getString("xtrace.server.datastore.dir");
    public static final int DATASTORE_BUFFER_SIZE = CONFIG.getInt("xtrace.server.datastore.buffer-size");
    public static final int DATASTORE_CACHE_SIZE = CONFIG.getInt("xtrace.server.datastore.cache-size");
    public static final int DATASTORE_CACHE_TIMEOUT = CONFIG.getInt("xtrace.server.datastore.cache-timeout");

}
