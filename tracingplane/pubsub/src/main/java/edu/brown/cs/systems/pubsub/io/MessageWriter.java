package edu.brown.cs.systems.pubsub.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(MessageWriter.class);

    private final SocketChannel channel;

    // buf[0] is the length, buf[1] is the message
    private ByteBuffer[] bufs = new ByteBuffer[2];

    public MessageWriter(SocketChannel channel) {
        this.channel = channel;
        this.bufs[0] = ByteBuffer.allocate(4);
        this.bufs[1] = null;
    }

    /** True if there is data to be written */
    public boolean canWrite() {
        if (bufs[1] == null) {
            byte[] current = Current();
            if (current == null) {
                return false; // No pending messages
            } else {
                bufs[0].rewind();
                bufs[0].putInt(current.length);
                bufs[0].rewind();
                bufs[1] = ByteBuffer.wrap(current);
            }
        }
        return true;
    }

    /** Get the current message to be written */
    public abstract byte[] Current();

    /** We're done writing the current message; get the next one */
    public abstract void MoveNext();

    /** Returns true if there is more data to be written, false otherwise */
    public boolean write() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            // Get the next message if necessary
            if (!canWrite()) {
                return false;
            }

            // Write as much as possible
            if (bufs[1].hasRemaining()) {
                long numWritten = channel.write(bufs);
                log.debug("Wrote {} bytes to {}", numWritten, channel);
            }

            // If there are still bytes to be written, return
            if (bufs[1].hasRemaining()) {
                log.debug("More bytes to be written, returning");
                return true;
            }

            // Message send completed
            bufs[1] = null;
            MoveNext();
        }
        return true; // Only here to compile
    }

}
