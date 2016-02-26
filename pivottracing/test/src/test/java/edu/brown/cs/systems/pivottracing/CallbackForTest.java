package edu.brown.cs.systems.pivottracing;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pivottracing.ResultsProtos.ResultsTuple;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Group;
import junit.framework.TestCase;

public class CallbackForTest implements QueryResultsCallback {
    private BlockingQueue<QueryResults> results = Queues.newLinkedBlockingQueue();

    public void onResultsReceived(QueryResults results) {
        this.results.add(results);
    }
    public QueryResults awaitResults() throws InterruptedException {
        return awaitResults(false);
    }
    public QueryResults awaitResults(boolean print) throws InterruptedException {
        QueryResults ret = results.poll(2000, TimeUnit.MILLISECONDS);
        if (print) 
            System.out.println(ret);
        return ret;
    }
    public void expectNoResults() throws InterruptedException {
        QueryResults results = awaitResults();
        TestCase.assertNotNull(results);
        TestCase.assertEquals(0, results.getGroupCount());
        TestCase.assertEquals(0, results.getTupleCount());            
    }
    public void expectTuple(String... values) throws InterruptedException {
        expectTuples(ResultsTuple.newBuilder().addAllValue(Lists.newArrayList(values)).build());
    }
    public void expectTuples(ResultsTuple... tuples) throws InterruptedException {
        expectTuples(Lists.newArrayList(tuples));
    }
    public void expectTuples(List<ResultsTuple> tuples) throws InterruptedException {
        QueryResults results = awaitResults();
        TestCase.assertNotNull(results);
        TestCase.assertEquals(0, results.getGroupCount());
        TestCase.assertEquals(tuples, results.getTupleList());
    }
    public void expectGroups(List<Group> groups) throws InterruptedException {
        QueryResults results = awaitResults();
        TestCase.assertNotNull(results);
        TestCase.assertEquals(0, results.getTupleCount());
        TestCase.assertEquals(groups, results.getGroupList());            
    }
}