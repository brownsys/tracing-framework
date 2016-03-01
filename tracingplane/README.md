### Tracing Plane

http://brownsys.github.io/tracing-framework/tracingplane

The Tracing Plane project contains common components to X-Trace, Retro, and Pivot Tracing.  It provides the core generic metadata propagation, Baggage, that the other projects are built on top of.  It also provides the backbone PubSub system the components use to communicate, the metadata propagation aspects that automatically add Baggage in places that match certain execution patterns, and the dynamic instrumentation library to rewrite and reload Java classes on the fly.