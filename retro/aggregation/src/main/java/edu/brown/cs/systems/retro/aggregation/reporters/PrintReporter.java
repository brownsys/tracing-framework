package edu.brown.cs.systems.retro.aggregation.reporters;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;

/**
 * Prints reports to a tsv file
 * 
 * @author a-jomace
 */
public class PrintReporter implements Runnable, Reporter {

    // Config
    private final Config config;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // Can write different aggregators to different files
    private final Map<ResourceAggregator, PrintWriter> resources = new HashMap<ResourceAggregator, PrintWriter>();

    // Only one writer per filename
    private final Map<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

    public PrintReporter(Config config) {
        this.config = config;

        long interval = config.getLong("resource-reporting.reporting.interval");

        // Determine the first reporting interval - as much as possible, align
        // with clock boundaries
        long initial_delay = 2 * interval - (System.currentTimeMillis() % interval);

        // Schedule at a fixed rate
        executor.scheduleAtFixedRate(this, initial_delay, interval, TimeUnit.MILLISECONDS);
        System.out.println("PrintReporter initialized");
    }

    public String getDefaultFilename() {
        return config.getString("resource-reporting.reporting.printer.filename");
    }

    @Override
    public synchronized void register(ResourceAggregator r) {
        String filename = getDefaultFilename();
        if (!writers.containsKey(filename)) {
            try {
                writers.put(filename, new PrintWriter(filename));
                register(writers.get(filename), r);
            } catch (Exception e) {
                // do nothing and return
                System.out.println("Unable to register " + r + ": " + e);
                return;
            }
        }
    }

    public synchronized void register(PrintWriter writer, ResourceAggregator r) {
        if (!resources.containsKey(r)) {
            resources.put(r, writer);
            System.out.println("Printing " + r + " reports");
        }
    }

    @Override
    public final void run() {
        synchronized (this) {
            for (ResourceAggregator r : resources.keySet()) {
                if (r.enabled()) {
                    PrintWriter writer = resources.get(r);
                    try {
                        ResourceReport report = r.getReport();
                        if (report != null) {
                            String prefix = String.format("%d\t%d\t%s\t%s\t%s\t%d\t%s", report.getStart(), report.getEnd(), report.getResource().toString(),
                                    report.getResourceID(), report.getMachine(), report.getProcessID(), report.getProcessName());
                            for (TenantOperationReport tr : report.getTenantReportList()) {
                                writer.printf("%s\t%s\t%d\t%d\t%d\t%d\n", prefix, tr.getOperation().toString(), tr.getTenantClass(), tr.getNumFinished(),
                                        tr.getTotalLatency(), tr.getTotalWork());
                            }
                            writer.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void printHeaders(PrintWriter w) {
        w.println("start\tend\tresource\tresourceid\tmachine\tprocid\tprocname\top\ttenant\tcount\tlatency\twork");
    }

    @Override
    public void reportImmediately(ImmediateReport.Builder report) {
        // print reporter ignores immediate reports
    }

}
