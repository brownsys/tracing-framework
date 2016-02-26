package edu.brown.cs.systems.pubsub.tools;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubServer;

/**
 * A main class that runs a standalone server on the default hostname and port
 */
public class StandaloneServer {

    /**
     * Run a standalone server until it is interrupted
     */
    public static void main(String[] args) throws IOException {
        // Configure console logging
        BasicConfigurator.configure();

        PubSubServer server = PubSub.startServer();
        try {
            server.join();
        } catch (InterruptedException e) {
            server.shutdown();
        }

    }

}
