package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;
import java.io.PrintWriter;

import edu.brown.cs.systems.retro.aggregation.Callback;
import edu.brown.cs.systems.retro.aggregation.ClusterResources;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;

public class Recorder extends Callback {

    private final PrintWriter writer;

    public Recorder(PrintWriter writer) {
        this.writer = writer;
        print(getReportTSVHeader());
        ClusterResources.subscribeToHDFSRequestReports(this);
    }

    public static String getReportTSVHeader() {
        return "start\tend\tmachine\tresource\tresourceID\tprocessID\tprocessName\tutilization\ttenant\toperation\tnStarted\tnFinished\twork\tlatencyPerWork\tworkPerOp\tlatency[ms]\tlatencyPerOp[ms]\toccupancy\n";
    }

    public static String reportToTSV(ResourceReport report) {
        StringBuilder sb = new StringBuilder();

        String prefix = report.getStart() + "\t" + report.getEnd() + "\t" + report.getMachine() + "\t" + report.getResource() + "\t" + report.getResourceID()
                + "\t" + report.getProcessID() + "\t" + report.getProcessName() + "\t" + report.getUtilization();

        for (TenantOperationReport r : report.getTenantReportList())
            sb.append(prefix + "\t" + r.getTenantClass() + "\t" + r.getOperation() + "\t" + r.getNumStarted() + "\t" + r.getNumFinished() + "\t"
                    + r.getTotalWork() + "\t" + (r.getTotalLatency() / (double) r.getTotalWork()) + "\t" + r.getTotalWork() / (double) r.getNumFinished()
                    + "\t" + r.getTotalLatency() / 1000.0 / 1000.0 + "\t" + (r.getTotalLatency() / 1000.0 / 1000.0 / (double) r.getNumFinished()) + "\t"
                    + r.getTotalOccupancy() + "\n");

        return sb.toString();
    }

    private void print(String s) {
        this.writer.print(s);
        this.writer.flush();
    }

    @Override
    protected synchronized void OnMessage(ResourceReport report) {
        if (report.getResource() == Type.HDFSREQUEST)
            print(reportToTSV(report));
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Must specify a filename to record reports to");
            return;
        }
        String filename = args[0];
        System.out.println("Recording resource reports to " + filename);
        Recorder r = new Recorder(new PrintWriter(filename));
    }

}
