package edu.brown.cs.systems.xtrace.reporting;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigFactory;

import edu.brown.cs.systems.pubsub.PubSub;

/**
 * A thread that sends X-Trace reports to the X-Trace server via PubSub
 */
public class PubSubReporter extends Thread implements XTraceReporter {

    protected static final Logger log = LoggerFactory.getLogger(PubSubReporter.class);

    protected final BlockingQueue<XTraceReport> outgoing = new LinkedBlockingQueue<XTraceReport>();

    /**
     * Creates a new PubSub reporter that publishes reports to the X-Trace
     * server via the default PubSub server
     */
    public PubSubReporter() {
    }

    /**
     * Enqueues the provided report to be sent by the reporting thread
     * 
     * @param report
     *            The report to send
     */
    public void send(XTraceReport report) {
        outgoing.offer(report);
    }

    @Override
    public void run() {
        // Get the configured topic to publish reports to
        String topic = ConfigFactory.load().getString("xtrace.pubsub.topic");
        log.info("Publishing X-Trace reports on topic {}", topic);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Take the next xtrace report
                XTraceReport report = outgoing.take();

                // Build and send
                PubSub.publish(topic, report.builder.build());
            }
        } catch (InterruptedException e) {
            // Do nothing and return
            log.warn("Publisher thread interrupted {}", e);
        }

        log.info("PubSubReporter complete");
    }

}
