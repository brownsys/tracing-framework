package edu.brown.cs.systems.mrgenerator.jobs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;

public interface MRGeneratorJob {

    /**
     * Initialize the input for the job. Called once at the very start, before
     * running the job multiple times
     * 
     * @throws IOException
     */
    public void initialize(FileSystem fs) throws IOException;

    /**
     * Configure the classes of the job. Called once at the very start, before
     * running the job multiple times
     */
    public void configure(JobConf jobconf);

    /**
     * Does any cleanup needed by the job, eg. to delete the output data. Called
     * before every MR job run
     */
    public void teardown(FileSystem fs) throws IOException;

}
