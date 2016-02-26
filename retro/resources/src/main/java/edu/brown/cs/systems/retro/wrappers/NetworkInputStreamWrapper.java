package edu.brown.cs.systems.retro.wrappers;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.Network;

public class NetworkInputStreamWrapper extends FilterInputStream {

    private final Network Read;
    private final StaticPart creator;

    public NetworkInputStreamWrapper(InputStream is, JoinPoint.StaticPart jp) {
        this(is, jp, false);
    }

    public NetworkInputStreamWrapper(InputStream is, JoinPoint.StaticPart jp, boolean isLoopback) {
        super(is);
        this.creator = jp;
        if (isLoopback)
            Read = Network.LoopbackRead;
        else
            Read = Network.Read;
    }

    /**
     * @return the best choice of pointcut to log this method call against.
     */
    private JoinPoint.StaticPart pointcut() {
        return JoinPointTracking.Caller.get(creator);
    }

    @Override
    public int read() throws IOException {
        return this.read(pointcut());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, pointcut());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return this.read(b, off, len, pointcut());
    }

    @Override
    public long skip(long n) throws IOException {
        return this.skip(n, pointcut());
    }

    public int read(JoinPoint.StaticPart jp) throws IOException {
        Read.starting(in, jp);
        try {
            return in.read();
        } finally {
            Read.finished(in, 1, jp);
        }
    }

    public int read(byte[] b, JoinPoint.StaticPart jp) throws IOException {
        Read.starting(in, jp);
        int numRead = 0;
        try {
            return numRead = in.read(b);
        } finally {
            Read.finished(in, numRead, jp);
        }
    }

    public int read(byte[] b, int off, int len, JoinPoint.StaticPart jp) throws IOException {
        Read.starting(in, jp);
        int numRead = 0;
        try {
            return numRead = in.read(b, off, len);
        } finally {
            Read.finished(in, numRead, jp);
        }
    }

    public long skip(long n, JoinPoint.StaticPart jp) throws IOException {
        Read.starting(in, jp);
        long numSkipped = 0;
        try {
            return numSkipped = in.skip(n);
        } finally {
            Read.finished(in, numSkipped, jp);
        }
    }
}