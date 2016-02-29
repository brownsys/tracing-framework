package edu.brown.cs.systems.xtrace;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.tracing.ByteStrings;
import edu.brown.cs.systems.xtrace.logging.XTraceLoggingLevel;

/** Interface to get and set X-Trace task IDs and event IDs in the tracing plane's baggage */
public class XTraceBaggageInterface {

    private static final Logger log = LoggerFactory.getLogger(XTraceBaggageInterface.class);

    /** For now, X-Trace is statically assigned the namespace for the byte 1 **/
    public static final ByteString XTRACE_BAGGAGE_NAMESPACE = ByteString.copyFrom(new byte[] { 0x01 });

    /** First of two baggage fields is the task ID **/
    public static final ByteString TASK_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x01 });

    /** Second of two baggage fields is the parent event ID (s) field **/
    public static final ByteString PARENT_EVENT_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x02 });

    /** Extra baggage field for discovery mode **/
    public static final ByteString DISCOVERY_MODE_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x03 });

    /** Extra baggage field for logging level for wrapped commons / log4j loggers **/
    public static final ByteString LOGGING_LEVEL_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x04 });

    /** Looks at this thread's current baggage to determine whether an X-Trace task ID is being propagated
     * 
     * @return true if an X-Trace task ID is being propagated by the current execution */
    public static boolean hasTaskID() {
        return BaggageContents.contains(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD);
    }

    /** Looks at this thread's current baggage to determine whether any X-Trace parent event IDs are being propagated
     * 
     * @return true if X-Trace parent event IDs are being propagated */
    public static boolean hasParents() {
        return BaggageContents.contains(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD);
    }

    /** Looks at this thread's current baggage, and returns the X-Trace task ID if it contains one
     * 
     * @return the X-Trace Task ID for the current execution, or 0 if none was found */
    public static long getTaskID() {
        // Get task ID from the baggage
        Set<ByteString> taskIds = BaggageContents.get(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD);

        // Warn if multiple task IDs
        if (taskIds.size() > 1) {
            log.warn("Found multiple X-Trace task IDs, this is indicative of tracing context leak");
        }

        // Return a valid task ID
        for (ByteString taskId : taskIds) {
            if (taskId.size() == 8) {
                return ByteStrings.toLong(taskId);
            } else {
                log.warn("Found invalid X-Trace task ID: {}", taskId);
            }
        }

        // If we have no IDs use discovery mode ID if we're in discovery mode; otherwise 0
        if (XTraceSettings.discoveryMode()) {
            return getDiscoveryModeId();
        } else {
            return 0;
        }
    }

    /** Like a task ID, but we throw it away once we encounter an actual task */
    public static long getDiscoveryModeId() {
        // Must be in discovery mode to put id in baggage
        if (!XTraceSettings.discoveryMode()) {
            return 0;
        }

        // Get IDs from the baggage
        Set<ByteString> taskIds = BaggageContents.get(XTRACE_BAGGAGE_NAMESPACE, DISCOVERY_MODE_ID_BAGGAGE_FIELD);

        // Find the first valid ID, replace all the others with it in the baggage, and return it
        for (ByteString taskId : taskIds) {
            if (taskId.size() == 8) {
                BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, DISCOVERY_MODE_ID_BAGGAGE_FIELD, taskId);
                return ByteStrings.toLong(taskId);
            }
        }

        // No valid discovery ID; create one
        long newDiscoveryId = XTrace.discoveryId();
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, DISCOVERY_MODE_ID_BAGGAGE_FIELD,
                ByteStrings.copyFrom(newDiscoveryId));
        setParentEventId(0);
        return newDiscoveryId;
    }

    /** Set this request's logging level to the level provided. Any log4j / slf4j log messages will be proxied to xtrace
     * if they are greater or equal to this log level */
    public static void setLoggingLevel(XTraceLoggingLevel level) {
        ByteString bytes = ByteString.copyFrom(new byte[] { (byte) level.ordinal() });
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, LOGGING_LEVEL_BAGGAGE_FIELD, bytes);
    }

    /** Each request can set its own logging level that overrides the global default */
    public static XTraceLoggingLevel getLoggingLevel() {
        // Take the max of the default logging level and all logging levels in the baggage
        Set<ByteString> loggingLevels = BaggageContents.get(XTRACE_BAGGAGE_NAMESPACE, LOGGING_LEVEL_BAGGAGE_FIELD);
        int max = XTraceSettings.defaultLoggingLevel().ordinal();
        for (ByteString level : loggingLevels) {
            if (level.size() == 1) {
                max = Math.max(max, level.byteAt(0));
            }
        }
        // Keep within valid bounds
        max = Math.max(0, max);
        max = Math.min(max, XTraceLoggingLevel.values().length - 1);
        return XTraceLoggingLevel.values()[max];
    }

    /** Set the task ID in the thread's current baggage to the specified task ID
     * 
     * @param taskId The task ID to set for the current execution */
    public static void setTaskID(long taskId) {
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD, ByteStrings.copyFrom(taskId));
    }

    /** Looks at this thread's current baggage, and returns the X-Trace parent event IDs if there are any
     * 
     * @return an array containing the X-Trace parent event IDs of the current execution, possibly empty */
    public static Collection<Long> getParentEventIds() {
        // Get event IDs from the baggage
        Set<ByteString> eventIds = BaggageContents.get(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD);
        if (eventIds.size() == 0) {
            return Collections.emptySet();
        }

        // Can be zero or more 8-byte event IDs
        Collection<Long> parentEventIds = Lists.newArrayList();
        for (ByteString parentEventId : eventIds) {
            if (parentEventId.size() == 8) {
                parentEventIds.add(ByteStrings.toLong(parentEventId));
            } else {
                log.warn("Encountered parent event ID with incorrect length {}: {}", parentEventId.size(),
                        parentEventId);
            }
        }
        return parentEventIds;
    }

    /** Set the parent event ID in the thread's current baggage to the specified event ID
     * 
     * @param parentEventId The event ID to set for the current execution */
    public static void setParentEventId(long parentEventId) {
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD,
                ByteStrings.copyFrom(parentEventId));
    }

    /** Set the parent event IDs in the thread's current baggage to the specified event IDs
     * 
     * @param parentEventIds The event IDs to set for the current execution */
    public static void setParentEventIds(long... parentEventIds) {
        Set<ByteString> parentEventIdSet = Sets.newHashSetWithExpectedSize(parentEventIds.length);
        for (int i = 0; i < parentEventIds.length; i++) {
            parentEventIdSet.add(ByteStrings.copyFrom(parentEventIds[i]));
        }
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD, parentEventIdSet);
    }

}
