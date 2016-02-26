package edu.brown.cs.systems.retro.aggregation;

import edu.brown.cs.systems.pubsub.PubSubClient;
import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;

public abstract class ImmediateReportCallback extends PubSubClient.Subscriber<ImmediateReport> {
}
