package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;
import edu.brown.cs.systems.tracing.Utils;

public class OpenRead implements Benchmark {

    private static final int PATH_DEPTH = 20;
    private static final int FOLDERNAME_LENGTH = 4;
    private static final int NUM_FILES = 1000;

    @Override
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        String prefix = String.format("/retro/benchmarks/open/%s/%d/%d", Utils.getHost(), pid, wid);

        System.out.println("Creating 1000 files");
        
        // Create 1000 files
        Path[] files = new Path[NUM_FILES];

        Random r = new Random();
        for (int j = 0; j < NUM_FILES; j++) {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix);
            for (int i = 0; i < PATH_DEPTH; i++) {
                builder.append('/');
                builder.append(RandomStringUtils.randomAlphanumeric(FOLDERNAME_LENGTH));
            }
            builder.append("/file-");
            builder.append(j);
            files[j] = new Path(builder.toString());
            try {
                FSUtils.createFile(hdfs, files[j], 1024 * 1024 * 64, 1).close();
            } catch (IOException e) {
                System.out.println("Warning: IOException attempting to create " + files[j]);
                e.printStackTrace();
                return;
            } finally {
                BenchmarkUtils.StopTracing();
            }
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

        // Randomly lookup files
        while (!Thread.currentThread().isInterrupted()) {
            Path file = files[r.nextInt(NUM_FILES)];
            try {
                BenchmarkUtils.SetTenant(1);
                BenchmarkUtils.PopulateCurrentBaggage(nbags, nper);

                long begin = System.nanoTime();
                FSDataInputStream stream = FSUtils.openFile(hdfs, file);
                long duration = System.nanoTime() - begin;
                aggregator.startedAndFinished(Operation.OPEN, 1, 1, duration);
                stream.close();
            } catch (IOException e) {
                System.out.println("Warning: IOException attempting to create " + file);
            } finally {
                BenchmarkUtils.StopTracing();
            }

        }
    }

}
