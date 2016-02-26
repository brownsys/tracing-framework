package edu.brown.cs.systems.retro.aggregation.reporters;

import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;

public class NullReporter implements Reporter {

    @Override
    public void register(ResourceAggregator aggregator) {
        // do nothing
    }

    @Override
    public void reportImmediately(ImmediateReport.Builder report) {
        // do nothing
    }

}
