package edu.brown.cs.systems.pubsub.io;

import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.brown.cs.systems.pubsub.message.TopicMessage;

public abstract class TopicWriter extends MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(TopicWriter.class);

    private boolean writingTopic = true;
    private TopicMessage current = null;

    public TopicWriter(SocketChannel channel) {
        super(channel);
    }

    public abstract TopicMessage CurrentMessage();

    public abstract void MessageSendComplete();

    @Override
    public byte[] Current() {
        // Get the current message
        if (current == null) {
            current = CurrentMessage();
        }

        // Nothing to write, so return
        if (current == null) {
            return null;
        }

        // Return either the topic or the message bytes
        if (writingTopic) {
            return current.topic();
        } else {
            return current.message();
        }
    }

    @Override
    public void MoveNext() {
        // Start writing message
        if (writingTopic) {
            writingTopic = false;
        } else {
            try {
                MessageSendComplete();
            } finally {
                writingTopic = true;
                current = null;
            }
        }
    }
}
