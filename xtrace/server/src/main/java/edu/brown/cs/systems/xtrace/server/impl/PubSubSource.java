package edu.brown.cs.systems.xtrace.server.impl;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReportv4;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;

public class PubSubSource extends edu.brown.cs.systems.pubsub.PubSubClient.Subscriber<XTraceReportv4> {
    private static final Logger LOG = Logger.getLogger(PubSubSource.class);

    private final PubSubClient pubsub;
    private final MetadataStore metadata;
    private final DataStore data;

    public PubSubSource(DataStore data, MetadataStore metadata) throws IOException {
        pubsub = PubSub.client();
        pubsub.subscribe(XTraceSettings.PUBSUB_TOPIC, this);
        this.data = data;
        this.metadata = metadata;
    }

    public void shutdown() {
        pubsub.close();
        LOG.info("PubSub subscriber closed");
    }

    @Override
    protected void OnMessage(XTraceReportv4 msg) {
        try {
            ReportImpl report = new ReportImpl(msg);
            data.reportReceived(report);
            metadata.reportReceived(report);
        } catch (Exception e) {
            LOG.warn("PubSub exception receiving report\n" + msg, e);
        }
    }
}
