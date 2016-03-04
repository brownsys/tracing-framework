package edu.brown.cs.systems.pubsub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.pubsub.PubSubProtos.ControlMessage;
import edu.brown.cs.systems.pubsub.io.TopicReader;
import edu.brown.cs.systems.pubsub.io.TopicWriter;
import edu.brown.cs.systems.pubsub.message.ByteMessage;
import edu.brown.cs.systems.pubsub.message.TopicMessage;

public class PubSubServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PubSubServer.class);

    // Topic on which control messages are sent
    private final byte[] CONTROL_TOPIC = ConfigFactory.load().getString("pubsub.topics.control").getBytes();

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final SelectionKey serverKey;

    private final Set<ConnectedClient> connected = Sets.newHashSet();
    private final Table<ConnectedClient, ByteString, Boolean> subscriptions = HashBasedTable.create();

    /**
     * Creates and immediately binds a pubsub server
     * 
     * @param hostname
     *            the hostname to bind to
     * @param port
     *            the port to bind to
     * @throws IOException
     *             if we were unable to create/bind the server, open a selector,
     *             or register the selector
     */
    public PubSubServer(String hostname, int port) throws IOException {
        // Open the server socket, bind it, and make non-blocking
        log.info("Creating server hostname {}, port {}", hostname, port);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(hostname, port));
        serverSocketChannel.configureBlocking(false);

        // Create selector
        log.debug("Creating and registering selector");
        selector = Selector.open();
        serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Shutdown the server and wait for the server main thread to complete
     */
    public void shutdown() {
        log.info("Interrupting server thread");
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            return;
        }
    }

    /**
     * Main run method for the server thread
     */
    @Override
    public void run() {
        try {
            ServerThreadMain();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                log.debug("Closing server socket channel");
                serverSocketChannel.close();
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main server loop
     * 
     * @throws IOException
     *             if we get an IO exception while selecting on the selector, or
     *             if an IO exception occurs while attempting to accept a new
     *             connection
     */
    private void ServerThreadMain() throws IOException {
        log.debug("Running main thread");
        while (!Thread.currentThread().isInterrupted()) {
            // Wait for something
            selector.select();

            // See what changed
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                // Get and remove next key
                SelectionKey k = it.next();
                it.remove();

                // See if the server key is acceptable
                if (k == serverKey && k.isAcceptable()) {
                    SocketChannel accepted = serverSocketChannel.accept();
                    if (accepted != null) {
                        log.info("Accepted incoming connection {}", accepted);
                        try {
                            connected.add(new ConnectedClient(accepted));
                        } catch (ClosedChannelException e) {
                            // Channel immediately was closed before creating a
                            // client
                        }
                    }
                    log.debug("Acceptable channel {} could not accept", k);
                    continue;
                }

                // Get the client for this key
                ConnectedClient client = null;
                try {
                    client = (ConnectedClient) k.attachment();
                } catch (ClassCastException e) {
                    log.debug("No connected client for key {}: {}", k, e);
                    log.debug("Cancelling key {}", k);
                    k.cancel();
                    continue;
                }

                // Not sure when this could happen, but just in case...
                if (client == null) {
                    log.debug("No connected client for key {}", k);
                    log.debug("Cancelling key {}", k);
                    k.cancel();
                    continue;
                }

                // If key is no longer valid, remove the client
                if (!k.isValid()) {
                    log.debug("Closing client with invalid key {}: {}", k, client);
                    client.close();
                    continue;
                }

                // To to read/write, and remove client on error
                try {
                    if (k.isReadable()) {
                        log.debug("{} readable", client.channel);
                        if (!client.reader.read()) {
                            log.info("{} reached end of stream", client.channel);
                            client.close();
                            continue;
                        }
                    }
                    if (k.isWritable()) {
                        log.debug("{} writeable", client.channel);
                        client.writer.write();
                    }
                    client.UpdateSelector();
                } catch (IOException e) {
                    log.info("IOException for client {}: {}", client, e);
                    client.close();
                    continue;
                }
            }
        }
    }

    /**
     * Handle an incoming message from a client
     */
    private void route(ConnectedClient from, TopicMessage message) {
        // Additional handling for control messages
        if (Arrays.equals(message.topic(), CONTROL_TOPIC)) {
            handleControlMessage(from, message.message());
        }

        // Route the message
        log.debug("Routing message on topic {}", message.topic());
        log.debug("Subscriptions: {}", subscriptions);
        for (ConnectedClient client : subscriptions.column(ByteString.copyFrom(message.topic())).keySet()) {
            log.debug("Routing from {} to {} message {}", from, client, message);
            client.outgoing.offer(message);
            client.UpdateSelector();
        }
    }

    /**
     * Handles a control message from a client (publish, subscribe, etc.)
     */
    private void handleControlMessage(ConnectedClient client, byte[] bytes) {
        ControlMessage message;
        try {
            message = ControlMessage.parseFrom(bytes);
            log.debug("Control message from client {}: {}", client, message);
        } catch (InvalidProtocolBufferException e) {
            // Bad message, ignore
            log.warn("Bad control message from client {}: {}", client, e);
            return;
        }

        // Do unsubscribes
        for (ByteString unsubscribe : message.getTopicUnsubscribeList()) {
            if (log.isInfoEnabled()) {
                if (unsubscribe.isValidUtf8()) {
                    log.info("{} unsubscribing from {}", client, unsubscribe.toStringUtf8());
                } else {
                    log.info("{} unsubscribing from {}", client, unsubscribe);
                }
            }
            subscriptions.remove(client, unsubscribe);
        }

        // Do subscribes
        for (ByteString subscribe : message.getTopicSubscribeList()) {
            if (log.isInfoEnabled()) {
                if (subscribe.isValidUtf8()) {
                    log.info("{} subscribing to {}", client, subscribe.toStringUtf8());
                } else {
                    log.info("{} subscribing to {}", client, subscribe);
                }
            }
            subscriptions.put(client, subscribe, true);
        }
    }

    /**
     * A client that connected to the pubsub server Each client has their own
     * queue of outgoing messages for topics they've subscribed to If a
     * connection drops, the client is removed
     */
    private class ConnectedClient {
        private final SocketChannel channel; // Clients channel
        private final SelectionKey key; // Clients selection key

        final Queue<TopicMessage> outgoing = Queues.newArrayDeque(); // Queue of
                                                                     // outgoing
                                                                     // messages
        final TopicReader reader; // Reads incoming messages
        final TopicWriter writer; // Writes outgoing messages

        /**
         * Client has connected on the provided channel
         * 
         * @throws IOException
         */
        public ConnectedClient(SocketChannel channel) throws IOException {
            // Save the channel, configure non-blocking
            this.channel = channel;
            this.channel.configureBlocking(false);

            // Create a channel reader that routes incoming messages
            this.reader = new TopicReader(channel) {
                public void OnMessage(byte[] topic, byte[] message) {
                    route(ConnectedClient.this, new ByteMessage(topic, message));
                }
            };

            // Create a channel writer for outgoing messages
            this.writer = new TopicWriter(channel) {
                public TopicMessage CurrentMessage() {
                    return outgoing.peek();
                }

                public void MessageSendComplete() {
                    outgoing.poll();
                }
            };

            // Nothing to write yet
            key = channel.register(selector, SelectionKey.OP_READ, this);
        }

        public void UpdateSelector() {
            if (this.writer.canWrite()) {
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }
        }

        public void close() {
            log.info("Closing client on channel {}", channel);
            try {
                subscriptions.row(this).clear(); // Remove any subscriptions
                outgoing.clear(); // Clear pending messages
                key.cancel(); // Cancel selector key
                channel.close(); // Close the channel
            } catch (IOException e) {
                // Ignore, we're closing anyway
            } finally {
                connected.remove(this); // Not connected any more
            }
        }

        @Override
        public String toString() {
            try {
                return this.channel.getRemoteAddress().toString();
            } catch (IOException e) {
                return super.toString();
            }
        }
    }

}
