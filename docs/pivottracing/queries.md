## Pivot Tracing Client: Writing Queries

Pivot Tracing queries can refer to one or more tracepoints.  They can be written and parsed from a text representation or constructed using a declarative API.  Consider the example query:

    From incr In DataNodeMetrics.incrBytesRead
    GroupBy incr.host
    Select incr.host, SUM(incr.delta)

The equivalent query using the declarative API is:

	PTQuery q = PTQuery.From("incr", hdfs_datanode_incrBytesRead).GroupBy("incr.host").Sum("incr.delta");

or even more concisely:

	PTQuery q = PTQuery.From(hdfs_datanode_incrBytesRead).GroupBy("host").Sum("delta");

The `PivotTracingClient` class provides the client API to Pivot Tracing.  Acquire an instance with:

	PivotTracingClient pt = PivotTracingClient.newInstance();

To parse a query from text, the tracepoints must be first registered, then the query can be parsed:

	String queryText = "From incr In DataNodeMetrics.incrBytesRead \n ...";

	pt.addTracepoint(hdfs_datanode_incrBytesRead);
	PTQuery q = pt.parse("q1", queryText);

Calling `q.Optimize()` will create an optimized query.  Optimization affects the number of tuples sent by Pivot Tracing in the baggage for evaluating happened-before join.

The following declarative API functions are available:

	_______________________________________________________________________________________________________________________
	..From(..)                |    
	..Where(..)               |  
	..Let(..)                 | 
	..HappenedBeforeJoin(..)  | 
	                          | .Select("a", "b", "c")                              // SELECT a, b, c
	                          | .GroupBy("c", "d", "e")                             // GROUPBY c, d, e
	                          | .Where("{} != 5", "d")                              // WHERE d != 5
	                          | .Let("x", "10 + {}", "d")                           // LET x = 10 + d
	                          | .HappenedBeforeJoin("q2", Q2)                       // JOIN q2 IN Q2 ON q2 -> q
                              | .HappenedBeforeJoin(Filter.MOSTRECENT, "q2", Q2)    // JOIN q2 IN MOSTRECENT(Q2) ON q2 -> q
	_______________________________________________________________________________________________________________________
    ..GroupBy(..)             | .Count()                                            // SELECT .., COUNT
                              | .Sum("b")                                           // SELECT .., SUM(b)
                              | .Aggregate(e, Agg.MIN)                              // SELECT .., MIN(e)
	_______________________________________________________________________________________________________________________

#### Let and Where

Let and Where commands use replacement variables, because I didn't have time to parse expressions properly.  Occurrences of `{}` will be replaced by the corresponding positional variable, eg `.Where("{} != {}", "d", "e")` is logically equivalent to `d != e`.  If the variables are strings, you have to quote them, eg `.Where("\"{}\" != \"{}\"", "d", "e")`.  Ugly.  This is because, currently, I'm evaluating let and where expressions using Java's built-in javascript engine, and eval'ing a string.  Somebody... please... fix this.