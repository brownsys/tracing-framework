package edu.brown.cs.systems.pubsub.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

public class TestMessageIO extends TestCase {
    
    private static final Random r = new Random(100);
    
    public static int randomBetween(int min, int max) {
        return r.nextInt(max-min)+min;
    }
    
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        r.nextBytes(bytes);
        return bytes;
    }
    
    public static ByteString randomByteString(int length) {
        return ByteString.copyFrom(randomBytes(length));
    }

    // Saves the recevied message to check we got it!
    private class TestMessageReader extends MessageReader {

        public TestMessageReader(SocketChannel channel) {
            super(channel);
        }

        public byte[] msgReceived = null;

        @Override
        public void OnMessage(byte[] msg) {
            msgReceived = msg;
        }

    }

    private class TestMessageReader2 extends MessageReader {

        public TestMessageReader2(SocketChannel channel) {
            super(channel);
        }

        public List<byte[]> msgsReceived = Lists.newArrayList();

        @Override
        public void OnMessage(byte[] msg) {
            msgsReceived.add(msg);
        }

    }

    private class TestMessageWriter extends MessageWriter {

        public TestMessageWriter(SocketChannel channel) {
            super(channel);
        }

        private Queue<byte[]> msgs = Queues.newArrayDeque();

        public void Enqueue(byte[] msg) {
            msgs.add(msg);
        }

        @Override
        public byte[] Current() {
            if (msgs.size() > 0) {
                return msgs.peek();
            } else {
                return null;
            }
        }

        @Override
        public void MoveNext() {
            msgs.poll();
        }

    }

    private static final int SELECTION_TIMEOUT_MS = 1000;

    private static boolean awaitOp(Selector selector, SelectionKey key, int op) throws IOException {
        long end = System.currentTimeMillis() + SELECTION_TIMEOUT_MS;
        boolean selected = false;
        while (!selected) {
            // Check timeout
            long remaining = end - System.currentTimeMillis();
            if (remaining < 0) {
                break;
            }

            // Select up to remaining millis
            selector.select(remaining);

            // Handle op if possible
            switch (op) {
            case SelectionKey.OP_READ:
                selected |= key.isReadable();
                break;
            case SelectionKey.OP_ACCEPT:
                selected |= key.isAcceptable();
                break;
            case SelectionKey.OP_WRITE:
                selected |= key.isWritable();
                break;
            case SelectionKey.OP_CONNECT:
                selected |= key.isConnectable();
                break;
            }
        }
        return selected;
    }

    /** Waits for up to 1 second for the server to be acceptable */
    private static void awaitAcceptable(ServerSocketChannel channel) throws IOException {
        Selector selector = Selector.open();
        SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
        try {
            assertEquals(true, awaitOp(selector, key, SelectionKey.OP_ACCEPT));
        } finally {
            // Cancel key and close selector
            key.cancel();
            selector.close();
        }
    }

    /** Waits for up to 1 second for the channel to process the op */
    private static void await(SocketChannel channel, int op) throws IOException {
        Selector selector = Selector.open();
        SelectionKey key = channel.register(selector, op);
        try {
            assertEquals(true, awaitOp(selector, key, op));
        } finally {
            // Cancel key and close selector
            key.cancel();
            selector.close();
        }
    }

    private static void awaitConnectable(SocketChannel channel) throws IOException {
        await(channel, SelectionKey.OP_CONNECT);
    }

    private static void awaitReadable(SocketChannel channel) throws IOException {
        await(channel, SelectionKey.OP_READ);
    }

    private static void awaitWriteable(SocketChannel channel) throws IOException {
        await(channel, SelectionKey.OP_WRITE);
    }

    private static ServerSocketChannel createServer() throws IOException {
        // Create a server on a random port
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", 0));
        server.configureBlocking(false);
        return server;
    }

    private static SocketChannel connectClient(ServerSocketChannel server) throws IOException {
        // Connect a client to the server
        SocketChannel client = SocketChannel.open();
        client.configureBlocking(false);
        client.connect(server.getLocalAddress());
        return client;
    }

    private static SocketChannel acceptClient(ServerSocketChannel server) throws IOException {
        // Wait for up to 1 second for the client to connect
        awaitAcceptable(server);

        // Accept the server connection
        SocketChannel serverConnection = server.accept();
        serverConnection.configureBlocking(false);

        assertNotNull(serverConnection);
        return serverConnection;
    }

    private static void completeConnection(SocketChannel client) throws IOException {
        // Complete the client connection
        awaitConnectable(client);
        client.finishConnect();
    }

    private static class NIOConnection {
        public final ServerSocketChannel server;
        public final SocketChannel clientConnection, serverConnection;

        public NIOConnection(ServerSocketChannel server, SocketChannel clientConnection, SocketChannel serverConnection) {
            this.server = server;
            this.clientConnection = clientConnection;
            this.serverConnection = serverConnection;
        }

        public static NIOConnection setup() throws IOException {
            ServerSocketChannel server = createServer();
            SocketChannel clientConnection = connectClient(server);
            SocketChannel serverConnection = acceptClient(server);
            completeConnection(clientConnection);
            return new NIOConnection(server, clientConnection, serverConnection);
        }
    }

    private static byte[] generateMessage(int length) {
        return randomBytes(length);
    }

    private static void write(SocketChannel channel, byte[] msg) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(msg.length + 4);
        out.putInt(msg.length);
        out.put(msg);
        out.rewind();
        while (out.hasRemaining()) {
            channel.write(out);
        }
    }

    public void testReader() throws IOException {
        // Set up the connections
        NIOConnection conn = NIOConnection.setup();

        // Create a message reader for the client
        TestMessageReader tr = new TestMessageReader(conn.clientConnection);

        int msgCount = 1000;
        int maxMsgSize = 10000;
        int minMsgSize = 1;

        for (int i = 0; i < msgCount; i++) {
            // Generate a random message
            byte[] msg = generateMessage(randomBetween(minMsgSize, maxMsgSize));

            // Write the message from server to client
            write(conn.serverConnection, msg);

            // Wait for the client to be readable, up to 1 second
            awaitReadable(conn.clientConnection);

            // Read from the client
            tr.read();

            // Check the message was received
            assertNotNull(tr.msgReceived);

            // Check message length
            assertEquals(msg.length, tr.msgReceived.length);

            // Check message contents
            assert (Arrays.equals(msg, tr.msgReceived));
        }
    }

    public void testWriter() throws IOException {
        // Set up the connections
        NIOConnection conn = NIOConnection.setup();

        // Create a message writer for the server
        TestMessageWriter tw = new TestMessageWriter(conn.serverConnection);

        // Create a message reader for the client
        TestMessageReader tr = new TestMessageReader(conn.clientConnection);

        int msgCount = 1000;
        int maxMsgSize = 10000;
        int minMsgSize = 1;

        for (int i = 0; i < msgCount; i++) {
            // Generate a random message
            byte[] msg = generateMessage(randomBetween(minMsgSize, maxMsgSize));
            tw.Enqueue(msg);

            // Write the message from server to client
            tw.write();

            // Wait for the client to be readable, up to 1 second
            awaitReadable(conn.clientConnection);

            // Read from the client
            tr.read();

            // Check the message was received
            assertNotNull(tr.msgReceived);

            // Check message length
            assertEquals(msg.length, tr.msgReceived.length);

            // Check message contents
            assert (Arrays.equals(msg, tr.msgReceived));
        }
    }

    public void testWriteOverflow() throws IOException, InterruptedException {
        // Set up the connections
        NIOConnection conn = NIOConnection.setup();

        int maxMsgSize = 10000;
        int minMsgSize = 1;

        // Create a message writer for the server
        TestMessageWriter tw = new TestMessageWriter(conn.serverConnection);

        // Keep writing to the channel until we block
        List<byte[]> msgs = Lists.newArrayList();
        while (!tw.write()) {
            // Generate a random message
            byte[] msg = generateMessage(randomBetween(minMsgSize, maxMsgSize));
            tw.Enqueue(msg);
            msgs.add(msg);
        }

        // Create a message reader for the client
        TestMessageReader2 tr = new TestMessageReader2(conn.clientConnection);

        // Read from the client
        tr.read();

        // Check that we received all but one messages
        assertEquals(msgs.size() - 1, tr.msgsReceived.size());

        for (int i = 0; i < tr.msgsReceived.size(); i++) {
            byte[] sent = msgs.get(i);
            byte[] received = tr.msgsReceived.get(i);

            // Check message length
            assertEquals(sent.length, received.length);

            // Check message contents
            assert (Arrays.equals(sent, received));
        }

        // Now fill up the buffer again
        int numreceived = tr.msgsReceived.size();
        while (!tw.write()) {
            // Generate a random message
            byte[] msg = generateMessage(randomBetween(minMsgSize, maxMsgSize));
            tw.Enqueue(msg);
            msgs.add(msg);
        }

        Thread.sleep(50);

        // Read from the client some more
        tr.read();

        // Check that we received more messages, and still all but one
        assertNotSame(numreceived, tr.msgsReceived.size());
        assertEquals(msgs.size() - 1, tr.msgsReceived.size());

        for (int i = 0; i < tr.msgsReceived.size(); i++) {
            byte[] sent = msgs.get(i);
            byte[] received = tr.msgsReceived.get(i);

            // Check message length
            assertEquals(sent.length, received.length);

            // Check message contents
            assert (Arrays.equals(sent, received));
        }

    }

    public void testWriterBulk() throws IOException {
        // Set up the connections
        NIOConnection conn = NIOConnection.setup();

        // Create a message writer for the server
        TestMessageWriter tw = new TestMessageWriter(conn.serverConnection);

        // Create a message reader for the client
        TestMessageReader2 tr = new TestMessageReader2(conn.clientConnection);

        int msgCount = 100;
        int maxMsgSize = 1000;
        int minMsgSize = 1;

        // Create the messages
        List<byte[]> msgs = Lists.newArrayList();
        for (int i = 0; i < msgCount; i++) {
            // Generate a random message
            byte[] msg = generateMessage(randomBetween(minMsgSize, maxMsgSize));
            tw.Enqueue(msg);
            msgs.add(msg);
        }

        // Write the messages from server to client
        tw.write();

        // Wait for the client to be readable, up to 1 second
        awaitReadable(conn.clientConnection);

        // Read from the client
        tr.read();

        for (int i = 0; i < msgCount; i++) {
            byte[] sent = msgs.get(i);
            byte[] received = tr.msgsReceived.get(i);

            // Check message length
            assertEquals(sent.length, received.length);

            // Check message contents
            assert (Arrays.equals(sent, received));
        }
    }

}
