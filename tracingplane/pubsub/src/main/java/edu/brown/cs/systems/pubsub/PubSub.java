package edu.brown.cs.systems.pubsub;

import java.io.IOException;

import com.google.protobuf.Message;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;

public class PubSub {

    private static PubSubClient defaultClient = null;

    private static synchronized void createDefaultClient() {
        if (defaultClient == null) {
            Config conf = ConfigFactory.load();
            String hostname = conf.getString("pubsub.server.hostname");
            int port = conf.getInt("pubsub.server.port");
            int maxPendingMessages = conf.getInt("pubsub.client.maxPendingMessages");
            try {
                defaultClient = startClient(hostname, port, maxPendingMessages, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Create a pub sub client and connect it to the specified server
     * 
     * @param serverHostName Hostname of the server to connect to
     * @param serverPort port of the server to connect to
     * @return a pub sub client that has kicked off a separate thread to connect to the server
     * @throws IOException */
    public static PubSubClient startClient(String serverHostName, int serverPort, int maxPendingMessages, boolean isDaemon)
            throws IOException {
        PubSubClient client = new PubSubClient(serverHostName, serverPort, maxPendingMessages);
        client.setDaemon(isDaemon);
        client.start();
        return client;
    }

    /** Create a pub sub server, bind it to the default hostname (pubsub.server.bindto) and port (pubsub.server.port),
     * and start the server thread
     * 
     * @return the started server
     * @throws IOException if the server could not bind to the provided host/port, or some other exception occurred */
    public static PubSubServer startServer() throws IOException {
        String bindToHostName = ConfigFactory.load().getString("pubsub.server.bindto");
        int bindToPort = ConfigFactory.load().getInt("pubsub.server.port");
        return startServer(bindToHostName, bindToPort);
    }

    /** Create a pub sub server, bind it to the specified hostname / port, and start the server thread
     * 
     * @param bindToHostName hostname for the server to bind to
     * @param bindToPort port for the server to bind to
     * @return the started server
     * @throws IOException if the server could not bind to the provided host/port, or some other exception occurred */
    public static PubSubServer startServer(String bindToHostName, int bindToPort) throws IOException {
        PubSubServer server = new PubSubServer(bindToHostName, bindToPort);
        server.start();
        return server;
    }

    /** Returns the default pubsub client */
    public static PubSubClient client() {
        if (defaultClient == null) {
            createDefaultClient();
        }
        return defaultClient;
    }

    /** Subscribes to the specified topic, registering the provided callback, using the default subscriber.
     * 
     * @param topic the topic to subscribe to
     * @param subscriber the subscriber to register */
    public static void subscribe(String topic, Subscriber<?> subscriber) {
        client().subscribe(topic, subscriber);
    }

    /** Unsubscribes the provided callback from the specified topic
     * 
     * @param topic the topic to unsubscribe from
     * @param subscriber the subscriber to unsubscribe, if subscribed */
    public static void unsubscribe(String topic, Subscriber<?> subscriber) {
        client().unsubscribe(topic, subscriber);
    }

    /** Publishes to the specified topic and message using the default publisher
     * 
     * @param topic the topic to publish on
     * @param message the message to publish */
    public static void publish(String topic, Message message) {
        client().publish(topic, message);
    }

    /** Publishes to the specified topic and message using the default publisher
     * 
     * @param topic the topic to publish on
     * @param message the message to publish */
    public static void publish(String topic, String message) {
        client().publish(topic, StringMessage.newBuilder().setMessage(message).build());
    }

    /** Wait for all pending messages to be sent to the server  */
    public static void awaitFlush(long timeout) throws InterruptedException {
        client().waitUntilEmpty(timeout);
    }

}
