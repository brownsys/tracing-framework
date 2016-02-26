package edu.brown.cs.systems.pubsub.tools;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;

import edu.brown.cs.systems.pubsub.PubSub;

public class PublishTool {

    public static void main(String[] args) throws InterruptedException {
        // Configure console logging
        BasicConfigurator.configure();

        String topic = "publishing_to_topic";
        if (args.length > 0) {
            topic = args[0];
        }
        System.out.println("Publishing to topic " + topic);

        while (!Thread.currentThread().isInterrupted()) {
            String s = RandomStringUtils.randomAlphanumeric(10);
            PubSub.publish(topic, s);
            Thread.sleep(1000);
        }
    }

}
