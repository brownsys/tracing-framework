package edu.brown.cs.systems.baggage;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageImpl;

public class BaggageImplCurvePerf {

    private static final DecimalFormat format = new DecimalFormat("#.##");

    private void printTitle(String title, String description) {
        System.out.println(title +" | " + description + ":");
    }
    private void printResults(double[] durations, double count, double[] cycleslist, String[] labels) {
        String timestr = "", avgstr = "", cpustr = "", labelstr = "";
        assert durations.length == cycleslist.length;
        int sampleAmount = durations.length; 
        for (int i = 0; i < sampleAmount; i++) {
            if (i > 0) {
                timestr += ", ";
                avgstr += ", ";
                cpustr += ", ";
                labelstr += ", ";
            }
            timestr +=  format.format(durations[i] / 1000000000.0);
            avgstr += format.format(durations[i] / count);
            cpustr += format.format(cycleslist[i] / count);
            labelstr += labels[i];
        }
        System.out.println("  Time (s):     [" + timestr + "]");
        System.out.println("  Count:         " + count);
        System.out.println("  Avg (ns):     [" + avgstr + "]");
        System.out.println("  CPU (cpu ns): [" + cpustr + "]");
        System.out.println("  Label:        [" + labelstr + "]");
    }

    private static final ThreadMXBean tbean = ManagementFactory
            .getThreadMXBean();

    @Test
    public void testAddValueCurve() {
        printTitle("ADD", "add a value with pre-existing 0, ..., 127 values");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("key".getBytes());
        ByteString myvalue = ByteString.copyFrom("v".getBytes());
        int perIteration = 50000;
        int expSize = 8;
        
        double[] durations = new double[expSize], cycleslist = new double[expSize];
        String[] labels = new String[expSize]; 
        for (int k = 0; k < expSize; k++) {
            int baseCount = (1 << k) - 1;
            BaggageImpl[] b = new BaggageImpl[perIteration];
            for (int j = 0; j < perIteration; j++) {
                b[j] = new BaggageImpl();
                for (int i = 0; i < baseCount; i++)
                    b[j].add(myname, mykey, ByteString.copyFrom(Integer.toString(i).getBytes()));
            }
            long startcpu = tbean.getCurrentThreadCpuTime();
            long start = System.nanoTime();
            for (int i = 0; i < perIteration; i++) {
                b[i].add(myname, mykey, myvalue);
            }
            durations[k] = System.nanoTime() - start;
            cycleslist[k] = tbean.getCurrentThreadCpuTime() - startcpu;
            labels[k] = Integer.toString(baseCount);
        }
        printResults(durations, perIteration, cycleslist, labels);
    }

}
