package edu.brown.cs.systems.xtrace;

import java.util.Set;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.xtrace.logging.XTraceLoggingLevel;

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
    
    public final boolean on, defaultEnabled, discoveryMode, traceMain;
    public final XTraceLoggingLevel defaultLoggingLevel, mainMethodLoggingLevel;
    public final Set<String> classesEnabled, classesDisabled;
    public final int recycleThreshold;

    public XTraceSettings() {
        Config config = ConfigFactory.load();
        on = config.getBoolean("xtrace.client.reporting.on");
        defaultEnabled = config.getBoolean("xtrace.client.reporting.default");
        discoveryMode = config.getBoolean("xtrace.client.reporting.discoverymode");
        traceMain = config.getBoolean("xtrace.client.tracemain");
        defaultLoggingLevel = XTraceLoggingLevel.valueOf(config.getString("xtrace.client.reporting.default_level").toUpperCase());
        mainMethodLoggingLevel = XTraceLoggingLevel.valueOf(config.getString("xtrace.client.tracemain_level").toUpperCase());
        classesEnabled = Sets.newHashSet(config.getStringList("xtrace.client.reporting.enabled"));
        classesDisabled = Sets.newHashSet(config.getStringList("xtrace.client.reporting.disabled"));
        recycleThreshold = config.getInt("xtrace.client.recycle-threshold");
    }
    
    public static XTraceLoggingLevel defaultLoggingLevel() {
        return instance().defaultLoggingLevel;
    }
    
    public static boolean discoveryMode() {
        return instance().discoveryMode;
    }
    
    public static int recycleThreshold() {
        return instance().recycleThreshold;
    }

    public static boolean traceMainMethods() {
        return instance().traceMain;
    }

    public static XTraceLoggingLevel mainMethodLoggingLevel() {
        return instance().mainMethodLoggingLevel;
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
