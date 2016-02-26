package edu.brown.cs.systems.xtrace.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.servlet.CGI;

import edu.brown.cs.systems.utils.TempFileExtractor;
import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;
import edu.brown.cs.systems.xtrace.server.api.Report;
import edu.brown.cs.systems.xtrace.server.api.TaskRecord;

public class WebServer extends Server {

    private static final Logger LOG = Logger.getLogger(WebServer.class);

    private static final DateFormat JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat HTML_DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy, HH:mm:ss");
    private static final int PAGE_LENGTH = 25;

    private DataStore data;
    private MetadataStore metadata;

    private String webui = "";

    private void extractWebUI() throws IOException, URISyntaxException {
        webui = TempFileExtractor.extractFolderToTemp("webui", "webui");
    }

    public WebServer(int httpport, DataStore data, MetadataStore metadata) throws IOException, URISyntaxException {
        super(httpport);

        extractWebUI();

        this.data = data;
        this.metadata = metadata;

        // Initialize Velocity template engine
        try {
            Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
            Velocity.setProperty("runtime.log.logsystem.log4j.logger", "edu.berkeley.xtrace.server.XTraceServer");
            Velocity.setProperty("file.resource.loader.path", webui + "/templates");
            Velocity.setProperty("file.resource.loader.cache", "true");
            Velocity.init();
        } catch (Exception e) {
            LOG.warn("Failed to initialize Velocity", e);
        }

        // Create Jetty server
        Context context = new Context(this, "/");

        // Create a CGI servlet for scripts in webui/cgi-bin
        ServletHolder cgiHolder = new ServletHolder(new CGI());
        cgiHolder.setInitParameter("cgibinResourceBase", webui + "/cgi-bin");

        // Pass any special PATH setting on to the execution environment
        if (System.getenv("PATH") != null)
            cgiHolder.setInitParameter("Path", System.getenv("PATH"));

        context.addServlet(cgiHolder, "*.cgi");
        context.addServlet(cgiHolder, "*.pl");
        context.addServlet(cgiHolder, "*.py");
        context.addServlet(cgiHolder, "*.rb");
        context.addServlet(cgiHolder, "*.tcl");
        context.addServlet(new ServletHolder(new GetReportsServlet()), "/reports/*");
        context.addServlet(new ServletHolder(new TagServlet()), "/tag/*");
        context.addServlet(new ServletHolder(new TitleServlet()), "/title/*");
        context.addServlet(new ServletHolder(new TitleLikeServlet()), "/titleLike/*");

        // JSON APIs for interactive visualization
        context.addServlet(new ServletHolder(new GetJSONReportsServlet()), "/interactive/reports/*");
        context.addServlet(new ServletHolder(new GetOverlappingTasksServlet()), "/interactive/overlapping/*");
        context.addServlet(new ServletHolder(new GetTagsForTaskServlet()), "/interactive/tags/*");
        context.addServlet(new ServletHolder(new GetTasksForTags()), "/interactive/taggedwith/*");

        context.setResourceBase(webui + "/html");
        context.addServlet(new ServletHolder(new IndexServlet()), "/");
    }

