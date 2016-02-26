package edu.brown.cs.systems.pubsub;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.pubsub.PubSubProtos.ControlMessage;
import edu.brown.cs.systems.pubsub.io.TopicReader;
import edu.brown.cs.systems.pubsub.io.TopicWriter;
import edu.brown.cs.systems.pubsub.message.ProtobufMessage;
import edu.brown.cs.systems.pubsub.message.TopicMessage;

public class PubSubClient extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PubSubClient.class);

    // Topic on which control messages are sent
    private final String CONTROL_TOPIC = ConfigFactory.load().getString("pubsub.topics.control");

    // Server to connect to
    private final String hostname;
    private final int port;

    // Outgoing messages
    private TopicMessage current = null;
    private final BlockingQueue<TopicMessage> pending = Queues.newLinkedBlockingQueue();

    // Selector
    private final Selector selector;

    // Subscribers to incoming messages
    private final Multimap<ByteString, Subscriber<?>> subscribers = HashMultimap.create();

    PubSubClient(String hostname, int port) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.selector = Selector.open();
    }

    /**
     * Close this client's connection to the server and terminate the client
     * thread
     */
    public void close() {
        try {
            this.interrupt();
            this.join();
        } catch (InterruptedException e) {
        }
    }

    /** Publish a message to the server */
    public void publish(String topic, Message message) {
        log.debug("Publishing topic {}, message {}", topic, message);
        // Enqueue the message
        pending.offer(new ProtobufMessage(topic, message));

        // Wake up the selector
        selector.wakeup();
    }

    public synchronized void subscribe(String topic, Subscriber<?> subscriber) {
        log.info("Subscribing topic {}", topic);
        ByteString topicBytes = ByteString.copyFrom(topic.getBytes());
        if (subscribers.put(topicBytes, subscriber)) {
            publish(CONTROL_TOPIC, ControlMessage.newBuilder().addTopicSubscribe(topicBytes).build());
        }
    }

    public synchronized void unsubscribe(String topic, Subscriber<?> subscriber) {
        log.info("Subscribing topic {}", topic);
        ByteString topicBytes = ByteString.copyFrom(topic.getBytes());
        subscribers.remove(topicBytes, subscriber);
        if (subscribers.get(topicBytes).size() == 0) {
            publish(CONTROL_TOPIC, ControlMessage.newBuilder().addTopicUnsubscribe(topicBytes).build());
        }
    }

    /** Simply call each subscriber to the topic, passing the message */
    private synchronized void OnMessage(byte[] topic, byte[] message) {
        for (Subscriber<?> subscriber : subscribers.get(ByteString.copyFrom(topic))) {
            try {
                subscriber.OnMessage(message);
            } catch (Exception e) {
                // Ignore exceptions once they reach us here
            }
        }
    }

    /**
     * Reads topic messages from the channel and hands them over to the
     * onmessage callback
     */
    private class ClientReader extends TopicReader {

        public ClientReader(SocketChannel channel) {
            super(channel);
        }

        @Override
        public void OnMessage(byte[] topic, byte[] message) {
            PubSubClient.this.OnMessage(topic, message);
        }

    }

    /**
     * Writes the current message to the channel
     */
    private class ClientWriter extends TopicWriter {
        public ClientWriter(SocketChannel channel) {
            super(channel);
        }

        public TopicMessage CurrentMessage() {
            if (current == null) {
                current = pending.poll();
            }
            return current;
        }

        public void MessageSendComplete() {
            current = null;
        }
    }

    /**
     * The main client loop when we've connected to the server. Selects on the
     * selector and does async read/write
     */
    private void ClientThreadMainLoop(SocketChannel channel) throws IOException {
        // Create a message reader and writer
        log.debug("Creating client reader and writer for channel {}", channel);
        ClientReader reader = new ClientReader(channel);
        ClientWriter writer = new ClientWriter(channel);
        SelectionKey k = channel.register(selector, SelectionKey.OP_READ);

        // Send subscriptions if we have some already
        synchronized (this) {
            if (subscribers.size() > 0) {
                log.info("Sending existing subscriptions");
                ControlMessage.Builder msg = ControlMessage.newBuilder();
                for (ByteString topic : subscribers.keySet()) {
                    msg.addTopicSubscribe(topic);
                }
                publish(CONTROL_TOPIC, msg.build());
            }
        }

        // Do main loop
        while (!Thread.currentThread().isInterrupted()) {
            // Register for read and write as needed
            int ops = SelectionKey.OP_READ;
            if (writer.canWrite()) {
                ops |= SelectionKey.OP_WRITE;
            }
            k.interestOps(ops);

            // Wait until we can do something
            selector.select();

            // Deal with keys
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                // Get next key, remove from selected
                SelectionKey selected = it.next();
                it.remove();

                // Cancel if necessary
                if (!selected.isValid()) {
                    selected.cancel();
                }

                // Do nothing if its not our key... why this can happen, I do
                // not know
                if (selected != k) {
                    continue;
                }

                // Check to see whether we can read and write
                if (k.isWritable()) {
                    log.debug("Writing");
                    writer.write();
                } else if (k.isReadable()) {
                    log.debug("Reading");
                    if (!reader.read()) {
                        log.info("Reader reached end of stream");
                        k.cancel();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Establishes a connection to the server, then runs the client thread main
     * loop Returns if connection fails or if the connection is interrupted
     */
    private void ClientThreadRun() throws IOException {
        SocketChannel channel = null;
        try {
            // Connect to the server synchronously
            log.info("Attempting connection to {}:{}", hostname, port);
            channel = SocketChannel.open(new InetSocketAddress(hostname, port));

            // Convert to non-blocking and run the main loop
            channel.configureBlocking(false);
            ClientThreadMainLoop(channel);
        } finally {
            // No matter what happens, close the channel
            if (channel != null) {
                channel.close();
            }
        }
    }

    /**
     * Keeps trying to establish connection, periodically sleeping, until the
     * thread is interrupted
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                try {
                    ClientThreadRun();
                } catch (IOException e1) {
                    // Connection dropped or something like that. Sleep then try
                    // again
                    if (!Thread.currentThread().isInterrupted()) {
                        log.info("Sleeping for 1 second");
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e2) {
                // Thread was interrupted; return
                return;
            }
        }
    }

    /**
     * Subscriber to a topic. Uses reflection to parse byte arrays to protobuf
     * messages
     */
    public static abstract class Subscriber<T extends Message> {

        private final Parser<T> parser;

        public Subscriber() {
            Parser<T> parser = null;
            try {
                Class<?> cl = getClass();
                while (!Subscriber.class.equals(cl.getSuperclass())) {
                    // case of multiple inheritance, we are trying to get the
                    // first available generic info
                    if (cl.getGenericSuperclass() instanceof ParameterizedType) {
                        break;
                    }
                    cl = cl.getSuperclass();
                }
                Class<T> type = ((Class<T>) ((ParameterizedType) cl.getGenericSuperclass()).getActualTypeArguments()[0]);
                parser = (Parser<T>) type.getDeclaredField("PARSER").get(null);
            } catch (Exception e) {
                System.out.println("Error: callback creation failed");
                e.printStackTrace();
            }
            this.parser = parser;
        }

        public Subscriber(Class<T> type) {
            Parser<T> parser = null;
            try {
                parser = (Parser<T>) type.getDeclaredField("PARSER").get(null);
            } catch (Exception e) {
                System.out.println("Error: callback creation failed");
                e.printStackTrace();
            }
            this.parser = parser;
        }

        protected abstract void OnMessage(T message);

        void OnMessage(byte[] payload) {
            try {
                OnMessage(parser.parseFrom(payload));
            } catch (InvalidProtocolBufferException e) {
                System.out.println("PubSub error deserializing message");
                e.printStackTrace();
            }
        }
    }

}
