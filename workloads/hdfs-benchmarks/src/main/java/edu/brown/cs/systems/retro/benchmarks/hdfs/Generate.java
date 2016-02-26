package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;

import edu.brown.cs.systems.retro.aggregation.aggregators.HDFSAggregator;

public class Generate implements Benchmark {

    private final String dsname;

    public Generate(String dsname) {
        this.dsname = dsname;
    }

    @Override
    public void run(FileSystem hdfs, int pid, int wid, HDFSAggregator aggregator, String... opts) {
        // Load the dataset
        try {
            DataSet.create(hdfs, dsname);
            System.out.println("done generating dataset");
        } catch (IOException e) {
            System.out.println("IOException loading dataset");
            e.printStackTrace();
            return;
        }
    }

}
