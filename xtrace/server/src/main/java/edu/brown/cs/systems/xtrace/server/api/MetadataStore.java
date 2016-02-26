package edu.brown.cs.systems.xtrace.server.api;

import java.util.Collection;
import java.util.List;

/**
 * Defines the interface for persisting task metadata to a database for querying
 * for web APIs.
 * 
 * @author Jonathan Mace
 */
public interface MetadataStore {

    public void reportReceived(Report report);

    public List<TaskRecord> getTasksSince(long startTime, int offset, int limit);

    public List<TaskRecord> getLatestTasks(int offset, int limit);

    public List<TaskRecord> getTasksByTag(String tag, int offset, int limit);

    public List<TaskRecord> getTasksByTitle(String title, int offset, int limit);

    public List<TaskRecord> getTasksByTitleSubstring(String title, int offset, int limit);

    public Collection<String> getConcurrentTasks(String taskId);

    public Collection<String> getTags(String taskId);

    public int numTasks();

    public int numReports();

    public void shutdown();

}
