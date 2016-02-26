package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.EOFException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Random;

import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

class FSUtils {

    public static final Random r = new Random();

    public static boolean fileExists(FileSystem fs, Path filePath) throws IOException {
        return fs.exists(filePath);
    }

    public static boolean deleteFile(FileSystem fs, Path filePath) throws IOException {
        return fs.delete(filePath, false);
    }

    public static boolean renameFile(FileSystem fs, Path srcFilePath, Path dstFilePath) throws IOException {
        return fs.rename(srcFilePath, dstFilePath);
    }

    public static void readFile(FileSystem fs, Path filePath) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        FSDataInputStream in = fs.open(filePath); // uses the
                                                  // dfs.stream-buffer-size
                                                  // property to set buffer size
        try {
            while (in.read(buf) != -1) {
            } // read until eof
        } finally {
            in.close();
        }
    }

    public static void readFully(FileSystem fs, Path filePath, byte[] buffer) throws IOException {
        FSDataInputStream in = fs.open(filePath); // uses the
                                                  // dfs.stream-buffer-size
                                                  // property to set buffer size
        try {
            in.readFully(buffer);
        } catch (EOFException e) {
            System.out.println("EOF exception reading fully: " + e.getMessage());
        } finally {
            in.close();
        }
    }

    public static void readPartial(FileSystem fs, Path filePath, int offset, int length, byte[] buffer) throws IOException {
        readAmount(fs, fs.open(filePath), offset, length, buffer);
    }

    public static void readAmount(FileSystem fs, FSDataInputStream in, int offset, int length, byte[] buffer) throws IOException {
        try {
            in.readFully(offset, buffer, 0, length);
        } catch (EOFException e) {
            System.out.println("EOF exception reading partial: " + e.getMessage());
        }
    }

    public static FSDataOutputStream createFile(FileSystem fs, Path file, long blockSize, int nReplicas) throws IOException {
        // Create the file
        return fs.create(file, FsPermission.getFileDefault().applyUMask(FsPermission.getUMask(fs.getConf())),
                EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE, CreateFlag.SYNC_BLOCK), 512, (short) nReplicas, blockSize, null);
    }

    public static FSDataInputStream openFile(FileSystem fs, Path file) throws IOException {
        return fs.open(file);
    }

    public static void createAndWriteFile(FileSystem fs, Path file, long fileSize, long blockSize, int nReplicas) throws IOException {
        // Create the file
        FSDataOutputStream out = createFile(fs, file, blockSize, nReplicas);

        try {
            // Write bytes
            byte[] buffer = new byte[(int) Math.min(fileSize, 65536)];
            r.nextBytes(buffer);

            long bytes_remaining = fileSize;

            while (bytes_remaining > 0) {
                int nbytes = (int) Math.min(buffer.length, bytes_remaining);
                bytes_remaining -= nbytes;
                out.write(buffer, 0, nbytes);
            }
        } finally {
            out.close();
        }
    }

}
