package edu.brown.cs.systems.xtrace;

import java.util.Set;

import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;

public class XTraceSettings {

    private static XTraceSettings instance;

    private static XTraceSettings instance() {
        if (instance == null) {
            synchronized (XTraceSettings.class) {
                if (instance == null) {
                    instance = new XTraceSettings();
                }
            }
        }
        return instance;
    }

    public final boolean on = ConfigFactory.load().getBoolean("xtrace.client.reporting.on");
    public final boolean defaultEnabled = ConfigFactory.load().getBoolean("xtrace.client.reporting.default");
    public final boolean discoveryMode = ConfigFactory.load().getBoolean("xtrace.client.reporting.discoverymode");
    public final boolean traceMain = ConfigFactory.load().getBoolean("xtrace.client.tracemain");
    public final Set<String> classesEnabled = Sets.newHashSet(ConfigFactory.load().getStringList("xtrace.client.reporting.enabled"));
    public final Set<String> classesDisabled = Sets.newHashSet(ConfigFactory.load().getStringList("xtrace.client.reporting.disabled"));

    public static boolean discoveryMode() {
        return instance().discoveryMode;
    }

    public static boolean traceMainMethods() {
        return instance().traceMain;
    }
    
    public static boolean On() {
        return instance().on;
    }

    /**
     * If reporting is turned off, returns false. If the logging class is in the
     * disabled set, returns false. If logging is enabled by default, returns
     * true. If the logging class is in the enabled set, returns true.
     * Otherwise, returns false
     * 
     * @param loggingClass
     *            The logging class to check
     * @return true if the class is allowed to log, false otherwise.
     */
    public static boolean Enabled(String loggingClass) {
        if (!On()) {
            return false;
        }
        if (instance().classesDisabled.contains(loggingClass)) {
            return false;
        }
        if (instance().defaultEnabled) {
            return true;
        }
        if (instance().classesEnabled.contains(loggingClass)) {
            return true;
        }
        return false;
    }

}
