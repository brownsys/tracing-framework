package edu.brown.cs.systems.retro.wrappers;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.DiskResource;
import edu.brown.cs.systems.retro.wrappers.FileChannelWrapper.WriteCounter;

public class FileOutputStreamWrapper extends FileOutputStream {
    private final StaticPart creator;
    private final WriteCounter counter;

    private class FileOutputStreamProxyWriteCounter extends WriteCounter {
        @Override
        protected void sync() {
            try {
                getChannel().force(true);
            } catch (Exception e) {
                // just squelch
            }
        }
    }

    public FileOutputStreamWrapper(File file, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(file);
        this.creator = jp;
        this.counter = new FileOutputStreamProxyWriteCounter();
    }

    public FileOutputStreamWrapper(File file, boolean append, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(file, append);
        this.creator = jp;
        this.counter = new FileOutputStreamProxyWriteCounter();
    }

    public FileOutputStreamWrapper(FileDescriptor fdObj, JoinPoint.StaticPart jp) {
        super(fdObj);
        this.creator = jp;
        this.counter = new FileOutputStreamProxyWriteCounter();
    }

    public FileOutputStreamWrapper(String name, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(name);
        this.creator = jp;
        this.counter = new FileOutputStreamProxyWriteCounter();
    }

    public FileOutputStreamWrapper(String name, boolean append, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(name, append);
        this.creator = jp;
        this.counter = new FileOutputStreamProxyWriteCounter();
    }

    /**
     * @return the best choice of pointcut to log this method call against.
     */
    private JoinPoint.StaticPart pointcut() {
        return JoinPointTracking.Caller.get(creator);
    }

    private FileChannelWrapper channel = null;

    @Override
    public FileChannel getChannel() {
        if (channel == null) {
            synchronized (this) {
                if (channel == null)
                    channel = new FileChannelWrapper(this, super.getChannel(), counter, pointcut());
            }
        }
        return channel;
    }

    @Override
    public void close() throws IOException {
        close(pointcut());
    }

    @Override
    public void flush() throws IOException {
        flush(pointcut());
    }

    @Override
    public void write(int b) throws IOException {
        write(b, pointcut());
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, pointcut());
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        write(b, off, len, pointcut());
    }

    public void close(JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Close.starting(this, jp, true);
        try {
            super.close();
        } finally {
            DiskResource.Close.finished(this, jp, true);
        }
    }

    public void flush(JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Flush.starting(this, jp, true);
        try {
            super.flush();
        } finally {
            DiskResource.Flush.finished(this, jp, true);
        }
    }

    public void write(int b, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, 1, jp, true);
        try {
            super.write(b);
        } finally {
            DiskResource.Write.finished(this, 1, jp, true);
            counter.wrote(1);
        }
    }

    public void write(byte[] b, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, b.length, jp, true);
        try {
            super.write(b);
        } finally {
            DiskResource.Write.finished(this, b.length, jp, true);
            counter.wrote(b.length);
        }
    }

    public void write(byte[] b, int off, int len, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, len, jp, true);
        try {
            super.write(b, off, len);
        } finally {
            DiskResource.Write.finished(this, len, jp, true);
            counter.wrote(len);
        }
    }

}
