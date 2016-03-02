package edu.brown.cs.systems.pubsub;

import java.io.IOException;

import org.junit.Test;

import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;
import junit.framework.TestCase;

public class TestPubSubQueueLimit extends TestCase {
    
    
    @Test
    public void testMaxQueue() throws IOException {
        PubSubClient c = new PubSubClient("127.0.0.1", 5563, 10);
        
        String topic = "Hello";
        StringMessage msg = StringMessage.newBuilder().setMessage("Hello").build();
        
        for (int i = 0; i < 100; i++) {
            c.publish(topic, msg);
            assertTrue(c.pending.size() <= 10);
        }
    }

}
