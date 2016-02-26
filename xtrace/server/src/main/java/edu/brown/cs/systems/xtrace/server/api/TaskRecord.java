package edu.brown.cs.systems.xtrace.server.api;

import java.util.List;

public class TaskRecord {
    private String taskId;
    private long firstSeen;
    private long lastUpdated;
    private int numReports;
    private String title;
    private List<String> tags;

    public TaskRecord(String taskId, long firstSeen, long lastUpdated, int numReports, String title, List<String> tags) {
        this.taskId = taskId;
        this.firstSeen = firstSeen;
        this.lastUpdated = lastUpdated;
        this.numReports = numReports;
        this.title = title;
        this.tags = tags;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public int getNumReports() {
        return numReports;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTags() {
        return tags;
    }
}
