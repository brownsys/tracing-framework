package edu.brown.cs.systems.xtrace;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageContents;

/**
 * Interface to get and set X-Trace task IDs and event IDs in the tracing
 * plane's baggage
 */
public class XTraceBaggageInterface {

    private static final Logger log = LoggerFactory.getLogger(XTraceBaggageInterface.class);

    /** For now, X-Trace is statically assigned the namespace for the byte 1 **/
    public static final ByteString XTRACE_BAGGAGE_NAMESPACE = ByteString.copyFrom(new byte[] { 0x01 });
    
    /** First of two baggage fields is the task ID **/
    public static final ByteString TASK_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x01 });
    
    /** Second of two baggage fields is the parent event ID (s) field **/
    public static final ByteString PARENT_EVENT_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x02 });

    /**
     * Looks at this thread's current baggage to determine whether an X-Trace
     * task ID is being propagated
     * 
     * @return true if an X-Trace task ID is being propagated by the current
     *         execution
     */
    public static boolean hasTaskID() {
        return BaggageContents.contains(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD);
    }

    /**
     * Looks at this thread's current baggage to determine whether any X-Trace
     * parent event IDs are being propagated
     * 
     * @return true if X-Trace parent event IDs are being propagated
     */
    public static boolean hasParents() {
        return BaggageContents.contains(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD);
    }

    /**
     * Looks at this thread's current baggage, and returns the X-Trace task ID
     * if it contains one
     * 
     * @return the X-Trace Task ID for the current execution, or 0 if none was
     *         found
     */
    public static long getTaskID() {
        // Get task IDs from the baggage
        Set<ByteString> taskIds = BaggageContents.get(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD);

        // Under normal circumstances, either one 8-byte task ID, or no task ID
        if (taskIds.size() == 1) {
            ByteString taskIdBs = taskIds.iterator().next();
            if (taskIdBs.size() == 8) {
                return taskIdBs.asReadOnlyByteBuffer().getLong();
            } else {
                log.warn("Encountered task ID with incorrect length {}: {}", taskIdBs.size(), taskIdBs);
            }
        } else if (taskIds.size() > 1) {
            log.warn("Execution has {} task IDs.  This could be indicative of an error in tracing", taskIds.size());
            BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD, taskIds.iterator().next());
            return getTaskID();
        } else if (XTraceSettings.discoveryMode()) {
            XTrace.startTask(true);
            return getTaskID();
        }
        return 0;
    }

    /**
     * Set the task ID in the thread's current baggage to the specified task ID
     * 
     * @param taskId
     *            The task ID to set for the current execution
     */
    public static void setTaskID(long taskId) {
        byte[] taskIdBytes = ByteBuffer.allocate(8).putLong(taskId).array();
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, TASK_ID_BAGGAGE_FIELD, ByteString.copyFrom(taskIdBytes));
    }

    /**
     * Looks at this thread's current baggage, and returns the X-Trace parent
     * event IDs if there are any
     * 
     * @return an array containing the X-Trace parent event IDs of the current
     *         execution, possibly empty
     */
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
                parentEventIds.add(parentEventId.asReadOnlyByteBuffer().getLong());
            } else {
                log.warn("Encountered parent event ID with incorrect length {}: {}", parentEventId.size(), parentEventId);
            }
        }
        return parentEventIds;
    }

    /**
     * Set the parent event ID in the thread's current baggage to the specified
     * event ID
     * 
     * @param parentEventId
     *            The event ID to set for the current execution
     */
    public static void setParentEventId(long parentEventId) {
        byte[] parentEventIdBytes = ByteBuffer.allocate(8).putLong(parentEventId).array();
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD, ByteString.copyFrom(parentEventIdBytes));
    }

    /**
     * Set the parent event IDs in the thread's current baggage to the specified
     * event IDs
     * 
     * @param parentEventIds
     *            The event IDs to set for the current execution
     */
    public static void setParentEventIds(long... parentEventIds) {
        Set<ByteString> parentEventIdSet = Sets.newHashSetWithExpectedSize(parentEventIds.length);
        for (int i = 0; i < parentEventIds.length; i++) {
            byte[] parentEventIdBytes = ByteBuffer.allocate(8).putLong(parentEventIds[i]).array();
            parentEventIdSet.add(ByteString.copyFrom(parentEventIdBytes));
        }
        BaggageContents.replace(XTRACE_BAGGAGE_NAMESPACE, PARENT_EVENT_ID_BAGGAGE_FIELD, parentEventIdSet);
    }

}
