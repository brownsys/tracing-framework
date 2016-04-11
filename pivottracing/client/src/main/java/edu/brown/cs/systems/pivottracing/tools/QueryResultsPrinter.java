package edu.brown.cs.systems.pivottracing.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PivotTracingConfig;
import edu.brown.cs.systems.pivottracing.ResultsProtos.QueryResults;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;

/** Just subscribes and prints all query results to stdout */
public class QueryResultsPrinter extends Subscriber<QueryResults> {
    
    public Set<ByteString> outputIds = Sets.newHashSet();
    
    /** If 1 or more values are specified for queriesToPrint, only prints results for those queries */
    public QueryResultsPrinter(String... queriesToPrint) {
        for (String s : queriesToPrint) {
            outputIds.add(ByteString.copyFromUtf8(s));
        }
    }

    public void print(QueryResults message) {
        System.out.println(message);
    }
    
    @Override
    protected void OnMessage(QueryResults message) {
        if (outputIds.isEmpty() || outputIds.contains(message.getEmit().getOutputId())) {
            print(message);
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        PubSub.subscribe(PivotTracingConfig.RESULTS_TOPIC, new QueryResultsPrinter(args));
        PubSub.join();
    }

}
