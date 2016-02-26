package edu.brown.cs.systems.pubsub.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MessageReader {

    private static final Logger log = LoggerFactory.getLogger(MessageReader.class);

    private final SocketChannel channel;

    public MessageReader(SocketChannel channel) {
        this.channel = channel;
    }

    private ByteBuffer sizeBuf = ByteBuffer.allocate(4);
    private ByteBuffer msgBuf = null;

    public abstract void OnMessage(byte[] msg);

    /**
     * Reads from the channel, handling messages, until no more bytes can be
     * read. Returns true if the connection is still open, or false otherwise
     */
    public boolean read() throws IOException {
        log.debug("{} reading from channel {}", this, channel);
        // Loop until thread is interrupted or no more data to read
        while (!Thread.currentThread().isInterrupted()) {
            // If we haven't finished reading the size, read some more
            if (sizeBuf.hasRemaining()) {
                int numRead = channel.read(sizeBuf);
                if (numRead == -1) {
                    log.info("{} has reached end of stream", channel);
                    return false;
                } else {
                    log.debug("Read {} length bytes", numRead);
                }
            }

            // If we still haven't finished reading the size, we're done
            if (sizeBuf.hasRemaining()) {
                break;
            }

            // Parse size, allocate message buffer
            if (msgBuf == null) {
                sizeBuf.rewind();
                int size = sizeBuf.getInt();
                log.debug("Reading message of size {}", size);
                msgBuf = ByteBuffer.allocate(size);
            }

            // Read message until complete
            if (msgBuf.hasRemaining()) {
                int numRead = channel.read(msgBuf);
                if (numRead == -1) {
                    log.info("{} has reached end of stream", channel);
                    return false;
                } else {
                    log.debug("Read {} bytes of message, with {} remaining", numRead, msgBuf.remaining());
                }
            }

            // If there are still more bytes to come, return
            if (msgBuf.hasRemaining()) {
                break;
            }

            // Call message handler
            try {
                log.debug("Handling message {}", msgBuf.array());
                OnMessage(msgBuf.array());
            } finally {
                // Reset the state
                msgBuf = null;
                sizeBuf.rewind();
            }
        }
        return true;
    }
}
