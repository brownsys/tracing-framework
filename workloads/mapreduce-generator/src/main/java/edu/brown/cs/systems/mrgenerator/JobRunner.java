package edu.brown.cs.systems.mrgenerator;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.mrgenerator.jobs.MRGeneratorJob;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public class JobRunner extends Thread {

    private XTraceLogger xtrace = XTrace.getLogger(JobRunner.class);

    private MRGeneratorJob job;
    private AtomicInteger numToSample = new AtomicInteger(4);
    private final int tenantClass;
    private volatile String status;
    private volatile int count = 1;
    private volatile RunningJob rj = null;

    public JobRunner(MRGeneratorJob job, int tenantClass) {
        this.job = job;
        this.tenantClass = tenantClass;
        this.status = "Not started";
    }

    public void stopJobRunner() {
        this.interrupt();
    }

    public void run() {
        try {
            runJobContinuously();
        } catch (IOException e1) {
            System.out.printf("IOException attempting to run MapReduce %s, ending\n", job.getClass().getSimpleName().toString());
            e1.printStackTrace();
        } catch (InterruptedException e2) {
            System.out.printf("MapReduce %s interrupted, ending\n", job.getClass().getSimpleName().toString());
        }
    }

    private void runJobContinuously() throws IOException, InterruptedException {
        System.out.printf("Starting MapReduce %s runner for tenant %d\n", job.getClass().getSimpleName(), tenantClass);
        this.status = "Configuring";

        // Set the HDFS variables in the config
        Configuration conf = new Configuration();
        conf.set("yarn.resourcemanager.hostname", ConfigFactory.load().getString("mapreduce-generator.yarn-resourcemanager-hostname"));
        conf.set("mapreduce.framework.name", "yarn");
        conf.set("fs.defaultFS", ConfigFactory.load().getString("mapreduce-generator.hdfs-namenode-url"));

        // Create a job config and get the job to populate it
        JobConf jobconf = new JobConf(conf);
        job.configure(jobconf);

        // Populate the input data if needed
        this.status = "Initializing input data";
        FileSystem fs = FileSystem.get(conf);
        job.initialize(fs);

        // Now start running the job in a loop
        while (!Thread.currentThread().isInterrupted()) {
            this.status = "Cleaning up Job #" + count;
            // Clear any previous xtrace context
            Baggage.stop();

            // Clean up previous output if necessary
            job.teardown(fs);

            // Set the xtrace metadata for the new job
            boolean sampled = numToSample.getAndDecrement() > 0;
            if (sampled) {
                XTrace.startTask(true);
                xtrace.log("Starting job");
            } else {
                numToSample.getAndIncrement();
            }
            Retro.setTenant(tenantClass);

            // Run the job
            try {
                this.status = "Running Job #" + (count++);
                JobClient jc = new JobClient(jobconf);
                rj = jc.submitJob(jobconf);
                rj.waitForCompletion();
                System.out.println("Job Complete");
                System.out.println(rj);
                rj = null;
            } finally {
                // Log the end of the job and clear the metadata
                if (sampled)
                    xtrace.log("Job complete");
                Baggage.stop();
            }
        }
        throw new InterruptedException("JobRunner interrupted");
    }

    public String toString() {
        return String.format("tenant=%d %s %s", tenantClass, job, status);
    }

    public String status() {
        RunningJob rj = this.rj;
        if (rj != null)
            return rj.toString();
        else
            return status;
    }

}
