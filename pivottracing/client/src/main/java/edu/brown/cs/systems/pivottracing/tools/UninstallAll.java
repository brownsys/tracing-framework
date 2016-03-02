package edu.brown.cs.systems.pivottracing.tools;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pubsub.PubSub;

public class UninstallAll {
    
    public static void main(String[] args) throws InterruptedException {
        PivotTracingClient.newInstance().uninstallAll();
        while (!PubSub.awaitFlush(1000)) {
            System.out.println("Uninstall message not yet sent, waiting 1s...");
        }
        System.out.println("Uninstall sent, exiting");
        PubSub.close();
    }

}
