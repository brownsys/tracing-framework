package edu.brown.cs.systems.mrgenerator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.systems.mrgenerator.jobs.MRGeneratorJob;
import edu.brown.cs.systems.mrgenerator.jobs.ReadDataJob;
import edu.brown.cs.systems.mrgenerator.jobs.ReadExistingDataJob;

public class CommandLineInterface {

    private enum MRJob {
        read, bigread;

        public RunningJob create(int tenantClass, String jobName) {
            MRGeneratorJob toRun = null;
            switch (this) {
            case read:
                toRun = new ReadDataJob();
                break;
            case bigread:
                toRun = new ReadExistingDataJob();
                break;
            }
            RunningJob job = new RunningJob(tenantClass, jobName, new JobRunner(toRun, tenantClass));
            return job;
        }
    }

    public static class RunningJob {
        public final int tenantClass;
        public final String jobName;
        private final JobRunner runner;

        public RunningJob(int tenantClass, String jobName, JobRunner runner) {
            this.tenantClass = tenantClass;
            this.jobName = jobName;
            this.runner = runner;
        }

        public String toString() {
            return jobName + ": " + runner;
        }
    }

    private static Map<String, RunningJob> running = new HashMap<String, RunningJob>();

    public static synchronized void startJob(int tenantClass, String jobtype) {
        MRJob toRun = MRJob.valueOf(jobtype);

        if (toRun == null) {
            System.out.println("No job of type " + jobtype);
            return;
        }

        int i = 1;
        String name;
        do {
            name = String.format("%s-%d-%d", jobtype, tenantClass, i++);
        } while (running.containsKey(name));

        RunningJob rj = toRun.create(tenantClass, name);
        rj.runner.start();

        running.put(name, rj);
    }

    public static synchronized void stopJob(String jobname) {
        RunningJob rj = running.remove(jobname);
        if (rj != null)
            rj.runner.stopJobRunner();
    }

    public static synchronized void PrintStatus() {
        System.out.println("Running Jobs:");
        for (RunningJob rj : running.values()) {
            System.out.printf("  %s\n", rj);
        }
    }

    public static synchronized void PrintJobStatus(String jobname) {
        RunningJob rj = running.get(jobname);
        if (rj != null)
            System.out.println(rj.runner.status());
        else
            System.out.println("No known job with name " + jobname);
    }

    private static void PrintHelp() {
        StringBuffer sb = new StringBuffer();
        sb.append("usage:\n");
        sb.append("  - start <tenantid> <jobtype> (start one job of the specified type for the specified tenant)\n");
        sb.append("  - stop <jobname> (stop the named job. check job names with status command)\n");
        sb.append("  - status (list all running jobs)\n");
        sb.append("  - status <jobname> (detailed status of specified job)\n");
        sb.append("  - help (prints this status message)\n");
        sb.append("  - exit\n");
        System.out.println(sb.toString());
    }

    private static void Run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintHelp();
        while (true) {
            System.out.print("> ");
            String line = null;

            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                boolean exit = ParseAndExecuteCommand(line);
                if (exit)
                    break;
            } catch (Exception e) {
                System.err.println("exception when executing '" + line + "': " + e);
                e.printStackTrace();
            }
        }
    }

    public synchronized static boolean ParseAndExecuteCommand(String line) throws FileNotFoundException {
        String[] tokens = line.split(" ");

        if (tokens.length == 0)
            return false;

        String cmd = tokens[0].trim();

        if (cmd.equalsIgnoreCase("help")) {
            PrintHelp();
        } else if (cmd.equalsIgnoreCase("status")) {
            if (tokens.length > 1) {
                PrintJobStatus(tokens[1]);
            } else {
                PrintStatus();
            }
        } else if (cmd.equalsIgnoreCase("start")) {
            int tenantClass = Integer.parseInt(tokens[1]);
            String jobtype = tokens[2];
            startJob(tenantClass, jobtype);
        } else if (cmd.equalsIgnoreCase("stop")) {
            String jobtype = tokens[1];
            stopJob(jobtype);
        } else if (cmd.equalsIgnoreCase("exit")) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        Run();
    }

}
