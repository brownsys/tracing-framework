package edu.brown.cs.systems.mrgenerator.jobs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.NullOutputFormat;

import com.typesafe.config.ConfigFactory;

/** Just give the job a folder and it'll read it */
public class ReadExistingDataJob implements MRGeneratorJob {

    private final String input_path;

    public ReadExistingDataJob() {
        this(ConfigFactory.load().getString("mapreduce-generator.existing-data"));
    }

    public ReadExistingDataJob(String input_path) {
        this.input_path = input_path;
    }

    public void initialize(FileSystem fs) throws IOException {
        if (!fs.exists(new Path(this.input_path))) {
            throw new IOException("Input path does not exist: " + input_path);
        }
    }

    public void configure(JobConf job) {
        // Set the mapper and reducers
        job.setMapperClass(ReadDataJob.TestMapper.class);

        // Make sure this jar is included
        job.setJarByClass(ReadDataJob.TestMapper.class);

        // Specify the input and output data formats
        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(NullOutputFormat.class);

        // Turn off speculative execution
        job.setMapSpeculativeExecution(false);
        job.setReduceSpeculativeExecution(false);

        // Add the job input path
        FileInputFormat.addInputPath(job, new Path(this.input_path));
    }

    @Override
    public void teardown(FileSystem fs) throws IOException {
        // do nothing
    }

    public String toString() {
        return String.format("ReadExistingDataJob inputPath=%s", input_path);
    }

}
