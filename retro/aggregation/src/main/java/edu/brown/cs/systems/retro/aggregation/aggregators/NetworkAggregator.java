package edu.brown.cs.systems.retro.aggregation.aggregators;

import edu.brown.cs.systems.retro.aggregation.Resource;
import edu.brown.cs.systems.retro.aggregation.ResourceReportingSettings;

public class NetworkAggregator extends ResourceAggregator {

    private final boolean on;

    private NetworkAggregator(String linktype) {
        super(Resource.Type.NETWORK, linktype);
        on = ResourceReportingSettings.AGGREGATION_ENABLED && ResourceReportingSettings.NETWORK_AGGREGATION_ENABLED;
    }

    public static NetworkAggregator createUplinkAggregator() {
        return new NetworkAggregator("uplink");
    }

    public static NetworkAggregator createDownlinkAggregator() {
        return new NetworkAggregator("downlink");
    }

    public static NetworkAggregator createLoopbackUplinkAggregator() {
        return new NetworkAggregator("loopback-uplink");
    }

    public static NetworkAggregator createLoopbackDownlinkAggregator() {
        return new NetworkAggregator("loopback-downlink");
    }

    @Override
    public boolean enabled() {
        return on;
    }

}
