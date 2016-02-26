package edu.brown.cs.systems.retro.throttling.cli;

import edu.brown.cs.systems.retro.throttling.ClusterThrottlingPoints;

public enum UtilCommand {

    CLEARALL;

    public void execute() throws InterruptedException {
        switch (this) {
        case CLEARALL:
            Thread.sleep(1000);
            ClusterThrottlingPoints.clearAll();
            ;
        }
    }

    private static void usage() {
        StringBuilder usage = new StringBuilder();
        usage.append("Expected argument one of:");
        for (UtilCommand command : UtilCommand.values()) {
            usage.append(' ');
            usage.append(command.name());
        }
        System.out.println(usage.toString());
    }

    public static void main(String[] args) {
        UtilCommand command;
        try {
            command = UtilCommand.valueOf(args[0]);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No arguments specified");
            usage();
            return;
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown value " + args[0]);
            usage();
            return;
        }

        System.out.println("Waiting for cluster state");

        // Initialize the throttling and make sure we get some data
        ClusterThrottlingPoints.getTenants();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            return;
        }

        System.out.println("Executing " + command.name());

        try {
            command.execute();
        } catch (InterruptedException e) {
        }

        System.out.println("Done");
    }

}
