package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Represents the benchmark dataset. Only one exists for now, for simplicity
 */
public class DataSet {

    private static final Random r = new Random();

    private final FileSystem fs;

    public final String name;
    public final Path folder;
    public final long filesize;
    public final long nfiles;
    public final int nreplicas;

    private List<Path> files = new ArrayList<Path>();

    private DataSet(FileSystem fs, String name, Path folder, long filesize, long nfiles, int nreplicas) {
        this.fs = fs;
        this.name = name;
        this.folder = folder;
        this.filesize = filesize;
        this.nfiles = nfiles;
        this.nreplicas = nreplicas;
    }

    private DataSet(FileSystem fs, String name, Config c) {
        this(fs, name, new Path(c.getString("dir")), c.getLong("filesize"), c.getLong("nfiles"), c.getInt("nreplicas"));
    }

    private void loadOrCreate() throws IOException {
        load();
        while (files.size() < nfiles) {
            this.files.add(createFile(fs, folder, filesize, nreplicas));
        }
    }

    private void load() throws IOException {
        this.files.addAll(loadFiles(fs, folder));
    }

    public Path randomFile() {
        return files.get(r.nextInt(files.size()));
    }

    private static Set<Path> loadFiles(FileSystem fs, Path folder) throws IOException {
        FileStatus[] existingFiles = null;
        Set<Path> files = new HashSet<Path>();
        try {
            existingFiles = fs.listStatus(folder);
            for (FileStatus f : existingFiles) {
                files.add(f.getPath());
            }
        } catch (FileNotFoundException e) {
            // folder doesn't exist
        }
        System.out.printf("Loaded %d existing files\n", files.size());
        return files;
    }

    private static Path createFile(FileSystem fs, Path folder, long filesize, int nreplicas) throws IOException {
        Path file = new Path(String.format("%s/%s", folder, RandomStringUtils.randomAlphanumeric(16)));
        FSUtils.createAndWriteFile(fs, file, filesize, filesize, nreplicas);
        System.out.printf("Created file %s\n", file);
        return file;
    }

    @Override
    public String toString() {
        long size = filesize;
        String[] suffixes = new String[] { "b", "Kb", "Mb", "Gb" };
        int i = 0;
        while (size > 1024 && (size % 1024) == 0 && i < (suffixes.length - 1)) {
            size /= 1024;
            i++;
        }
        return String.format("DataSet %s: %d files, %d%s, at %s", name, nfiles, size, suffixes[i], folder);
    }

    public static DataSet create(FileSystem fs, String dsname) throws IOException {
        return create(fs, dsname, ConfigFactory.load().getConfig("hdfs-benchmark.datasets." + dsname));
    }

    public static DataSet create(FileSystem fs, String dsname, Config config) throws IOException {
        DataSet ds = new DataSet(fs, dsname, config);
        ds.loadOrCreate();
        if (ds.files.size() != ds.nfiles)
            System.out.printf("Warning: dataset has %d files, expected %d\n", ds.files.size(), ds.nfiles);
        return ds;
    }

    public static DataSet load(FileSystem fs, String dsname) throws IOException {
        return load(fs, dsname, ConfigFactory.load().getConfig("hdfs-benchmark.datasets." + dsname));
    }

    public static DataSet load(FileSystem fs, String dsname, Config config) throws IOException {
        DataSet ds = new DataSet(fs, dsname, config);
        ds.load();
        if (ds.files.size() != ds.nfiles)
            System.out.printf("Warning: dataset has %d files, expected %d\n", ds.files.size(), ds.nfiles);
        return ds;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(create(FileSystem.get(new Configuration()), "default"));
    }

}
