package edu.brown.cs.systems.mrgenerator.jobs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.NullOutputFormat;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.mrgenerator.DataGenerator;

/**
 * Reads input data from HDFS but does nothing subsequently Does not generate
 * any output for reducers
 */
public class ReadDataJob implements MRGeneratorJob {

    private final long filesize;
    private final long blocksize;
    private final int nReplicas;
    private final String input_filename;

    public ReadDataJob() {
        this(ConfigFactory.load().getLong("mapreduce-generator.block-size") * ConfigFactory.load().getInt("mapreduce-generator.num-blocks"), ConfigFactory
                .load().getLong("mapreduce-generator.block-size"), ConfigFactory.load().getInt("mapreduce-generator.replicas"));
    }

    public ReadDataJob(long filesize, long blocksize, int nReplicas) {
        this.filesize = filesize;
        this.blocksize = blocksize;
        this.nReplicas = nReplicas;
        this.input_filename = String.format("/mrgenerator-readdata-b%d-f%d-r%d", blocksize, filesize, nReplicas);
    }

    public void initialize(FileSystem fs) throws IOException {
        new DataGenerator(this.input_filename).blockSize(blocksize).fileSize(filesize).replicas(nReplicas).generate(fs);
    }

    public void configure(JobConf job) {
        // Set the mapper and reducers
        job.setMapperClass(TestMapper.class);
        // job.setReducerClass(TestReducer.class);

        // Set the output types of the mapper and reducer
        // job.setMapOutputKeyClass(IntWritable.class);
        // job.setMapOutputValueClass(NullWritable.class);
        // job.setOutputKeyClass(NullWritable.class);
        // job.setOutputValueClass(NullWritable.class);

        // Make sure this jar is included
        job.setJarByClass(TestMapper.class);

        // Specify the input and output data formats
        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(NullOutputFormat.class);

        // Turn off speculative execution
        job.setMapSpeculativeExecution(false);
        job.setReduceSpeculativeExecution(false);

        // Add the job input path
        FileInputFormat.addInputPath(job, new Path(this.input_filename));
    }

    @Override
    public void teardown(FileSystem fs) throws IOException {
        // do nothing
    }

    public static class TestMapper extends MapReduceBase implements Mapper<LongWritable, Text, IntWritable, NullWritable> {
        @Override
        public void configure(JobConf arg0) {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void map(LongWritable key, Text value, OutputCollector<IntWritable, NullWritable> output, Reporter reporter) throws IOException {
        }
    }

    // public static class TestReducer extends MapReduceBase implements
    // Reducer<IntWritable, NullWritable, NullWritable, NullWritable> {
    //
    // @Override
    // public void configure(JobConf arg0) {
    // }
    //
    // @Override
    // public void close() throws IOException {
    // }
    //
    // @Override
    // public void reduce(IntWritable key, Iterator<NullWritable> values,
    // OutputCollector<NullWritable, NullWritable> output, Reporter reporter)
    // throws IOException {
    // }
    //
    // }

    public String toString() {
        return String.format("ReadDataJob blockSize=%dMB fileSize=%dMB", blocksize / 1024 / 1024, filesize / 1024 / 1024);
    }

}
