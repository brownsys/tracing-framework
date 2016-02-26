package edu.brown.cs.systems.retro.aggregation.aggregators;

import java.io.IOException;
import java.io.PrintWriter;

import edu.brown.cs.systems.retro.aggregation.Callback;
import edu.brown.cs.systems.retro.aggregation.ClusterResources;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;

public class CounterRealtime extends Callback {

    private static final int tenantId = 1;
    private static volatile boolean recording = false;
    private static final String outputfile = "requests-hdfs.tab";
    private static final String outputfileother = "requests-other.tab";
    private static PrintWriter writer, otherwriter;

    public static void main(String[] args) throws InterruptedException, IOException {
        writer = new PrintWriter(outputfile);
        otherwriter = new PrintWriter(outputfileother);
        System.out.println("Writing to " + outputfile);
        print("Begin\tEnd\ttotalFinished\ttotalLatency\ttotalLatencySquared");
        printOther("Begin\tEnd\tRsrc\tid\top\ttotalFinished\ttotalLatency\ttotalLatencySquared");
        CounterRealtime c = new CounterRealtime();
        ClusterResources.subscribeToAll(c);
    }

    private static void print(String msg) {
        System.out.println(msg);
        writer.println(msg);
        writer.flush();
    }

    private static void printOther(String msg) {
        otherwriter.println(msg);
        otherwriter.flush();
    }

    @Override
    protected void OnMessage(ResourceReport r) {
        synchronized (this) {
            Type t = r.getResource();
            if (t == Type.HDFSREQUEST) {
                for (TenantOperationReport tr : r.getTenantReportList()) {
                    if (tr.getTenantClass() == tenantId) {
                        print(r.getStart() + "\t" + r.getEnd() + "\t" + tr.getNumFinished() + "\t" + tr.getTotalLatency() + "\t" + tr.getTotalLatencySquared());
                    }
                }
            } else {
                for (TenantOperationReport tr : r.getTenantReportList()) {
                    if (tr.getTenantClass() == tenantId) {
                        printOther(r.getStart() + "\t" + r.getEnd() + "\t" + t + "\t" + r.getResourceID() + "\t" + tr.getOperation() + "\t"
                                + tr.getNumFinished() + "\t" + tr.getTotalLatency() + "\t" + tr.getTotalLatencySquared());
                    }
                }
            }
        }
    }

}
