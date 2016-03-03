## Pivot Tracing Client: Installing Queries

Once you have written a query, it can be installed by invoking `PivotTracingClient.install(PTQuery query)`:

    PivotTracingClient pt = ...;
    PTQuery q = ...;
    ...
	pt.install(q);

Installing a query will perform several things:

1. The query will be compiled into *advice*, an intermediate representation of the instrumentation needed to evaluate the query.
2. The client will connect to the PubSub server and publish the advice
3. The Pivot Tracing agents will receive the advice and attempt to dynamically install the query
4. The Pivot Tracing agents will publish their status (along with any errors or exceptions) back via PubSub.

The `print-agent-status` executable prints the status reports that agents send.  Alternatively, you can explicitly receive these messages with the following:

    PubSub.subscribe(PivotTracingConfig.STATUS_TOPIC, new Subscriber<AgentStatus>() {
        protected void OnMessage(AgentStatus message) {
            // Print, process, etc....
        }
    });

You can also explicitly handle all query results sent by all queries with the following:

    PubSub.subscribe(PivotTracingConfig.RESULTS_TOPIC, new Subscriber<QueryResults>() {
        protected void OnMessage(QueryResults message) {
            // handle query results...
        }
    });

Alternatively, when you install a query, you can provide a callback for its results:

    QueryResultsCallback resultsCallback = new QueryResultsCallback() {
        public void onResultsReceived(QueryResults results) {
            // Handle results for query...
        }
    };
    ...
    pt.install(q, resultsCallback);

The `uninstall(PTQuery query)` and `uninstallAll()` methods will deregister any results callbacks and instruct the Pivot Tracing agents to remove instrumentation.