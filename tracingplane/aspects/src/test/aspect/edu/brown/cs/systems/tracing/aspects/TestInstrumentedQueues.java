package edu.brown.cs.systems.tracing.aspects;

import java.util.concurrent.BlockingQueue;

import org.junit.Test;

import com.google.common.collect.Queues;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.tracing.aspects.Annotations.InstrumentQueues;
import edu.brown.cs.systems.tracing.aspects.Annotations.InstrumentedQueueElement;
import edu.brown.cs.systems.tracing.aspects.Queues.QueueElementWithBaggage;
import junit.framework.TestCase;

public class TestInstrumentedQueues extends TestCase {
    
    @Override
    public void setUp() {
        Baggage.discard();
    }
    
    @Override
    public void tearDown() {
        Baggage.discard();
    }
    
    public static void baggageput(String a, String b, String c) {
        BaggageContents.add(a, b, c);
    }
    
    public static void baggageexpect(String a, String b, String c) {
        assertEquals(1, BaggageContents.get(a, b).size());
        assertEquals(ByteString.copyFromUtf8(c), BaggageContents.get(a, b).iterator().next());
    }
    
    public static void baggageexpectempty() {
        assertTrue(BaggageContents.isEmpty());
    }
    
    @InstrumentedQueueElement
    public static class MyQueueElement {
        
    }
    
    public static class MyQueueElement2 {
        
    }

    @InstrumentQueues
    public static class T1 {
        private static final BlockingQueue<MyQueueElement> q1 = Queues.newLinkedBlockingQueue();
        public static void add(MyQueueElement e) {
            q1.add(e);
        }
        public static MyQueueElement take() throws InterruptedException {
            return q1.take();
        }
    }
    
    @Test
    public void testInstrumentedQueueInterfaces() throws InterruptedException {
        MyQueueElement e = new MyQueueElement();
        MyQueueElement2 e2 = new MyQueueElement2();
        assertTrue(e instanceof QueueElementWithBaggage);
        assertFalse(e2 instanceof QueueElementWithBaggage);
    }
    
    /** T1 has InstrumentQueues and InstrumentedQueueElement annotations, so should be instrumented */
    @Test
    public void testInstrumentedQueue1() throws InterruptedException {
        MyQueueElement e = new MyQueueElement();
        
        baggageexpectempty();
        baggageput("a", "b", "c");
        baggageexpect("a", "b", "c");
        T1.add(e);
        baggageexpect("a", "b", "c");
        Baggage.discard();
        baggageexpectempty();
        T1.take();
        baggageexpect("a", "b", "c");
    }

    public static class T2 {
        private static final BlockingQueue<MyQueueElement2> q2 = Queues.newLinkedBlockingQueue();
        public static void add(MyQueueElement2 e) {
            q2.add(e);
        }
        public static MyQueueElement2 take() throws InterruptedException {
            return q2.take();
        }
    }
    
    /** MyQueueElement2 does not have the InstrumentedQueueElement annotation or InstrumentQueues */
    @Test
    public void testUninstrumentedQueue() throws InterruptedException {
        MyQueueElement2 e = new MyQueueElement2();
        
        baggageexpectempty();
        baggageput("a", "b", "c");
        baggageexpect("a", "b", "c");
        T2.add(e);
        baggageexpect("a", "b", "c");
        Baggage.discard();
        baggageexpectempty();
        T2.take();
        baggageexpectempty();
    }

    public static class T3 {
        private static final BlockingQueue<MyQueueElement> q1 = Queues.newLinkedBlockingQueue();
        public static void add(MyQueueElement e) {
            q1.add(e);
        }
        public static MyQueueElement take() throws InterruptedException {
            return q1.take();
        }
    }
    
    /** T3 does not have the InstrumentQueues annotation */
    @Test
    public void testUninstrumentedQueue2() throws InterruptedException {
        MyQueueElement e = new MyQueueElement();
        
        baggageexpectempty();
        baggageput("a", "b", "c");
        baggageexpect("a", "b", "c");
        T3.add(e);
        baggageexpect("a", "b", "c");
        Baggage.discard();
        baggageexpectempty();
        T3.take();
        baggageexpectempty();
    }

    @InstrumentQueues
    public static class T4 {
        private static final BlockingQueue<MyQueueElement2> q2 = Queues.newLinkedBlockingQueue();
        public static void add(MyQueueElement2 e) {
            q2.add(e);
        }
        public static MyQueueElement2 take() throws InterruptedException {
            return q2.take();
        }
    }
    
    /** T4 does not have the InstrumentedQueueElement annotation */
    @Test
    public void testUninstrumentedQueue3() throws InterruptedException {
        MyQueueElement2 e = new MyQueueElement2();
        
        baggageexpectempty();
        baggageput("a", "b", "c");
        baggageexpect("a", "b", "c");
        T4.add(e);
        baggageexpect("a", "b", "c");
        Baggage.discard();
        baggageexpectempty();
        T4.take();
        baggageexpectempty();
    }
    
    public static class MyQueueElement3 extends MyQueueElement {
    }
    
    @InstrumentQueues
    public static class T5 {
    }
    
    public static class T6 {
        private static final BlockingQueue<MyQueueElement3> q = Queues.newLinkedBlockingQueue();
        public static void add(MyQueueElement3 e) {
            q.add(e);
        }
        public static MyQueueElement3 take() throws InterruptedException {
            return q.take();
        }
    }
    
    /** T6 inherits instrumentation */
    @Test
    public void testInstrumentationInheritance() throws InterruptedException {
        MyQueueElement3 e = new MyQueueElement3();
        
        baggageexpectempty();
        baggageput("a", "b", "c");
        baggageexpect("a", "b", "c");
        T6.add(e);
        baggageexpect("a", "b", "c");
        Baggage.discard();
        baggageexpectempty();
        T6.take();
        baggageexpectempty();
    }

}
