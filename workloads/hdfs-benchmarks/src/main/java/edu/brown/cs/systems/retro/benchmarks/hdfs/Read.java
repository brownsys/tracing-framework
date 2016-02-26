package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;
import edu.brown.cs.systems.tracing.Utils;
import edu.brown.cs.systems.xtrace.XTrace;

public class Read implements Benchmark {

    private static final Random r = new Random(Utils.getProcessID() * System.currentTimeMillis());

    private final String dsname;
    private long readsize;

    public Read(String dsname, long readsize) {
        this.dsname = dsname;
        this.readsize = readsize;
    }

    @Override
    /**
     * opts optionally contains the number of bags and number of items per bag to propagate
     */
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        // Load the dataset
        DataSet ds;
        try {
            ds = DataSet.load(hdfs, dsname);
        } catch (IOException e) {
            System.out.println("IOException loading dataset");
            e.printStackTrace();
            return;
        }

        if (ds.filesize < readsize) {
            System.out.printf("Error: filesize smaller than readsize (%d < %d)\nDataset: %s", ds.filesize, readsize, ds);
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

        byte[] buf = new byte[64 * 1024]; // 64k buffer

        // Randomly read from files
        while (!Thread.currentThread().isInterrupted()) {
            Path file = ds.randomFile();

            try {
                FSDataInputStream in = hdfs.open(file);

                try {
                    int max_start_offset = (int) (ds.filesize - this.readsize);
                    int start_offset = r.nextInt(max_start_offset + 1);
                    long toread = readsize;

                    BenchmarkUtils.StopTracing();
                    BenchmarkUtils.SetTenant(1);
                    XTrace.startTask(true);
                    BenchmarkUtils.PopulateCurrentBaggage(nbags, nper);
                    XTrace.getDefaultLogger().tag("Starting read op", "Read");

                    // START
                    long begin = System.nanoTime();

                    toread -= in.read(start_offset, buf, 0, (int) Math.min(toread, buf.length));
                    while (toread > 0) {
                        int numread = in.read(buf, 0, (int) Math.min(toread, buf.length));
                        if (numread == -1)
                            throw new IOException("Reached EOF with " + toread + " bytes remaining to be read");
                        toread -= numread;
                    }

                    // END
                    long duration = System.nanoTime() - begin;

                    aggregator.startedAndFinished(Operation.READ, 1, readsize, duration);

                } finally {
                    in.close();
                }
            } catch (IOException e) {
                System.out.println("Warning: IOException attempting to read " + file);
            } finally {
                BenchmarkUtils.StopTracing();
            }
        }
    }

}
