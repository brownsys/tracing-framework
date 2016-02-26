package edu.brown.cs.systems.retro.aggregation.aggregators;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.systems.retro.aggregation.Callback;
import edu.brown.cs.systems.retro.aggregation.ClusterResources;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;
import edu.brown.cs.systems.retro.aggregation.Report.TenantOperationReport;
import edu.brown.cs.systems.retro.aggregation.Resource.Type;

public class Counter extends Callback {

    private static final int tenantId = 1;
    private static volatile boolean recording = false;
    private static final long measurement_duration = 30; // seconds
    private static final int iterations = 20;

    public static void main(String[] args) throws InterruptedException, IOException {
        Counter c = new Counter();
        ClusterResources.subscribeToAll(c);
        System.out.println("Hit any key to start");
        System.in.read();
        recording = true;
        System.out.println("Begin\tEnd\tDuration\tType\tAvg Throughput (req/s)\tAvg Latency (ms");
        for (int i = 0; i < iterations; i++) {
            long begin = System.currentTimeMillis();
            Thread.sleep(measurement_duration * 1000);
            long end = System.currentTimeMillis();
            synchronized (c) {
                c.print(begin, end);
                c.totalfinished = new HashMap<Type, Long>();
                c.totallatency = new HashMap<Type, Long>();
                c.num_hdfs_reports = 0;
            }
        }
    }

    double num_hdfs_reports = 0;
    Map<Type, Long> totalfinished = new HashMap<Type, Long>();
    Map<Type, Long> totallatency = new HashMap<Type, Long>();

    @Override
    protected void OnMessage(ResourceReport r) {
        synchronized (this) {
            if (!recording)
                return;
            Type t = r.getResource();
            for (TenantOperationReport tr : r.getTenantReportList()) {
                if (tr.getTenantClass() == tenantId) {
                    if (!totalfinished.containsKey(t)) {
                        totalfinished.put(t, 0L);
                        totallatency.put(t, 0L);
                    }
                    totalfinished.put(t, totalfinished.get(t) + tr.getNumFinished());
                    totallatency.put(t, totallatency.get(t) + tr.getTotalLatency());
                }
            }
            if (t == Type.HDFSREQUEST)
                num_hdfs_reports++;
        }
    }

    public synchronized void print(long begin, long end) {
        for (Type t : totalfinished.keySet()) {
            System.out.println(begin + "\t" + end + "\t" + (end - begin) + "\t" + t + "\t" + (totalfinished.get(t) / ((double) num_hdfs_reports)) + "\t"
                    + (totallatency.get(t) / (totalfinished.get(t))));
        }
    }

}
