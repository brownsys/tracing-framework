package edu.brown.cs.systems.pubsub;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;
import junit.framework.TestCase;

public class TestPubSub extends TestCase {

    public static final String host = "127.0.0.1";
    public static final int port = 5563;

    private static final class TestPubSubCallback extends Subscriber<PubSubProtos.StringMessage> {

        private static final int wait_time = 1000;

        public volatile String msg = null;
        private Semaphore semaphore = new Semaphore(0);

        public void awaitMessage(String msg) throws InterruptedException {
            assertTrue(semaphore.tryAcquire(wait_time, TimeUnit.MILLISECONDS));
            assertTrue(msg.equals(this.msg));
            reset();
        }

        public void awaitNoMessage(String msg) throws InterruptedException {
            assertFalse(semaphore.tryAcquire(wait_time, TimeUnit.MILLISECONDS));
            assertFalse(msg.equals(this.msg));
            assertNull(this.msg);
            reset();
        }

        public void reset() {
            semaphore.drainPermits();
            msg = null;
        }

        @Override
        protected void OnMessage(StringMessage message) {
            msg = message.getMessage();
            semaphore.release();
        }

    }

    @Test
    public void testBPubSub() throws InterruptedException, IOException {
        
        String topic = "test";

        // Start pubsub server
        PubSubServer server = new PubSubServer(host, port);
        server.start();

        // Create publisher and subscriber
        PubSubClient publisher = new PubSubClient(host, port, 0);
        publisher.start();
        PubSubClient subscriber = new PubSubClient(host, port, 0);
        subscriber.start();

        // Subscribe to test topic
        TestPubSubCallback cb = new TestPubSubCallback();
        subscriber.subscribe(topic, cb);
        Thread.sleep(1000);

        // Publish a message
        System.out.println("Publisher publishing " + "hello" + " on topic " + topic);
        publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        cb.awaitMessage("hello");

        // Publish a message
        System.out.println("Publisher publishing " + "hello" + " on topic " + "badtest");
        publisher.publish("badtest", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        cb.awaitNoMessage("hello");

        // Publish a message
        System.out.println("Publisher publishing " + "hello2" + " on topic " + topic);
        publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello2").build());
        cb.awaitMessage("hello2");

        // Unsubscribe
        subscriber.unsubscribe(topic, cb);
        Thread.sleep(1000);

        // Publish a message
        System.out.println("Publisher publishing " + "hello2" + " on topic " + topic);
        publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello2").build());
        cb.awaitNoMessage("hello2");
    }
    
    @Test
    public void testWaitUntilEmpty() throws IOException, InterruptedException {
        PubSubClient client = new PubSubClient(host, port, 0);

        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        
        assertEquals(3, client.pending.size());

        assertEquals(false, client.waitUntilEmpty(100));
        assertEquals(false, client.waitUntilEmpty(100));
        
        client.start();

        assertEquals(true, client.waitUntilEmpty(100));
        assertEquals(0, client.pending.size());
        
        client.suspend();

        assertEquals(0, client.pending.size());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        assertEquals(2, client.pending.size());
        
        client.resume();
        assertEquals(true, client.waitUntilEmpty(100));
        assertEquals(0, client.pending.size());
        
        client.suspend();

        assertEquals(0, client.pending.size());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        client.publish("hi", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
        assertEquals(2, client.pending.size());

        assertEquals(false, client.waitUntilEmpty(100));
        
        client.resume();
        assertEquals(true, client.waitUntilEmpty(100));
        assertEquals(0, client.pending.size());
        
        
        
    }

}
