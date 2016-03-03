## Pivot Tracing

Pivot Tracing is a monitoring framework for distributed systems that can seamlessly correlate statistics from across applications, components, and machines at runtime without needing to change or redeploy system code.  Users can define and install monitoring queries on-the-fly to collect arbitrary statistics from one point in the system while being able to select, filter, and group by events meaningful at other points in the system.

Pivot Tracing does not correlate cross-component events using expensive global aggregations, nor does it perform offline analysis.  Instead, Pivot Tracing directly correlates events as they happen by piggybacking metadata alongside requests as they execute -- even across component and machine boundaries.

For more information on Pivot Tracing, check out the research paper from [SOSP '15](http://pivottracing.io/mace15pivot.pdf) (Best Paper Award)

Head over to the [tutorials](http://brownsys.github.io/tracing-framework/docs/tutorials.html) section to get started with Pivot Tracing.