    private class GetReportsServlet extends HttpServlet {
        private static final long serialVersionUID = -5448528803007855403L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            String uri = request.getRequestURI();
            int pathLen = request.getServletPath().length() + 1;
            String taskId = uri.length() > pathLen ? uri.substring(pathLen) : null;
            Writer out = response.getWriter();
            if (taskId != null) {
                Iterator<Report> iter = data.getReports(taskId);
                while (iter.hasNext()) {
                    out.write(iter.next().toString());
                    out.write("\n\n");
                }
            }
        }
    }

    private class GetJSONReportsServlet extends HttpServlet {
        private static final long serialVersionUID = -3918120497812383181L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/json");
            response.setStatus(HttpServletResponse.SC_OK);
            String uri = request.getRequestURI();
            int pathLen = request.getServletPath().length() + 1;
            String taskIdString = uri.length() > pathLen ? uri.substring(pathLen) : null;
            String[] taskIds = taskIdString.split(",");

            Writer out = response.getWriter();
            out.write("[");
            boolean firstTaskDone = false;
            int count = 0;
            for (String taskId : taskIds) {
                Log.info("Writing task " + count++ + ": " + taskId);

                if (firstTaskDone)
                    out.write("\n,");
                firstTaskDone = true;

                out.append("{\"id\":\"");
                out.append(taskId);
                out.append("\",\"reports\":[");

                boolean firstReportDone = false;
                Iterator<Report> iter = data.getReports(taskId);
                while (iter.hasNext()) {
                    if (firstReportDone)
                        out.append(",\n");
                    out.append(iter.next().jsonRepr().toJSONString());
                    firstReportDone = true;
                }

                out.append("]}");
                Log.info("... done");
            }
            out.write("]");
        }
    }

    private class GetOverlappingTasksServlet extends HttpServlet {
        private static final long serialVersionUID = -6431290990122203372L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/json");
            response.setStatus(HttpServletResponse.SC_OK);
            String uri = request.getRequestURI();
            int pathLen = request.getServletPath().length() + 1;
            String taskIdString = uri.length() > pathLen ? uri.substring(pathLen) : null;
            String[] taskIds = taskIdString.split(",");

            HashSet<String> overlapping = new HashSet<String>();
            for (String taskId : taskIds) {
                overlapping.addAll(metadata.getConcurrentTasks(taskId));
            }

            Writer out = response.getWriter();
            out.write("[");
            boolean first = true;
            for (String taskId : overlapping) {
                if (!first)
                    out.write(',');
                out.write("\"");
                out.write(taskId);
                out.write("\"");
                first = false;
            }
            out.write("]");
        }
    }

    private class GetTagsForTaskServlet extends HttpServlet {
        private static final long serialVersionUID = 4539637720407471403L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/json");
            response.setStatus(HttpServletResponse.SC_OK);
            String uri = request.getRequestURI();
            int pathLen = request.getServletPath().length() + 1;
            String taskIdString = uri.length() > pathLen ? uri.substring(pathLen) : "";
            String[] taskIds = taskIdString.split(",");

            JSONObject obj = new JSONObject();
            for (String taskId : taskIds) {
                Collection<String> tags = metadata.getTags(taskId);
                JSONArray arr = new JSONArray();
                arr.addAll(tags);
                obj.put(taskId, arr);
            }

            Writer out = response.getWriter();
            out.write(obj.toJSONString());
        }
    }

    private class GetTasksForTags extends HttpServlet {
        private static final long serialVersionUID = 7557145515934865966L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/json");
            response.setStatus(HttpServletResponse.SC_OK);
            String uri = request.getRequestURI();
            int pathLen = request.getServletPath().length() + 1;
            String tagsString = uri.length() > pathLen ? uri.substring(pathLen) : "";
            String[] tags = tagsString.split(",");

            JSONObject obj = new JSONObject();
            for (String tag : tags) {
                JSONArray arr = new JSONArray();
                Collection<TaskRecord> taskInfos = metadata.getTasksByTag(tag, 0, Integer.MAX_VALUE);
                for (TaskRecord t : taskInfos) {
                    arr.add(t.getTaskId().toString());
                }
                obj.put(tag, arr);
            }

            Writer out = response.getWriter();
            out.write(obj.toJSONString());
        }
    }

    private class TagServlet extends HttpServlet {
        private static final long serialVersionUID = 4626142579591044224L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String tag = getUriPastServletName(request);
            if (tag == null || tag.equalsIgnoreCase("")) {
                response.sendError(505, "No tag given");
            } else {
                Collection<TaskRecord> taskInfos = metadata.getTasksByTag(tag, getOffset(request), getLength(request));
                showTasks(request, response, taskInfos, "Tasks with tag: " + tag, false);
            }
        }
    }

    private class TitleServlet extends HttpServlet {
        private static final long serialVersionUID = 4219687534314262068L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String title = getUriPastServletName(request);
            if (title == null || title.equalsIgnoreCase("")) {
                response.sendError(505, "No title given");
            } else {
                Collection<TaskRecord> taskInfos = metadata.getTasksByTitle(title, getOffset(request), getLength(request));
                showTasks(request, response, taskInfos, "Tasks with title: " + title, false);
            }
        }
    }

    private class TitleLikeServlet extends HttpServlet {
        private static final long serialVersionUID = -6165372457313341911L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String title = getUriPastServletName(request);
            if (title == null || title.equalsIgnoreCase("")) {
                response.sendError(505, "No title given");
            } else {
                Collection<TaskRecord> taskInfos = metadata.getTasksByTitleSubstring(title, getOffset(request), getLength(request));
                showTasks(request, response, taskInfos, "Tasks with title like: " + title, false);
            }
        }
    }

    private class IndexServlet extends DefaultServlet {
        private static final long serialVersionUID = -3214611259159362653L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            if (request.getRequestURI().equals("/")) {
                Collection<TaskRecord> tasks = metadata.getLatestTasks(getOffset(request), getLength(request));
                showTasks(request, response, tasks, "X-Trace Latest Tasks", true);
            } else {
                super.doGet(request, response);
            }
        }
    }

    private static String getUriPastServletName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int pathLen = request.getServletPath().length() + 1;
        String text = uri.length() > pathLen ? uri.substring(pathLen) : null;
        if (text != null) {
            try {
                text = URLDecoder.decode(text, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }
        return text;
    }

    private void showTasks(HttpServletRequest request, HttpServletResponse response, Collection<TaskRecord> tasks, String title, boolean showDbStats)
            throws IOException {
        if ("json".equals(request.getParameter("format"))) {
            response.setContentType("text/plain");
        } else {
            response.setContentType("text/html");
        }
        int offset = getOffset(request);
        int length = getLength(request);
        // Create Velocity context
        VelocityContext context = new VelocityContext();
        context.put("tasks", tasks);
        context.put("title", title);
        context.put("reportStore", metadata);
        context.put("request", request);
        context.put("offset", offset);
        context.put("length", length);
        context.put("lastResultNum", offset + length - 1);
        context.put("prevOffset", Math.max(0, offset - length));
        context.put("nextOffset", offset + length);
        context.put("showStats", showDbStats);
        context.put("JSON_DATE_FORMAT", JSON_DATE_FORMAT);
        context.put("HTML_DATE_FORMAT", HTML_DATE_FORMAT);
        context.put("PAGE_LENGTH", PAGE_LENGTH);
        // Return Velocity results
        try {
            Velocity.mergeTemplate("tasks.vm", "UTF-8", context, response.getWriter());
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            LOG.warn("Failed to display tasks.vm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to display tasks.vm");
        }
    }

    /**
     * Get the length GET parameter from a HTTP request, or return the default
     * (of PAGE_LENGTH) when it is not specified or invalid.
     * 
     * @param request
     * @return
     */
    private static int getLength(HttpServletRequest request) {
        int length = getIntParam(request, "length", PAGE_LENGTH);
        return Math.max(length, 0); // Don't allow negative
    }

    /**
     * Get the offset HTTP parameter from a request, or return the default (of
     * 0) when it is not specified.
     * 
     * @param request
     * @return
     */
    private static int getOffset(HttpServletRequest request) {
        int offset = getIntParam(request, "offset", 0);
        return Math.max(offset, 0); // Don't allow negative
    }

    /**
     * Read an integer parameter from a HTTP request, or return a default value
     * if the parameter is not specified.
     * 
     * @param request
     * @param name
     * @param defaultValue
     * @return
     */
    private static final int getIntParam(HttpServletRequest request, String name, int defaultValue) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

}
