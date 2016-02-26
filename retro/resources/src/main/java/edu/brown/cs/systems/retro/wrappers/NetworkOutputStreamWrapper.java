package edu.brown.cs.systems.retro.wrappers;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.Network;

public class NetworkOutputStreamWrapper extends FilterOutputStream {

    private final Network Write;
    private final StaticPart creator;

    public NetworkOutputStreamWrapper(OutputStream os, JoinPoint.StaticPart instantiationJoinPoint) {
        this(os, instantiationJoinPoint, false);
    }

    public NetworkOutputStreamWrapper(OutputStream os, JoinPoint.StaticPart instantiationJoinPoint, boolean isLoopback) {
        super(os);
        this.creator = instantiationJoinPoint;
        if (isLoopback)
            Write = Network.LoopbackWrite;
        else
            Write = Network.Write;
    }

    /**
     * @return the best choice of pointcut to log this method call against.
     */
    private JoinPoint.StaticPart pointcut() {
        return JoinPointTracking.Caller.get(creator);
    }

    @Override
    public void close() throws IOException {
        this.close(pointcut());
    }

    @Override
    public void flush() throws IOException {
        this.flush(pointcut());
    }

    @Override
    public void write(int b) throws IOException {
        this.write(b, pointcut());
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, pointcut());
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.write(b, off, len, pointcut());
    }

    public void close(JoinPoint.StaticPart jp) throws IOException {
        Network.Close.starting(out, jp);
        try {
            out.close();
        } finally {
            Network.Close.finished(out, jp);
        }
    }

    public void flush(JoinPoint.StaticPart jp) throws IOException {
        Network.Flush.starting(out, jp);
        try {
            out.flush();
        } finally {
            Network.Flush.finished(out, jp);
        }
    }

    public void write(int b, JoinPoint.StaticPart jp) throws IOException {
        Write.starting(out, jp);
        try {
            out.write(b);
        } finally {
            Write.finished(out, 1, jp);
        }
    }

    public void write(byte[] b, JoinPoint.StaticPart jp) throws IOException {
        Write.starting(out, jp);
        try {
            out.write(b);
        } finally {
            Write.finished(out, b.length, jp);
        }
    }

    public void write(byte[] b, int off, int len, JoinPoint.StaticPart jp) throws IOException {
        Write.starting(out, jp);
        try {
            out.write(b, off, len);
        } finally {
            Write.finished(out, len, jp);
        }
    }
}