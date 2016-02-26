package edu.brown.cs.systems.xtrace.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReportv4;
import edu.brown.cs.systems.xtrace.server.api.Report;

/**
 * X-Trace version 4 representation of X-Trace reports. Directly serializes
 * protocol buffers messages to disk. Not human readable but substantially more
 * efficient
 */
public class ReportImpl implements Report {

    private final String taskID;
    private final XTraceReportv4 event;

    public ReportImpl(XTraceReportv4 event) {
        this.event = event;
        this.taskID = String.format("%16s", Long.toHexString(event.getTaskId())).replace(' ', '0');
    }

    @Override
    public String getTaskID() {
        return taskID;
    }

    @Override
    public boolean hasTags() {
        return event.getTagsCount() > 0;
    }

    @Override
    public boolean hasTitle() {
        return false;
    }

    @Override
    public List<String> getTags() {
        return event.getTagsList();
    }

    @Override
    public String getTitle() {
        return "X-Trace Task";
    }

    @Override
    public String toString() {
        return event.toString();
    }

    @Override
    public void writeDelimitedTo(OutputStream output) throws IOException {
        event.writeDelimitedTo(output);
    }

    @Override
    public JSONObject jsonRepr() {
        JSONObject json = new JSONObject();

        if (event.hasTaskId())
            json.put("taskID", this.taskID);
        if (event.hasTimestamp())
            json.put("Timestamp", event.getTimestamp());
        if (event.hasHrt())
            json.put("HRT", event.getHrt());
        if (event.hasCycles())
            json.put("Cycles", event.getCycles());
        if (event.hasHost())
            json.put("Host", event.getHost());
        if (event.hasProcessId())
            json.put("ProcessID", event.getProcessId());
        if (event.hasProcessName())
            json.put("ProcessName", event.getProcessName());
        if (event.hasThreadId())
            json.put("ThreadID", event.getThreadId());
        if (event.hasThreadName())
            json.put("ThreadName", event.getThreadName());
        if (event.hasAgent())
            json.put("Agent", event.getAgent());
        if (event.hasSource())
            json.put("Source", event.getSource());
        if (event.hasLabel())
            json.put("Label", event.getLabel());
        for (int i = 0; i < event.getKeyCount(); i++) {
            json.put(event.getKey(i), event.getValue(i));
        }
        if (event.getTagsCount() > 0) {
            JSONArray tags = new JSONArray();
            tags.addAll(event.getTagsList());
            json.put("Tag", tags);
        }
        json.put("Title", "X-Trace Task");
        if (event.hasTenantClass())
            json.put("TenantClass", event.getTenantClass());
        if (event.hasEventId())
            json.put("EventID", Long.toString(event.getEventId()));
        if (event.getParentEventIdCount() > 0) {
            JSONArray parents = new JSONArray();
            for (int i = 0; i < event.getParentEventIdCount(); i++) {
                parents.add(Long.toString(event.getParentEventId(i)));
            }
            json.put("ParentEventID", parents);
        }

        return json;
    }

}