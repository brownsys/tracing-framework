# Running PubSub

All of the instrumentation libraries use a pub sub system to communicate.  Agents running in the instrumented system will receive commands over pubsub, and send their output back over the pubsub.

To run a pub sub server, run the `server` executable that is built in the `target/appassembler/bin` folder of the `tracingplane/pubsub` project.

In general, some projects have some handy executables that will be located in the `target/appassembler/bin` folder of the project.

The following might be of use:

* `tracingplane/pubsub` `server` runs a pubsub server
* `xtrace/server` `backend` runs a pubsub server plus the X-Trace reports server, and the X-Trace web server as described on the X-Trace [page](projects/xtrace.html)
* `pivottracing/client` `print-query-results` connects to the pubsub server and prints to the command line all Pivot Tracing query results.  See the Pivot Tracing [page](projects/pivottracing.html) for more details
* `retro/aggregation` `test-subscriber` connects to the pubsub server and prints to the command line all resource measurements made by Retro.  See the Retro [page](projects/retro.html) for more details
* `retro/visualization` `visualization-server` run a visualization server for Retro.  See the Retro [page](projects/retro.html) for more details

