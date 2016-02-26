package edu.brown.cs.systems.pivottracing;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.PTAgentProtos.PivotTracingCommand;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.query.Parser;
import edu.brown.cs.systems.pivottracing.query.Parser.PTQueryParserException;
import edu.brown.cs.systems.pivottracing.query.QueryAdvice;
import edu.brown.cs.systems.pivottracing.tracepoint.Tracepoint;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;

/** This is the main entry point for clients to submit queries to a Pivot Tracing enabled system, and receive results. */
public class PivotTracingClient {
    
    private static final Logger log = LoggerFactory.getLogger(PivotTracingClient.class);
    
    public final Map<String, Tracepoint> tracepoints = Maps.newHashMap(); // All tracepoints known to this client
    public final Map<String, PTQuery> queries = Maps.newHashMap(); // All queries added to this client
    private final Map<PTQuery, QueryAdvice> installedQueries = Maps.newHashMap(); // advice of queries that have been installed
    private ResultsSubscriber resultsSubscriber = null; // Lazily instantiated pubsub subscriber
    
    /** Create a new PT client */
    public static PivotTracingClient newInstance() {
        return new PivotTracingClient();
    }

    /** Gets, maybe instantiating, the results subscriber */
    private synchronized ResultsSubscriber subscriber() {
        if (resultsSubscriber == null) {
            resultsSubscriber = new ResultsSubscriber();
        }
        return resultsSubscriber;
    }
    
    /**
     * Look up a tracepoint by name
     * @param name the name of the tracepoint
     * @return the Tracepoint, or null if it does not exist
     */
    public Tracepoint getTracepoint(String name) {
        return tracepoints.get(name);
    }
    
    /**
     * Add a tracepoint
     * @param t the tracepoint to add; queries may refer to this tracepoint by name
     */
    public void addTracepoint(Tracepoint t) {
        tracepoints.put(t.getName(), t);
    }
    
    /** 
     * Parses and adds a query 
     * @param queryName a name to give this query; other queries may refer to this name
     * @param queryString the query string
     * @return the parsed PT query
     * @throws PTQueryParserException if there was a problem parsing the query
     * @throws PTQueryException if the query parsed but had errors
     */
    public PTQuery parse(String queryName, String queryString) throws PTQueryParserException, PTQueryException {
        return new Parser(this).parse(queryName, queryString);
    }
    
    /**
     * Look up a query by name
     * @param queryName name of the query to look up
     * @return the PTQuery by this name, or null if it does not exist
     */
    public PTQuery getQuery(String queryName) {
        return queries.get(queryName);
    }
    
    /** Generate QueryAdvice for the PT query */
    public QueryAdvice generateAdvice(PTQuery query) {
        return QueryAdvice.generate(query);
    }
    
    /** Is the specified query currently installed? */
    public boolean isInstalled(PTQuery query) {
        return installedQueries.containsKey(query);
    }
    
    /** Get the installation ID of the specified query if it is installed */
    public ByteString queryId(PTQuery query) {
        if (installedQueries.containsKey(query)) {
            return installedQueries.get(query).getQueryId();
        }
        return null;
    }
    
    /** Install a query if it is not already installed */
    public void install(PTQuery query) {
        install(query, null);
    }
    
    /** Install a query if it hasn't already been installed.
     * If it's already been installed, subscribe to the already-installed version */
    public void install(PTQuery query, QueryResultsCallback resultsCallback) {
        // Install the query if it hasn't already been installed
        if (!installedQueries.containsKey(query)) {
            // Generate advice
            QueryAdvice advice = generateAdvice(query);
            
            // Create installation command
            PivotTracingCommand.Builder b = PivotTracingCommand.newBuilder();
            b.getUpdateBuilder().addAllWeave(advice.getWeaveSpecs());

            // Install
            PivotTracingCommand command = b.build();
            log.info("Publishing command to {}:\n{}", PivotTracingConfig.COMMANDS_TOPIC, command);
            PubSub.publish(PivotTracingConfig.COMMANDS_TOPIC, command);
            
            // Save query
            installedQueries.put(query, advice);
        }
        
        // Register callback
        if (resultsCallback != null) {
            subscriber().subscribe(queryId(query), resultsCallback);
        }
    }
    
    /** Uninstall a query and stop subscribing */
    public void uninstall(PTQuery query) {
        QueryAdvice advice = installedQueries.remove(query);
        if (advice != null) {
            // Create uninstallation command
            PivotTracingCommand.Builder b = PivotTracingCommand.newBuilder();
            b.getUpdateBuilder().addAllRemove(advice.getAdviceIds());

            // Uninstall
            PubSub.publish(PivotTracingConfig.COMMANDS_TOPIC, b.build());
            
            // Remove subscriber
            subscriber().unsubscribeAll(advice.getQueryId());
        }
    }
    
    /** Instruct all PT agents to uninstall all currently installed queries */
    public void uninstallAll() {
        // Create remove all command
        PivotTracingCommand.Builder b = PivotTracingCommand.newBuilder();
        b.getUpdateBuilder().setRemoveAll(true);
        
        // Remove all
        PubSub.publish(PivotTracingConfig.COMMANDS_TOPIC, b.build());
        
        // Tidy up subscribers
        installedQueries.clear();
        subscriber().unsubscribeAll();
    }
    
    /** Subscribe to the results of a query.  If the query is not installed, this does nothing */
    public void subscribe(PTQuery query, QueryResultsCallback resultsCallback) {
        QueryAdvice advice = installedQueries.get(query);
        if (advice != null) {
            subscriber().subscribe(advice.getQueryId(), resultsCallback);
        }
    }
    
    /** Unsubscribe from the query results of a query, but leave the query installed */
    public void unsubscribe(PTQuery query) {
        QueryAdvice advice = installedQueries.get(query);
        if (advice != null) {
            subscriber().unsubscribeAll(advice.getQueryId());
        }
    }
    
    private class ResultsSubscriber extends Subscriber<ResultsProtos.QueryResults> {
        
        private Multimap<ByteString, QueryResultsCallback> subscribers = HashMultimap.create();
        
        public ResultsSubscriber() {
            PubSub.subscribe(PivotTracingConfig.RESULTS_TOPIC, this);
        }
        
        public void subscribe(ByteString queryId, QueryResultsCallback callback) {
            subscribers.put(queryId, callback);
        }
        
        public void unsubscribeAll(ByteString queryId) {
            subscribers.removeAll(queryId);
        }
        
        public void unsubscribeAll() {
            subscribers.clear();
        }

        @Override
        protected void OnMessage(ResultsProtos.QueryResults resultsMessage) {
            log.debug("Query results\n{}", resultsMessage);
            for (QueryResultsCallback callback : subscribers.get(resultsMessage.getEmit().getOutputId())) {
                try {
                    callback.onResultsReceived(resultsMessage);
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }
        
    }
    

}
