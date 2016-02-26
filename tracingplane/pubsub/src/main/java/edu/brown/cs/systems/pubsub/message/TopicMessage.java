package edu.brown.cs.systems.pubsub.message;

public interface TopicMessage {

    public byte[] topic();

    public byte[] message();

}
