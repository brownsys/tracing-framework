package edu.brown.cs.systems.xtrace.server.impl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;
import edu.brown.cs.systems.xtrace.server.api.Report;
import edu.brown.cs.systems.xtrace.server.api.TaskRecord;

public class DerbyMetadataStore implements MetadataStore {
    private static DerbyMetadataStore INSTANCE;

    /**
     * Derby only allows one instance per JVM (apparently). This constructs and
     * gets the instance of DerbyMetadataStore.
     */
    public static DerbyMetadataStore getInstance() throws Exception {
        if (INSTANCE == null)
            INSTANCE = new DerbyMetadataStore(XTraceSettings.DATASTORE_DIRECTORY + "/derby/");
        return INSTANCE;
    }

    private static final Logger LOG = Logger.getLogger(DerbyMetadataStore.class);

    private final DatabaseReader reader;
    private final DatabaseWriter writer;

    private volatile boolean alive = true;

    private DerbyMetadataStore(String dataDirName) throws Exception {
        File dataRootDir = new File(dataDirName);
        if (!dataRootDir.isDirectory() && !dataRootDir.mkdirs())
            throw new IOException("Data Store location isn't a directory " + dataDirName);
        if (!dataRootDir.canWrite())
            throw new IOException("Can't write to data store directory");

        System.setProperty("derby.system.home", dataDirName);
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();

        try {
            DriverManager.getConnection("jdbc:derby:tasks").close();
        } catch (SQLException e) {
            LOG.info("Derby database does not exist; creating...");
            createDatabase();
        }

        reader = new DatabaseReader();
        writer = new DatabaseWriter();
        writer.start();

        LOG.info("Successfully connected to the internal Derby database");
        LOG.info("Database directory: " + dataRootDir.getAbsolutePath());
    }

    private void createDatabase() throws SQLException {
        Connection create = DriverManager.getConnection("jdbc:derby:tasks;create=true");
        Statement s = create.createStatement();
        s.executeUpdate("create table tasks(" + "taskId varchar(40) not null primary key, " + "firstSeen timestamp default current_timestamp not null, "
                + "lastUpdated timestamp default current_timestamp not null, " + "numReports integer default 1 not null, " + "tags varchar(32672), "
                + "title varchar(128))");
        s.executeUpdate("create index idx_tasks on tasks(taskid)");
        s.executeUpdate("create index idx_firstseen on tasks(firstSeen)");
        s.executeUpdate("create index idx_lastUpdated on tasks(lastUpdated)");
        s.executeUpdate("create index idx_tags on tasks(tags)");
        s.executeUpdate("create index idx_title on tasks(title)");
        s.close();
        create.commit();
        create.close();
    }

    @Override
    public void shutdown() {
        // Set alive to false to stop doing anything new
        alive = false;

        // Interrupt the database updater and wait for him to finish
        try {
            writer.interrupt();
            writer.join();
        } catch (InterruptedException e) {
        }

        // Shut down the database
        try {
            DriverManager.getConnection("jdbc:derby:tasks;shutdown=true");
            LOG.info("Derby Metadata store successfully shut down");
        } catch (SQLException e) {
            if (!e.getSQLState().equals("08006")) {
                LOG.warn("Unable to shutdown embedded database", e);
            } else {
                LOG.info("Derby Metadata store successfully shut down");
            }
        }
    }

    @Override
    public void reportReceived(Report report) {
        if (alive && report != null) {
            writer.reportReceived(report.getTaskID());
            if (report.hasTitle())
                writer.setTitle(report.getTaskID(), report.getTitle());
            if (report.hasTags())
                writer.addTags(report.getTaskID(), report.getTags());
        }
    }

    @Override
    public List<TaskRecord> getTasksSince(long startTime, int offset, int limit) {
        if (alive)
            return reader.getTasksSince(startTime, offset, limit);
        return Collections.emptyList();
    }

