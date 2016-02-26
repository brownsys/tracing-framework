package edu.brown.cs.systems.retro.aggregation;

import edu.brown.cs.systems.pubsub.PubSubClient;
import edu.brown.cs.systems.retro.aggregation.Report.ResourceReport;

public abstract class Callback extends PubSubClient.Subscriber<ResourceReport> {
}
