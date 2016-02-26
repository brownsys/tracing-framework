package edu.brown.cs.systems.retro.aggregation.reporters;

import edu.brown.cs.systems.retro.aggregation.Report.ImmediateReport;
import edu.brown.cs.systems.retro.aggregation.aggregators.ResourceAggregator;

public interface Reporter {

    public void register(ResourceAggregator aggregator);

    public void reportImmediately(ImmediateReport.Builder report);

}
