package edu.brown.cs.systems.retro.aggregation;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;

/**
 * Random utility class to turn reports to JSON.
 * 
 * @author jon
 *
 */
public class JSON {

    private static JSONObject tenantReportToJSON(TenantOperationReport report) {
        JSONObject json = new JSONObject();
        json.put("tenantClass", report.getTenantClass());
        if (report.hasOperation())
            json.put("operation", report.getOperation());
        json.put("numStarted", report.getNumStarted());
        json.put("numFinished", report.getNumFinished());
        json.put("totalWork", report.getTotalWork());
        json.put("totalWorkSquared", report.getTotalWorkSquared());
        json.put("totalLatency", report.getTotalLatency());
        json.put("totalLatencySquared", report.getTotalLatencySquared());
        json.put("totalOccupancy", report.getTotalOccupancy());
        json.put("totalOccupancySquared", report.getTotalOccupancySquared());
        return json;
    }

    public static String reportToJson(ResourceReport r) {
        JSONObject json = new JSONObject();

        json.put("start", r.getStart());
        json.put("end", r.getEnd());
        json.put("resource", r.getResource());
        if (r.hasResourceID())
            json.put("resourceID", r.getResourceID());
        json.put("machine", r.getMachine());
        json.put("processID", r.getProcessID());
        if (r.hasProcessName())
            json.put("processName", r.getProcessName());
        if (r.hasUtilization())
            json.put("utilization", r.getUtilization());

        JSONArray reports = new JSONArray();
        for (TenantOperationReport report : r.getTenantReportList()) {
            reports.add(tenantReportToJSON(report));
        }
        json.put("tenantReports", reports);

        return json.toString();
    }
}
