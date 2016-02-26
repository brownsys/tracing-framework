package edu.brown.cs.systems.pubsub.message;

import com.google.protobuf.Message;

public class ProtobufMessage implements TopicMessage {

    private final String topic;
    private final Message message;
    private byte[] topicBytes = null;
    private byte[] messageBytes = null;

    public ProtobufMessage(String topic, Message message) {
        this.topic = topic;
        this.message = message;
    }

    @Override
    public byte[] topic() {
        if (topicBytes == null) {
            topicBytes = topic.getBytes();
        }
        return topicBytes;
    }

    @Override
    public byte[] message() {
        if (messageBytes == null) {
            messageBytes = message.toByteArray();
        }
        return messageBytes;
    }

}
