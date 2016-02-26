package edu.brown.cs.systems.retro.wrappers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;

import edu.brown.cs.systems.retro.logging.JoinPointTracking;
import edu.brown.cs.systems.retro.resources.DiskResource;

public class RandomAccessFileWrapper extends RandomAccessFile {

    private final StaticPart creator;

    public RandomAccessFileWrapper(File file, String mode, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(file, mode);
        this.creator = jp;
    }

    public RandomAccessFileWrapper(String name, String mode, JoinPoint.StaticPart jp) throws FileNotFoundException {
        super(name, mode);
        this.creator = jp;
    }

    /**
     * @return the best choice of pointcut to log this method call against.
     */
    private JoinPoint.StaticPart pointcut() {
        return JoinPointTracking.Caller.get(creator);
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

    public void write(int b, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, 1, jp, true);
        try {
            super.write(b);
        } finally {
            DiskResource.Write.finished(this, 1, jp, true);
        }
    }

    public void write(byte[] b, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, b.length, jp, true);
        try {
            super.write(b);
        } finally {
            DiskResource.Write.finished(this, b.length, jp, true);
        }
    }

    public void write(byte[] b, int off, int len, JoinPoint.StaticPart jp) throws IOException {
        DiskResource.Write.starting(this, len, jp, true);
        try {
            super.write(b, off, len);
        } finally {
            DiskResource.Write.finished(this, len, jp, true);
        }
    }

}
