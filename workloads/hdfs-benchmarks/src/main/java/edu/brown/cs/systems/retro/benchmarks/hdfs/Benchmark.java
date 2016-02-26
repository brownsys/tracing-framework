package edu.brown.cs.systems.retro.benchmarks.hdfs;

import org.apache.hadoop.fs.FileSystem;

import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;

public interface Benchmark {

    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts);

}
