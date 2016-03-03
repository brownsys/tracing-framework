package edu.brown.cs.systems.pivottracing.agent.advice.utils;

import edu.brown.cs.systems.pivottracing.agent.advice.output.EmitAPIImpl;

public class EmitAPIImplForTest extends EmitAPIImpl {

    public EmitAPIImplForTest(int reportInterval, String resultsTopic) {
        super(reportInterval, resultsTopic, true);
        // TODO Auto-generated constructor stub
    }
//    
//    public final ResultsReporterForTest results;
////    
//    public EmitAPIImplForTest() {
//        super(1000, new ResultsReporterForTest());
//        this.results = (ResultsReporterForTest) reporter;
//    }
//    
//    public static class ResultsReporterForTest implements ResultsReporter {
//        public List<QueryResults> reports = Lists.newArrayList();
//        public void send(QueryResults results) {
//            this.reports.add(results);
//        }
//    }

}
