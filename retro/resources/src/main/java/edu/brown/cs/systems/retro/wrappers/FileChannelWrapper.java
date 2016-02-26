package edu.brown.cs.systems.retro.wrappers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.aspectj.lang.JoinPoint.StaticPart;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.DiskResource;
import edu.brown.cs.systems.retro.resources.Network;

public class FileChannelWrapper extends FileChannel {

    static abstract class WriteCounter {
        private static final boolean sync_after_write = ConfigFactory.load().getBoolean("resource-tracing.disk.sync-after-write");
        private static final long sync_after_write_threshold = ConfigFactory.load().getLong("resource-tracing.disk.sync-threshold");
        private long bytes_since_last_sync = 0;

        public long synced() {
            long ret = bytes_since_last_sync;
            bytes_since_last_sync = 0;
            return ret;
        }

        public void wrote(long bytes) {
            if (bytes > 0) {
                bytes_since_last_sync += bytes;
                if (sync_after_write && bytes_since_last_sync >= sync_after_write_threshold)
                    sync();
            }
        }

        protected abstract void sync();
    }

    private static final WriteCounter DO_NOTHING_WRITE_COUNTER = new WriteCounter() {
        protected void sync() {
        }
    };

    private final StaticPart creator;
    private final FileChannel channel;
    private final Object file;
    private final WriteCounter counter;

    public FileChannelWrapper(Object file, FileChannel channel, StaticPart creator) {
        this(file, channel, DO_NOTHING_WRITE_COUNTER, creator);
    }

    public FileChannelWrapper(Object file, FileChannel channel, WriteCounter counter, StaticPart creator) {
        this.file = file;
        this.channel = channel;
        this.counter = counter;
        this.creator = creator;
    }

    private void start(DiskResource rsrc) {
        start(rsrc, 0);
    }

    private void start(DiskResource rsrc, long estimate) {
        rsrc.starting(file, estimate, JoinPointTracking.Caller.get(creator), true);
    }

    private void finish(DiskResource rsrc, long written) {
        rsrc.finished(file, written, JoinPointTracking.Caller.get(creator), true);
    }

    @Override
    public void force(boolean metaData) throws IOException {
        start(DiskResource.Sync);
        try {
            channel.force(metaData);
        } finally {
            finish(DiskResource.Sync, counter.synced());
        }
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return channel.lock(position, size, shared);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return channel.map(mode, position, size);
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        return channel.position(newPosition);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        start(DiskResource.Read, 1);
        int numRead = 0;
        try {
            return numRead = channel.read(dst);
        } finally {
            finish(DiskResource.Read, numRead);
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        start(DiskResource.Read, 1);
        int numRead = 0;
        try {
            return numRead = channel.read(dst, position);
        } finally {
            finish(DiskResource.Read, numRead);
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        start(DiskResource.Read, 1);
        long numRead = 0;
        try {
            return numRead = channel.read(dsts, offset, length);
        } finally {
            finish(DiskResource.Read, numRead);
        }
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        DiskResource rsrc = DiskResource.Write;
        if (src instanceof SocketChannel) {
            if (Network.isLoopback((SocketChannel) src)) {
                rsrc = DiskResource.TransferFromLoopback;
            } else {
                rsrc = DiskResource.TransferFromNetwork;
            }
        }

        start(rsrc, count);
        long numWritten = 0;
        try {
            return numWritten = channel.transferFrom(src, position, count);
        } finally {
            finish(rsrc, numWritten);
            counter.wrote(numWritten);
        }
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        DiskResource rsrc = DiskResource.Read;
        if (target instanceof SocketChannel) {
            if (Network.isLoopback((SocketChannel) target)) {
                rsrc = DiskResource.TransferToLoopback;
            } else {
                rsrc = DiskResource.TransferToNetwork;
            }
        }

        start(rsrc, count);
        long numRead = 0;
        try {
            return numRead = channel.transferTo(position, count, target);
        } finally {
            finish(rsrc, numRead);
        }
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        return channel.truncate(size);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return channel.tryLock(position, size, shared);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        start(DiskResource.Write);
        int numWritten = 0;
        try {
            return numWritten = channel.write(src);
        } finally {
            finish(DiskResource.Write, numWritten);
            counter.wrote(numWritten);
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        start(DiskResource.Write);
        int numWritten = 0;
        try {
            return numWritten = channel.write(src, position);
        } finally {
            finish(DiskResource.Write, numWritten);
            counter.wrote(numWritten);
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        start(DiskResource.Write);
        long numWritten = 0;
        try {
            return numWritten = channel.write(srcs, offset, length);
        } finally {
            finish(DiskResource.Write, numWritten);
            counter.wrote(numWritten);
        }
    }

    @Override
    protected void implCloseChannel() throws IOException {
        channel.close();
    }
}
