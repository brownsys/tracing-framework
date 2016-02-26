package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;

public class CreateWrite implements Benchmark {

    @Override
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        String prefix = String.format("/retro/benchmarks/create/pid%d/worker%d", pid, wid);

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

        Random r = new Random();
        long i = 1;
        while (!Thread.currentThread().isInterrupted()) {
            int a = r.nextInt(100);
            int b = r.nextInt(100);
            int c = r.nextInt(100);
            int d = r.nextInt(100);
            Path file = new Path(String.format("%s/%d/%d/%d/file-%d-%d", prefix, a, b, c, d, i++));
            try {
                BenchmarkUtils.SetTenant(1);
                BenchmarkUtils.PopulateCurrentBaggage(nbags, nper);

                long begin = System.nanoTime();
                FSDataOutputStream stream = FSUtils.createFile(hdfs, file, 1024 * 1024 * 64, 1);
                long duration = System.nanoTime() - begin;
                stream.close();
                aggregator.startedAndFinished(Operation.WRITE, 1, 1, duration);
            } catch (IOException e) {
                System.out.println("Warning: IOException attempting to create " + file);
            } finally {
                BenchmarkUtils.StopTracing();
            }
        }
    }

}
