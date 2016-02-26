package edu.brown.cs.systems.pubsub.message;

public class ByteMessage implements TopicMessage {

    private final byte[] topic;
    private final byte[] message;

    public ByteMessage(byte[] topic, byte[] message) {
        this.topic = topic;
        this.message = message;
    }

    @Override
    public byte[] topic() {
        return topic;
    }

    @Override
    public byte[] message() {
        return message;
    }

}