    @Override
    public List<TaskRecord> getLatestTasks(int offset, int limit) {
        if (alive)
            return reader.getLatestTasks(offset, limit);
        return Collections.emptyList();
    }

    @Override
    public List<TaskRecord> getTasksByTag(String tag, int offset, int limit) {
        if (alive)
            return reader.getTasksByTag(tag, offset, limit);
        return Collections.emptyList();
    }

    @Override
    public List<TaskRecord> getTasksByTitle(String title, int offset, int limit) {
        if (alive)
            return reader.getTasksByTitle(title, offset, limit);
        return Collections.emptyList();
    }

    @Override
    public List<TaskRecord> getTasksByTitleSubstring(String title, int offset, int limit) {
        if (alive)
            return reader.getTasksByTitleSubstring(title, offset, limit);
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getConcurrentTasks(String taskId) {
        if (alive)
            return reader.getOverlappingTasks(taskId);
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getTags(String taskId) {
        if (alive)
            return reader.getTagsForTask(taskId);
        return Collections.emptyList();
    }

    @Override
    public int numTasks() {
        if (alive)
            return reader.numTasks();
        return 0;
    }

    @Override
    public int numReports() {
        if (alive)
            return reader.numReports();
        return 0;
    }

    private final class DatabaseReader {

        private Connection read;
        private PreparedStatement getByTag, tasksBetween, updatedSince;
        private PreparedStatement totalNumReports, totalNumTasks, timesByTask;
        private PreparedStatement lastTasks, getTags, getByTitle, getByTitleApprox;

        private DatabaseReader() throws Exception {
            read = DriverManager.getConnection("jdbc:derby:tasks");
            read.setAutoCommit(false);
            createPreparedStatements();
        }

        private void createPreparedStatements() throws SQLException {
            tasksBetween = read.prepareStatement("select taskid from tasks where firstseen <= ? and lastUpdated >= ?");
            totalNumReports = read.prepareStatement("select sum(numReports) as totalreports from tasks");
            totalNumTasks = read.prepareStatement("select count(distinct taskid) as numtasks from tasks");
            timesByTask = read.prepareStatement("select firstseen, lastUpdated from tasks where taskid = ?");
            updatedSince = read.prepareStatement("select * from tasks where firstseen >= ? order by lastUpdated desc");
            lastTasks = read.prepareStatement("select * from tasks order by lastUpdated desc");
            getByTag = read.prepareStatement("select * from tasks where upper(tags) like upper('%'||?||'%') order by lastUpdated desc");
            getTags = read.prepareStatement("select tags from tasks where taskid = ?");
            getByTitle = read.prepareStatement("select * from tasks where upper(title) = upper(?) order by lastUpdated desc");
            getByTitleApprox = read.prepareStatement("select * from tasks where upper(title) like upper('%'||?||'%') order by lastUpdated desc");
        }

        /**
         * Creates a TaskRecord object from a database result set
         */
        private TaskRecord readTaskRecord(ResultSet rs) throws SQLException {
            String taskId = rs.getString("taskId");
            long firstSeen = rs.getTimestamp("firstSeen").getTime();
            long lastUpdated = rs.getTimestamp("lastUpdated").getTime();
            String title = rs.getString("title");
            int numReports = rs.getInt("numReports");
            String tagstring = rs.getString("tags");
            if (tagstring == null)
                tagstring = "";
            List<String> tags = Arrays.asList(tagstring.split(","));
            return new TaskRecord(taskId, firstSeen, lastUpdated, numReports, title, tags);
        }

        /**
         * Creates a list of TaskRecord objects from a databse result set
         */
        private List<TaskRecord> createRecordList(ResultSet rs, int offset, int limit) throws SQLException {
            List<TaskRecord> lst = new ArrayList<TaskRecord>();
            int i = 0;
            while (rs.next()) {
                if (i >= offset && i < offset + limit)
                    lst.add(readTaskRecord(rs));
                i++;
            }
            return lst;
        }

        public synchronized List<TaskRecord> getTasksSince(long milliSecondsSince1970, int offset, int limit) {
            ArrayList<TaskRecord> lst = new ArrayList<TaskRecord>();

            try {
                if (offset + limit + 1 < 0) {
                    updatedSince.setMaxRows(Integer.MAX_VALUE);
                } else {
                    updatedSince.setMaxRows(offset + limit + 1);
                }
                updatedSince.setString(1, (new Timestamp(milliSecondsSince1970)).toString());
                ResultSet rs = updatedSince.executeQuery();
                try {
                    int i = 0;
                    while (rs.next()) {
                        if (i >= offset && i < offset + limit)
                            lst.add(readTaskRecord(rs));
                        i++;
                    }
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Internal SQL error", e);
            }

            return lst;
        }

        public synchronized Collection<String> getTagsForTask(String taskId) {
            try {
                getTags.setString(1, taskId);
                getTags.execute();
                ResultSet rs = getTags.getResultSet();
                try {
                    if (rs.next())
                        return Arrays.asList(rs.getString("tags").split(","));
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Unable to get tags for task " + taskId, e);
            }
            return Collections.emptyList();
        }

        public synchronized List<TaskRecord> getLatestTasks(int offset, int limit) {
            int numToFetch = offset + limit;
            List<TaskRecord> lst = new ArrayList<TaskRecord>();
            try {
                if (offset + limit + 1 < 0) {
                    lastTasks.setMaxRows(Integer.MAX_VALUE);
                } else {
                    lastTasks.setMaxRows(offset + limit + 1);
                }
                ResultSet rs = lastTasks.executeQuery();
                try {
                    int i = 0;
                    while (rs.next() && numToFetch > 0) {
                        if (i >= offset && i < offset + limit)
                            lst.add(readTaskRecord(rs));
                        numToFetch -= 1;
                        i++;
                    }
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Internal SQL error", e);
            }
            return lst;
        }

        public synchronized List<TaskRecord> getTasksByTag(String tag, int offset, int limit) {
            List<TaskRecord> lst = new ArrayList<TaskRecord>();
            try {
                if (offset + limit + 1 < 0) {
                    getByTag.setMaxRows(Integer.MAX_VALUE);
                } else {
                    getByTag.setMaxRows(offset + limit + 1);
                }
                getByTag.setString(1, tag);
                ResultSet rs = getByTag.executeQuery();
                try {
                    int i = 0;
                    while (rs.next()) {
                        TaskRecord rec = readTaskRecord(rs);
                        if (rec.getTags().contains(tag)) { // Make sure the SQL
                                                           // "LIKE"
                            // match is exact
                            if (i >= offset && i < offset + limit)
                                lst.add(rec);
                        }
                        i++;
                    }
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("SQLException in getTasksByTag", e);
            }
            return lst;
        }

        public synchronized Collection<String> getOverlappingTasks(String taskId) {
            HashSet<String> overlaps = new HashSet<String>();
            overlaps.add(taskId);

            try {
                // Fetch the timestamps for this task
                timesByTask.setString(1, taskId.toString());
                timesByTask.execute();
                ResultSet rs1 = timesByTask.getResultSet();
                try {
                    if (rs1.next()) {
                        // Update the search bounds if necessary
                        long firstSeen = rs1.getTimestamp("firstseen").getTime();
                        long lastUpdated = rs1.getTimestamp("lastUpdated").getTime();

                        // Now search for all taskids between these bounds
                        tasksBetween.setString(1, new Timestamp(lastUpdated).toString());
                        tasksBetween.setString(2, new Timestamp(firstSeen).toString());
                        tasksBetween.execute();
                        ResultSet rs2 = tasksBetween.getResultSet();
                        try {
                            while (rs2.next()) {
                                overlaps.add(rs2.getString("taskid"));
                            }
                        } finally {
                            rs2.close();
                        }
                    }
                } finally {
                    rs1.close();
                }
            } catch (SQLException e) {
                LOG.warn("SQLException in getOverlappingTasks " + taskId, e);
            }

            return overlaps;
        }

        public synchronized List<TaskRecord> getTasksByTitle(String title, int offset, int limit) {
            try {
                if (offset + limit + 1 < 0) {
                    getByTitle.setMaxRows(Integer.MAX_VALUE);
                } else {
                    getByTitle.setMaxRows(offset + limit + 1);
                }
                getByTitle.setString(1, title);
                ResultSet rs = getByTitle.executeQuery();
                try {
                    return createRecordList(rs, offset, limit);
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("SQLException in getTasksByTitle", e);
            }
            return Collections.emptyList();
        }

        public synchronized List<TaskRecord> getTasksByTitleSubstring(String title, int offset, int limit) {
            try {
                if (offset + limit + 1 < 0) {
                    getByTitleApprox.setMaxRows(Integer.MAX_VALUE);
                } else {
                    getByTitleApprox.setMaxRows(offset + limit + 1);
                }
                getByTitleApprox.setString(1, title);
                ResultSet rs = getByTitleApprox.executeQuery();
                try {
                    return createRecordList(rs, offset, limit);
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Internal SQL error", e);
            }
            return Collections.emptyList();
        }

        public synchronized int numReports() {
            try {
                ResultSet rs = totalNumReports.executeQuery();
                try {
                    rs.next();
                    return rs.getInt("totalreports");
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Internal SQL error", e);
            }
            return 0;
        }

        public synchronized int numTasks() {
            try {
                ResultSet rs = totalNumTasks.executeQuery();
                try {
                    rs.next();
                    return rs.getInt("numtasks");
                } finally {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.warn("Internal SQL error", e);
            }
            return 0;
        }
    }

    private final class DatabaseWriter extends Thread {

        private final class TaskUpdate {
            public String taskid;
            public String title = null;
            public Set<String> tags = null;
            public int newreportcount = 0;
        }

        private Connection write;
        private PreparedStatement insert, update, updateTitle, updateTags, countTasks, getTags;

        private Map<String, TaskUpdate> pendingUpdates = new HashMap<String, TaskUpdate>();

        public DatabaseWriter() throws Exception {
            write = DriverManager.getConnection("jdbc:derby:tasks");
            write.setAutoCommit(false);
            createPreparedStatements();
        }

        private void createPreparedStatements() throws SQLException {
            insert = write.prepareStatement("insert into tasks (taskid, tags, title, numReports) values (?, ?, ?, ?)");
            update = write.prepareStatement("update tasks set lastUpdated = current_timestamp, " + "numReports = numReports + ? where taskId = ?");
            updateTitle = write.prepareStatement("update tasks set title = ? where taskid = ?");
            updateTags = write.prepareStatement("update tasks set tags = ? where taskid = ?");
            countTasks = write.prepareStatement("select count(taskid) as rowcount from tasks where taskid = ?");
            getTags = write.prepareStatement("select tags from tasks where taskid = ?");
        }

        private TaskUpdate getTaskUpdate(String taskId) {
            TaskUpdate update = pendingUpdates.get(taskId);
            if (update == null) {
                update = new TaskUpdate();
                update.taskid = taskId;
                pendingUpdates.put(taskId, update);
            }
            return update;
        }

        public synchronized void reportReceived(String taskId) {
            TaskUpdate update = getTaskUpdate(taskId);
            update.newreportcount++;
        }

        public synchronized void setTitle(String taskId, String title) {
            TaskUpdate update = getTaskUpdate(taskId);
            update.title = title;
        }

        public synchronized void addTags(String taskId, List<String> tags) {
            TaskUpdate update = getTaskUpdate(taskId);
            if (update.tags == null)
                update.tags = new HashSet<String>();
            update.tags.addAll(tags);
        }

        @Override
        public void run() {
            long last_processing_time = System.currentTimeMillis();
            Set<String> seen = new HashSet<String>();
            Map<String, TaskUpdate> updates = new HashMap<String, TaskUpdate>();
            while (alive && !Thread.currentThread().isInterrupted()) {
                // Wait until the next allowed processing time
                try {
                    long tosleep;
                    while ((tosleep = (last_processing_time + XTraceSettings.DATABASE_UPDATE_INTERVAL - System.currentTimeMillis())) > 0) {
                        Thread.sleep(tosleep);
                    }
                } catch (InterruptedException e) {
                    // This means the thread is shutting down; we still want to
                    // finish
                    // updates
                }

                // Get the pending database updates
                synchronized (this) {
                    Map<String, TaskUpdate> pending = pendingUpdates;
                    pendingUpdates = updates;
                    updates = pending;
                }

                // Save the start time of processing, to later sleep
                last_processing_time = System.currentTimeMillis();

                if (updates.size() > 0) {
                    // Process all of the updates
                    for (TaskUpdate update : updates.values()) {
                        String taskId = update.taskid;
                        try {
                            if (!seen.contains(taskId) && !taskExists(taskId)) {
                                // fast track add all details at once
                                newTask(taskId, update);
                            } else {
                                if (update.title != null)
                                    updateTitle(taskId, update.title);
                                if (update.tags != null)
                                    addTagsToExistingTask(taskId, update.tags);
                                if (update.newreportcount > 0)
                                    updateExistingTaskReportCount(taskId, update.newreportcount);
                            }
                        } catch (Exception e) {
                            LOG.warn("Error processing database update for task " + taskId + ", dropping database update.  Report will still exist on disk", e);
                        }
                    }

                    // Commit the updates
                    try {
                        write.commit();
                    } catch (SQLException e) {
                        LOG.warn("Error committing database updates for database updater thread", e);
                    }

                    // Keep track of the task IDs we've most recently seen
                    if (seen.size() > 1000)
                        seen.clear();
                    seen.addAll(updates.keySet());

                    // Clear the updates
                    updates.clear();
                }
            }

            // Finally, clear up the database connection
            try {
                write.close();
            } catch (SQLException e) {
                LOG.warn("Database Updater thread unable to close connection to database", e);
            }
        }

        private boolean taskExists(String taskId) throws SQLException {
            try {
                countTasks.setString(1, taskId);
                ResultSet rs = countTasks.executeQuery();
                try {
                    rs.next();
                    return rs.getInt("rowcount") != 0;
                } finally {
                    rs.close();
                }
            } catch (Exception e) {
                return false;
            }
        }

        private void newTask(String taskId, TaskUpdate update) throws SQLException {
            String title = update.title == null ? taskId : update.title;
            insert.setString(1, taskId);
            insert.setString(2, joinWithCommas(update.tags));
            insert.setString(3, title);
            insert.setInt(4, update.newreportcount);
            insert.executeUpdate();
        }

        private void updateTitle(String taskId, String title) throws SQLException {
            updateTitle.setString(1, title);
            updateTitle.setString(2, taskId);
            updateTitle.executeUpdate();
        }

        private void updateExistingTaskReportCount(String taskId, Integer reportCount) throws SQLException {
            update.setInt(1, reportCount);
            update.setString(2, taskId);
            update.executeUpdate();
        }

        private void addTagsToExistingTask(String taskId, Set<String> tags) throws SQLException {
            getTags.setString(1, taskId);
            ResultSet tagsRs = getTags.executeQuery();
            String oldTags = "";
            try {
                tagsRs.next();
                oldTags = tagsRs.getString("tags");
            } finally {
                tagsRs.close();
            }
            tags.addAll(Arrays.asList(oldTags.split(",")));
            updateTags.setString(1, joinWithCommas(tags));
            updateTags.setString(2, taskId);
            updateTags.executeUpdate();
        }
    }

    private String joinWithCommas(Collection<String> strings) {
        if (strings == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = strings.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext())
                sb.append(",");
        }
        return sb.toString();
    }

}
