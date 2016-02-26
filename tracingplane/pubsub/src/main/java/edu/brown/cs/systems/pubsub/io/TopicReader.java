package edu.brown.cs.systems.pubsub.io;

import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TopicReader extends MessageReader {

    private static final Logger log = LoggerFactory.getLogger(TopicReader.class);

    private byte[] currentMessageTopic;

    public TopicReader(SocketChannel channel) {
        super(channel);
        this.currentMessageTopic = null;
    }

    public abstract void OnMessage(byte[] topic, byte[] message);

    @Override
    public void OnMessage(byte[] message) {
        if (currentMessageTopic == null) {
            currentMessageTopic = message;
            log.debug("Read message topic {}", message);
        } else {
            try {
                OnMessage(currentMessageTopic, message);
            } finally {
                currentMessageTopic = null;
            }
        }
    }

}
