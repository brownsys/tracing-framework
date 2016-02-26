package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import edu.brown.cs.systems.pivottracing.agent.PivotTracing;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;
import edu.brown.cs.systems.tracing.Utils;

/**
 * Everything here is static, because the HDFS client under the covers does a
 * lot of object sharing and caching. Want to make it clear that multiple
 * instances would not be distinct from each other. Only multiple processes will
 * achieve true independence.
 * 
 * @author a-jomace
 */
public enum Benchmarks {
    rename(new Rename()), openread(new OpenRead()), createwrite(new CreateWrite()), generate(new Generate("default")), read8k(new Read("default", 8 * 1024)), // 8kB
    read4m(new Read("default", 4 * 1024 * 1024)), // 4MB
    read64m(new Read("default", 64 * 1024 * 1024)), // 64MB
    delete(new Delete()),

    // read8k but also propagate metadata determined by Benchmark.run()
    propagate(new Propagate("default", 8 * 1024));

    private final Benchmark benchmark;

    private Benchmarks(Benchmark benchmark) {
        this.benchmark = benchmark;
    }

    public void run(String hdfsurl, int numthreads, final String... opts) {
        final FileSystem hdfs;
        try {
            Configuration conf = new HdfsConfiguration();
            conf.set("fs.defaultFS", hdfsurl);
            hdfs = FileSystem.get(conf);
        } catch (Exception e) {
            System.out.println("Unable to get configured file system.");
            System.out.println("hdfs-generator.hdfs-url=" + hdfsurl);
            e.printStackTrace();
            return;
        }

        final HDFSAggregator aggregator = LocalResources.getHDFSAggregator();

        Runnable r = new Runnable() {
            final int pid = Utils.getProcessID();
            final AtomicInteger worker_id_seed = new AtomicInteger();

            @Override
            public void run() {
                int wid = worker_id_seed.incrementAndGet();
                benchmark.run(hdfs, pid, wid, aggregator, opts);
            }
        };

        ExecutorService threadpool = Executors.newCachedThreadPool();
        for (int i = 0; i < numthreads; i++)
            threadpool.submit(r);

        try {
            threadpool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            threadpool.shutdownNow();
        }
    }

    public static void runBenchmark(String hdfsurl, String benchmark, int numthreads, String... opts) {
        Benchmarks torun = null;
        try {
            torun = Benchmarks.valueOf(benchmark.toLowerCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown benchmark " + benchmark);
            return;
        }
        torun.run(hdfsurl, numthreads, opts);
    }

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("src/main/resources/log4j.properties"));
        PropertyConfigurator.configure(props);
        PivotTracing.initialize();
        
        String hdfsurl, benchmark;
        int numthreads;
        try {
            hdfsurl = args[0];
            benchmark = args[1];
            numthreads = Integer.parseInt(args[2]);
        } catch (Exception e) {
            System.out.println("Unable to run benchmark");
            System.out.println("Expected arguments:");
            System.out.println("  <hdfsurl> <benchmark> <numthreads>");
            System.out.println("eg,");
            System.out.println("  hdfs://127.0.0.1:9000 rename 1");
            return;
        }
        try {
            runBenchmark(hdfsurl, benchmark, numthreads, Arrays.copyOfRange(args, 3, args.length));
        } catch (Exception e) {
            System.out.println("Exception running benchmark: " + e);
            e.printStackTrace();
        }
    }
}
