package edu.brown.cs.systems.retro.wrappers;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.DiskResource;

public class FileInputStreamWrapper extends FileInputStream {

    private final JoinPoint.StaticPart creator;

    public FileInputStreamWrapper(File file, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(file);
        this.creator = jp;
    }

    public FileInputStreamWrapper(FileDescriptor fdObj, JoinPoint.StaticPart jp) {
        super(fdObj);
        this.creator = jp;
    }

    public FileInputStreamWrapper(String name, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(name);
        this.creator = jp;
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
                    channel = new FileChannelWrapper(this, super.getChannel(), pointcut());
            }
        }
        return channel;
    }

    @Override
    public void close() throws IOException {
        close(pointcut());
    }

    @Override
    public int read() throws IOException {
        return read(pointcut());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(pointcut(), b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return read(pointcut(), b, off, len);
    }

    public void close(JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Close.starting(this, jp, true);
        try {
            super.close();
        } finally {
            DiskResource.Close.finished(this, jp, true);
        }
    }

    public int read(JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Read.starting(this, 1, jp, true);
        try {
            return super.read();
        } finally {
            DiskResource.Read.finished(this, 1, jp, true);
        }
    }

    public int read(JoinPoint.StaticPart jp, byte[] b) throws IOException {
        DiskResource.Read.starting(this, b.length, jp, true);
        int numRead = 0;
        try {
            return numRead = super.read(b);
        } finally {
            DiskResource.Read.finished(this, numRead, jp, true);
        }
    }

    public int read(JoinPoint.StaticPart jp, byte[] b, int off, int len) throws IOException {
        DiskResource.Read.starting(this, len, jp, true);
        int numRead = 0;
        try {
            return numRead = super.read(b, off, len);
        } finally {
            DiskResource.Read.finished(this, numRead, jp, true);
        }
    }

}
