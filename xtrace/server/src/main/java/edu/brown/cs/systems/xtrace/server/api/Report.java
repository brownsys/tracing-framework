package edu.brown.cs.systems.xtrace.server.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.minidev.json.JSONObject;

/**
 * Interface for representing a report.
 */
public interface Report {

    public String getTaskID();

    public boolean hasTags();

    public boolean hasTitle();

    public List<String> getTags();

    public String getTitle();

    public JSONObject jsonRepr();

    void writeDelimitedTo(OutputStream output) throws IOException;

}
