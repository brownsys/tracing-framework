//package edu.brown.cs.systems.pubsub;
//
//import static org.junit.Assert.*;
//
//import java.util.concurrent.Semaphore;
//import java.util.concurrent.TimeUnit;
//
//import org.junit.Test;
//
//import edu.brown.cs.systems.pubsub.PubSubProtos.StringMessage;
//import edu.brown.cs.systems.pubsub.Subscriber.Callback;
//
//public class TestPubSub {
//
//  /**
//   * Tests to see whether the ZMQ bindings work
//   */
//  @Test
//  public void testAZMQ() {
//    try {
//      if (PubSub.context != null) {
//        System.out.println("ZMQ Binding successful");
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("Unable to bind ZMQ");
//    }
//  }
//  
//  
//  private static final class TestPubSubCallback extends Callback<PubSubProtos.StringMessage> {
//    
//    private static final int wait_time = 1000;
//    
//    public volatile String msg = null;
//    private Semaphore semaphore = new Semaphore(0);
//    
//    public void awaitMessage(String msg) throws InterruptedException {
//      assertTrue(semaphore.tryAcquire(wait_time, TimeUnit.MILLISECONDS));
//      assertTrue(msg.equals(this.msg));
//      reset();
//    }
//    
//    public void awaitNoMessage(String msg) throws InterruptedException {
//      assertFalse(semaphore.tryAcquire(wait_time, TimeUnit.MILLISECONDS));
//      assertFalse(msg.equals(this.msg));
//      assertNull(this.msg);
//      reset();      
//    }
//    
//    public void reset() {
//      semaphore.drainPermits();
//      msg = null;
//    }
//
//    @Override
//    protected void OnMessage(StringMessage message) {
//      msg = message.getMessage();
//      semaphore.release();
//    }
//    
//  }
//  
//  @Test
//  public void testBPubSub() throws InterruptedException {
//    String topic = "test";
//    
//    // Start pubsub server
//    Server server = new Server("127.0.0.1", Settings.CLIENT_SUBSCRIBE_PORT, Settings.CLIENT_PUBLISH_PORT);
//    server.start();
//    
//    // Create publisher and subscriber
//    Publisher publisher = new Publisher("127.0.0.1", Settings.CLIENT_PUBLISH_PORT);
//    Subscriber subscriber = new Subscriber("127.0.0.1", Settings.CLIENT_SUBSCRIBE_PORT);
//    
//    // Subscribe to test topic
//    TestPubSubCallback cb = new TestPubSubCallback();
//    subscriber.subscribe(topic, cb);
//    Thread.sleep(1000);
//    
//    // Publish a message
//    System.out.println("Publisher publishing " + "hello" + " on topic " + topic);
//    publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
//    cb.awaitMessage("hello");
//    
//    // Publish a message
//    System.out.println("Publisher publishing " + "hello" + " on topic " + "badtest");
//    publisher.publish("badtest", PubSubProtos.StringMessage.newBuilder().setMessage("hello").build());
//    cb.awaitNoMessage("hello");
//    
//    // Publish a message
//    System.out.println("Publisher publishing " + "hello2" + " on topic " + topic);
//    publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello2").build());
//    cb.awaitMessage("hello2");
//    
//    // Unsubscribe
//    subscriber.unsubscribe(topic, cb);
//    Thread.sleep(1000);
//    
//    // Publish a message
//    System.out.println("Publisher publishing " + "hello2" + " on topic " + topic);
//    publisher.publish(topic, PubSubProtos.StringMessage.newBuilder().setMessage("hello2").build());
//    cb.awaitNoMessage("hello2");
//  }
//
//
// }
