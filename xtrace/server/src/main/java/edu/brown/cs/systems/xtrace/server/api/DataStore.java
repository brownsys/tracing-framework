package edu.brown.cs.systems.xtrace.server.api;

import java.util.Iterator;

/**
 * Defines the interface for persisting the reports themselves. Distinct from
 * MetadataStore which just persists statistics about tasks
 * 
 * @author Jonathan Mace
 */
public interface DataStore {

    public void reportReceived(Report r);

    public Iterator<Report> getReports(String taskId);

    public void shutdown();

}
