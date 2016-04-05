package edu.brown.cs.systems.xtrace.logging;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.xtrace.XTraceBaggageInterface;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;
import edu.brown.cs.systems.xtrace.reporting.XTraceReporter;

public class XTraceLoggerImpl implements XTraceLogger {

    private final String agent;
    private final XTraceReporter reporter;

    /**
     * Create an X-Trace logger for the provided agent, that will log reports to
     * the provided X-Trace reporter
     * 
     * @param agent
     *            The name of this logger
     * @param reporter
     *            The reporter to send reports to
     */
    public XTraceLoggerImpl(String agent, XTraceReporter reporter) {
        this.agent = agent;
        this.reporter = reporter;
    }

    /**
     * XTrace logger is only valid if the current execution has an X-Trace Task
     * ID
     * 
     * @return true if the current execution is valid and can log, false
     *         otherwise
     */
    public boolean valid() {
        return XTraceSettings.discoveryMode() || XTraceBaggageInterface.hasTaskID();
    }
    
    public boolean valid(XTraceLoggingLevel level) {
        return XTraceSettings.discoveryMode() || (level.valid() && XTraceBaggageInterface.hasTaskID());
    }

    public void log(String message, Object... labels) {
        log(null, message, labels);
    }

    public void log(JoinPoint.StaticPart joinPoint, String message, Object... labels) {
        if (!valid()) {
            return;
        }
        XTraceReport report = XTraceReport.create();
        report.builder.setAgent(agent);
        report.addStandardFields();
        report.makeXTraceEvent(joinPoint);
        report.setMessage(message, labels);
        report.setJoinPoint(joinPoint);
        report.applyDecorators();
        reporter.send(report);
    }

    public void tag(String message, String... tags) {
        tag(null, message, tags);
    }

    public void tag(JoinPoint.StaticPart joinPoint, String message, String... tags) {
        if (!valid()) {
            return;
        }
        XTraceReport report = XTraceReport.create();
        report.addStandardFields();
        report.builder.setAgent(agent);
        report.builder.setLabel(message);
        report.makeXTraceEvent(joinPoint);
        report.setJoinPoint(joinPoint);
        for (String tag : tags) {
            if (tag != null) {
                report.builder.addTags(tag);
            }
        }
        report.applyDecorators();
        reporter.send(report);
    }

}
