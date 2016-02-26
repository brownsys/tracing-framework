package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;
import edu.brown.cs.systems.tracing.Utils;

/**
 * benchmark whose requests propagate metadata through HDFS read8k requests.
 * 
 * The baggage to propagate is passed in the "opts" argument to run(). This
 * object array is assumed to contain
 * 
 * @author ryan
 * 
 */
public class Propagate implements Benchmark {

    private static final Random r = new Random(Utils.getProcessID() * System.currentTimeMillis());

    private static final int BYTES_PER_BAG = 8; /* probably a realistic average */

    /**
     * @param nbags
     * @param nper
     * @return a mapping defining baggage containing nbags keys, with nper items
     *         per bag. Each baggage item and each key is BYTES_PER_BAG bytes
     *         long.
     */
    public static Map<String, List<ByteString>> generateRandomBaggage(int nbags, int nper) {
        Map<String, List<ByteString>> baggage = Maps.newHashMap();
        for (int i = 0; i < nbags; i++) {
            /* generate the list of items for the bag */
            List<ByteString> items = Lists.newArrayList();
            for (int j = 0; j < nper; j++) {
                byte[] itemBytes = new byte[BYTES_PER_BAG];
                r.nextBytes(itemBytes);
                items.add(ByteString.copyFrom(itemBytes));
            }

            /* find a key for it */
            String key;
            byte[] keyBytes = new byte[BYTES_PER_BAG];
            do {
                r.nextBytes(keyBytes);
                key = new String(keyBytes);
            } while (baggage.containsKey(key));

            /* pack it */
            baggage.put(key, items);
        }
        return Collections.unmodifiableMap(baggage);
    }

    private final String dsname;
    private long readsize;

    public Propagate(String dsname, long readsize) {
        this.dsname = dsname;
        this.readsize = readsize;
    }

    @Override
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        // parse number of bags / number of items per bag from opts
        if (opts.length < 2) {
            System.err.println("insufficient arguments to run benchmark");
            throw new IllegalArgumentException(String.format("expected 2 opts, received %d", opts.length));
        }

        int nbags, nper;
        try {
            nbags = Integer.parseInt(opts[0]);
            nper = Integer.parseInt(opts[1]);
        } catch (Exception e) {
            System.out.println("error parsing nbags/nper");
            throw new IllegalArgumentException(e);
        }

        BenchmarkUtils.StopTracing();
        BenchmarkUtils.SetTenant(1);
        BenchmarkUtils.PopulateCurrentBaggage(nbags, nper);
        byte[] baggage = Baggage.stop().toByteArray();

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

                    Baggage.start(baggage);

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
