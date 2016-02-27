package edu.brown.cs.systems.xtrace.reporting;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;

import com.google.common.collect.Lists;

import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReportv4;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReportv4.Builder;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;

public class XTraceReport {

    private static final String host = Utils.getHost();
    private static final int procid = Utils.getProcessID();

    /**
     * The XTraceReportv4 message builder for this report
     */
    public final Builder builder = XTraceReportv4.newBuilder();

    private XTraceReport() {
    }

    /**
     * Adds standard fields to this report, such as hostname, timestamp, etc.
     * 
     * @return This report, with additional fields added
     */
    public XTraceReport addStandardFields() {
        builder.setHost(host);
        builder.setProcessId(procid);
        builder.setProcessName(Utils.getProcessName());
        builder.setThreadId((int) Thread.currentThread().getId());
        builder.setThreadName(Thread.currentThread().getName());
        builder.setTimestamp(System.currentTimeMillis());
        builder.setHrt(System.nanoTime());
        return this;
    }

    /**
     * Turns this report into an X-Trace "event", by adding the current thread's
     * parent event IDs to the report, generating a random event ID for the
     * report, and updating the current thread's parent event IDs to this
     * report's event ID
     * 
     * @return This report, with additional fields added
     */
    public XTraceReport makeXTraceEvent() {
        long taskId = XTraceBaggageInterface.getTaskID();
        long eventId = XTrace.randomId();
        Collection<Long> parentIds = XTraceBaggageInterface.getParentEventIds();
        setXTrace(taskId, eventId, parentIds);
        XTraceBaggageInterface.setParentEventId(eventId);
        return this;
    }
    
    public XTraceReport setXTrace(long taskId, long eventId, Collection<Long> parentEventIds) {
        builder.setTaskId(taskId);
        builder.setEventId(eventId);
        builder.addAllParentEventId(parentEventIds);
        return this;
    }

    /** Add information to the report such as source line, etc. */
    public void setJoinPoint(JoinPoint.StaticPart joinPoint) {
        if (joinPoint == null) {
            return;
        }
        builder.setSource(joinPoint.getSourceLocation().toString());
    }

    /**
     * Set the label of this report to the provided message. The message can
     * have variable-substitution characters {} for replacement with the
     * provided vars. Further variables can be specified as key-value pairs to
     * be added to the report. See the documentation of XTraceLogger for more
     * details.
     */
    public XTraceReport setMessage(String message, Object... vars) {
        int varsIndex = 0;
        if (message != null) {
            // Split the string based on the replacement variable {}
            String[] tokens = StringUtils.splitByWholeSeparatorPreserveAllTokens(message, "{}");
//            System.out.println(tokens.length + " tokens");
//            for (int i = 0; i < tokens.length; i++) {
//                System.out.println("\"" + tokens[i] + "\" ");
//            }

            // Start constructing the message
            StringBuilder replaced = new StringBuilder();
            if (tokens.length > 0) {
                replaced.append(tokens[0]);
            }

            // Replace instances of {} with a var
            while (varsIndex + 1 < tokens.length && varsIndex < vars.length) {
                replaced.append(vars[varsIndex] == null ? "null" : vars[varsIndex].toString());
                replaced.append(tokens[varsIndex + 1]);
                varsIndex++;
            }

            builder.setLabel(replaced.toString());
        }

        // Put remaining vars as kv pairs
        while (varsIndex + 1 < vars.length) {
            put(vars[varsIndex++], vars[varsIndex++]);
        }

        return this;
    }

    /**
     * Add a key-value pair to the report
     * 
     * @param key
     *            The key to add to the report. If this is null, the key-value
     *            pair will be ignored
     * @param value
     *            The value to add for the key. If this is null, the string
     *            "null" will be added
     * @return This report, with the added k-v pair
     */
    public XTraceReport put(Object key, Object value) {
        if (key != null) {
            builder.addKey(key.toString());
            builder.addValue(value == null ? "null" : value.toString());
        }
        return this;
    }

    /**
     * Create a new, empty report
     */
    public static XTraceReport create() {
        return new XTraceReport();
    }

    @Override
    public String toString() {
        return builder.build().toString();
    }

    private static final List<Decorator> decorators = Lists.newArrayList();

    /**
     * A decorator is something clients can add to add fields to XTrace reports
     */
    public static interface Decorator {
        public void decorate(XTraceReport report);
    }

    /**
     * Add a report decorator. The decorate function will be called for every
     * X-Trace report that is sent
     */
    public static synchronized void addDecorator(Decorator d) {
        decorators.add(d);
    }

    public static synchronized void removeDecorator(Decorator d) {
        decorators.remove(d);
    }

    /**
     * Apply any registered decorators to this report
     */
    public XTraceReport applyDecorators() {
        for (Decorator d : decorators) {
            try {
                d.decorate(this);
            } catch (Exception e) {
                // Do nothing
            }
        }
        return this;
    }

}
