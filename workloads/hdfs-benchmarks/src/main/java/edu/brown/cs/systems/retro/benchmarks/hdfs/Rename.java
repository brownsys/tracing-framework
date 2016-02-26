package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;

public class Rename implements Benchmark {

    @Override
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        String prefix = String.format("/retro/benchmarks/rename/pid%d/worker%d/file", pid, wid);

        int i = 1;
        Path file = new Path(prefix + i++);

        try {
            FSUtils.createAndWriteFile(hdfs, file, 0, 1024 * 1024 * 64, 1);
        } catch (IOException e) {
            System.out.println("Rename worker unable to create file for rename: " + file);
            e.printStackTrace();
            return;
        }

        int nbags = -1, nper = -1;
        boolean propagateBaggage = opts.length >= 2;
        if (propagateBaggage) {
            try {
                int tmpA = Integer.parseInt(opts[0]);
                int tmpB = Integer.parseInt(opts[1]);
                nbags = tmpA;
                nper = tmpB;
            } catch (NumberFormatException e) {
                System.err.println("warning: malformed opts, not propagating baggage");
                propagateBaggage = false;
            }
        }

        while (!Thread.currentThread().isInterrupted()) {
            Path next = new Path(prefix + i++);
            try {
                BenchmarkUtils.SetTenant(1);
                BenchmarkUtils.PopulateCurrentBaggage(nbags, nper);

                long begin = System.nanoTime();
                boolean success = FSUtils.renameFile(hdfs, file, next);
                long duration = System.nanoTime() - begin;
                aggregator.startedAndFinished(Operation.RENAME, 1, 1, duration);

                if (success) {
                    file = next;
                } else {
                    System.out.println("Warning: was unable to rename " + file + " to " + next);
                }
            } catch (IOException e) {
                System.out.println("Warning: IOException attempting to rename " + file + " to " + next);
            } finally {
                BenchmarkUtils.StopTracing();
            }
        }
    }

}
