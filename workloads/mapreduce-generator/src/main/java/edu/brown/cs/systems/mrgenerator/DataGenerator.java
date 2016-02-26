package edu.brown.cs.systems.mrgenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.Text;

/**
 * Generates mapreduce input file data
 * 
 * @author a-jomace
 *
 */
public class DataGenerator {

    private long blockSizeBytes = 1024 * 1024 * 8; // 64MB blocks by default
    private long fileSizeBytes = 1024 * 1024 * 64; // 1GB input file by default
    private int lineLength = 100; // 1000 characters per input line by default
    private int nReplicas = 3; // 3 replicas by default
    private String filename;

    public DataGenerator(String filename) {
        this.filename = filename;
    }

    public DataGenerator replicas(int nReplicas) {
        this.nReplicas = nReplicas;
        return this;
    }

    public DataGenerator blockSize(long blockSizeBytes) {
        this.blockSizeBytes = blockSizeBytes;
        return this;
    }

    public DataGenerator fileSize(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
        return this;
    }

    public DataGenerator fileName(String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * Generates the data
     * 
     * @throws IOException
     */
    public void generate(FileSystem fs) throws IOException {
        Path p = new Path(this.filename);

        boolean generate = false;
        try {
            FileStatus status = fs.getFileStatus(p);

            if (!status.isFile()) {
                System.out.printf("Input path %s already exists, but it is not a file.\n", p.toString());
                generate = true;
            } else {
                // Check the block size of the existing file
                long blockSize = status.getBlockSize();
                if (blockSize != blockSizeBytes) {
                    System.out.printf("Input file %s exists with blockSize=%d, but expected blockSize=%d.\n", p.toString(), blockSize, blockSizeBytes);
                    generate = true;
                }

                // Check the file size of the existing file
                long fileSize = status.getLen();
                if (fileSize > fileSizeBytes + 2000 || fileSize < fileSizeBytes - 2000) {
                    System.out.printf("Input file %s exists with fileSize=%d, but expected fileSize=%d\n", p.toString(), fileSize, fileSizeBytes);
                    generate = true;
                }
            }

            if (generate) {
                System.out.printf("Deleting existing path %s\n", p.toString());
                if (!fs.delete(p, true)) {
                    throw new IOException("Unable to delete existing path " + p);
                }
            }
        } catch (FileNotFoundException e) {
            // good, no file, continue
            System.out.printf("Input path %s does not exist\n", p.toString());
            generate = true;
        }

        if (generate) {
            System.out.printf("Generating input file file %s with blockSize=%d and fileSize=%d.\n", p.toString(), blockSizeBytes, fileSizeBytes);
            DataGenerator.createAndWriteFile(fs, p, fileSizeBytes, blockSizeBytes, nReplicas, lineLength);
        } else {
            System.out.printf("Input file %s already exists with blockSize=%d and fileSize=%d.\n", p.toString(), blockSizeBytes, fileSizeBytes);
        }
    }

    private static FSDataOutputStream createFile(FileSystem fs, Path file, long blockSize, int nReplicas) throws IOException {
        // Create the file
        return fs.create(file, FsPermission.getFileDefault().applyUMask(FsPermission.getUMask(fs.getConf())),
                EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE, CreateFlag.SYNC_BLOCK), 512, (short) nReplicas, blockSize, null);
    }

    /** Writes a text file with lines of the specified length */
    public static void createAndWriteFile(FileSystem fs, Path file, long fileSize, long blockSize, int nReplicas, int lineLength) throws IOException {
        FSDataOutputStream out = DataGenerator.createFile(fs, file, blockSize, nReplicas);
        try {
            long bytes_remaining = fileSize;

            while (bytes_remaining > 0) {
                String nextString = RandomStringUtils.randomAlphanumeric(lineLength) + '\n';
                Text textOut = new Text(nextString);
                byte[] outBytes = textOut.getBytes();
                bytes_remaining -= outBytes.length;
                out.write(outBytes);
            }
        } finally {
            out.close();
        }
    }

}
