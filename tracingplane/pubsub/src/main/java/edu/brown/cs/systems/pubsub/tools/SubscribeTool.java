package edu.brown.cs.systems.pubsub.tools;

import org.apache.log4j.BasicConfigurator;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;

public class SubscribeTool {

    private static class SubscribeToolSubscriber extends Subscriber<StringMessage> {

        @Override
        protected void OnMessage(StringMessage message) {
            System.out.println("Received message:" + message);
        }

    }

    public static void main(String[] args) throws InterruptedException {
        // Configure console logging
        BasicConfigurator.configure();

        String topic = "publishing_to_topic";
        if (args.length > 0) {
            topic = args[0];
        }
        System.out.println("Subscribing to topic " + topic);

        // Subscribe to the topic
        PubSub.subscribe(topic, new SubscribeToolSubscriber());

        // Wait for the client to be interrupted
        PubSub.client().join();
    }

}
