package edu.brown.cs.systems.xtrace.server.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import edu.brown.cs.systems.xtrace.Reporting.XTraceReportv4;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.Report;

public class FileTreeDataStore implements DataStore {

    static private final Logger LOG = Logger.getLogger(FileTreeDataStore.class);

    private final File data;

    public FileTreeDataStore(String directory) throws IOException {
        if (directory == null)
            throw new IOException("FileTreeDataStore directory name is null");

        data = new File(directory);
        if (!data.isDirectory() && !data.mkdirs())
            throw new IOException("FileTreeDataStore specified data store is not a directory " + directory);
        if (!data.canWrite())
            throw new IOException("FileTreeDataStore cannot write to specified datastore " + directory);

        LOG.info("DataStore directory: " + data.getAbsolutePath());
    }

    private class FileCloser implements RemovalListener<String, OutputStream> {
        public void onRemoval(RemovalNotification<String, OutputStream> notification) {
            try {
                notification.getValue().close();
            } catch (IOException e) {
                LOG.warn("IOException closing writer for task " + notification.getKey(), e);
            }
        }
    }

    private class FileOpener extends CacheLoader<String, OutputStream> {
        public OutputStream load(String taskId) throws IOException {
            if (taskId.length() < 6)
                throw new IllegalArgumentException("Invalid Task ID: " + taskId);

            // For some reason, it was decided that a three-level directory is
            // the best choice
            File l1 = new File(data, taskId.substring(0, 2));
            File l2 = new File(l1, taskId.substring(2, 4));
            File l3 = new File(l2, taskId.substring(4, 6));

            // Make sure the directory to write the task exists
            if (!l3.exists() && !l3.mkdirs()) {
                LOG.warn("Error creating directory " + l3.toString());
                throw new IOException("Error creating directory " + l3.toString());
            }

            // Create the task file and a writer to it
            File taskfile = new File(l3, taskId + ".txt");
            return new BufferedOutputStream(new FileOutputStream(taskfile, true), XTraceSettings.DATASTORE_BUFFER_SIZE);
        }
    }

    private final LoadingCache<String, OutputStream> writercache = CacheBuilder.newBuilder().maximumSize(XTraceSettings.DATASTORE_CACHE_SIZE)
            .expireAfterWrite(XTraceSettings.DATASTORE_CACHE_TIMEOUT, TimeUnit.MILLISECONDS).removalListener(new FileCloser()).build(new FileOpener());

    @Override
    public void reportReceived(Report r) {
        try {
            OutputStream writer = writercache.get(r.getTaskID());
            synchronized (writer) {
                r.writeDelimitedTo(writer);
            }
            writer.flush(); // for now flush immediately... but might not be the
                            // best choice
            writercache.cleanUp();
        } catch (ExecutionException e) {
            LOG.warn("Discarding a report due to file cache error", e);
        } catch (IOException e) {
            LOG.warn("Discarding a report due to IOException", e);
        }
    }

    @Override
    public Iterator<Report> getReports(String taskId) {
        File l1 = new File(data, taskId.substring(0, 2));
        File l2 = new File(l1, taskId.substring(2, 4));
        File l3 = new File(l2, taskId.substring(4, 6));
        File taskfile = new File(l3, taskId + ".txt");
        return new ReportIterator(taskfile);
    }

    /**
     * Flushes all open writers to disk.
     */
    public void flush() {
        for (OutputStream out : writercache.asMap().values()) {
            try {
                out.flush();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void shutdown() {
        writercache.invalidateAll();
        writercache.cleanUp();
        LOG.info("Data store flushed all data to disk");
    }

    private class ReportIterator implements Iterator<Report> {

        private InputStream in = null;
        private Report nextReport = null;

        public ReportIterator(File taskfile) {
            if (taskfile.exists() && taskfile.canRead()) {
                try {
                    in = new BufferedInputStream(new FileInputStream(taskfile), 4096);
                    nextReport = calcNext();
                } catch (FileNotFoundException e) {
                }
            }
        }

        public boolean hasNext() {
            return nextReport != null;
        }

        public Report next() {
            Report ret = nextReport;
            nextReport = calcNext();
            return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Report calcNext() {
            try {
                XTraceReportv4 report = XTraceReportv4.parseDelimitedFrom(in);
                try {
                    return new ReportImpl(report);
                } catch (NullPointerException e) {
                    System.out.println(report);
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
    }
}
