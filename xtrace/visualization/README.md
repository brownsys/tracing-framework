## XTrace - WebUI Graph Visualizations

The visualization package contains several web-based visualizations of X-Trace tasks.  They are mostly written using D3.  They can be accessed through the X-Trace web UI, usually at http://localhost:4080

#### Animated

This visualization draws the X-Trace reports as a directed, acyclic graph, with causality going top to bottom.  Each node of the graph is one X-Trace report:

![X-Trace animated task graph](../docs/images/xtrace_viz_interactive.png "Animated DAG visualization of an X-Trace task")

The visualization is interactive -- hovering over a node will show the X-Trace report details and highlight the causal path in the graph:

![X-Trace animated task graph](../docs/images/xtrace_viz_interactive_tooltip.png "Animated DAG visualization of an X-Trace task")

A right-click context menu allows you to manually hide nodes and select paths in the graph.

The "Lite" graph is the same as the Animated graph, but lacks animations, making it a better choice for large graphs that might be slow to render.

#### SwimLane

This visualization draws the X-Trace reports as a swimlane, with time going from left to right.  Each dot is one X-Trace report.  Each row is one thread in one process.  Threads of the same process are color coded.  Rows are labelled on the left with the thread names.  Lines are drawn between dots if there is a direct causal relation between them (eg, if report A was generated followed by report B, a line will be drawn from A to B).

![X-Trace swimlane visualization](../docs/images/xtrace_viz_swimlane.png "Swimlane visualization of an X-Trace task")

Hovering over a node will show the X-Trace report details.

![X-Trace swimlane visualization](../docs/images/xtrace_viz_swimlane_tooltip.png "Swimlane visualization of an X-Trace task")

If Retro is also [configured to log X-Trace reports](../docs/retro/retrowithxtrace.html) when resources are consumed, the swimlane graph will have additional highlights for garbage collection, disk events, and network events.

![X-Trace swimlane visualization](../docs/images/xtrace_viz_swimlane_retro.png "Swimlane visualization of an X-Trace task")