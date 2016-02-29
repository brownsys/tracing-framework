### Configuring X-Trace Logging with Retro

In addition to aggregating resource consumption statistics, Retro can explicitly generate an X-Trace report every time a resource is intercepted.  As normal, an X-Trace report will only be generated if the request is part of an X-Trace task (eg, that earlier in the request's execution, `XTrace.startTask` was called).  However, because there are many resources consumed during the execution of even just one request, this can generate many X-Trace reports (for example, reading a 4MB file from HDFS produces approximately 600 additional X-Trace reports).

X-Trace logging can be used even if Retro resource aggregation is disabled (eg, if `resource-reporting.aggregation.active = false`).

Retro uses the following X-Trace logging classes for its resource reports:

* `"Disk"` -- disk consumption reports.  The `Operation` field of the report will be one of `fileopen`, `fileread`, `filewrite`, `fileflush`, `filesync`, `fileop`.
* `"Execution"` -- events related to threads, such as starting and stopping processing in a thread, forking, joining, and sleeping.  The `Operation` field of the report will be one of `set`, `unset`, `branch`, `join`, `waited`.
* `"Network"` -- events related to network.  The `Operation` field of the report will be one of `netconnect`, `netread`, `netwrite`, `netflush`, `netclose`, `loopback-read`, `loopback-write`.
* `"Queue"` -- events related to instrumented queues.  The `Operation` field of the report will be one of `threadpool-enqueue`, `threadpool-start`, `threadpool-end`.
* `"MonitorLock"` -- events related to monitor locks.  The `Operation` field of the report will be one of `lockacquire` and `lockrelease`.  Monitor locks are not instrumented by default with Retro, and must be configured as described in the [build options documentation](buildoptions.html).
* `"ReadWriteLock"` -- events related to read/write locks.  The `Operation` field of the report will be one of `readlockrequest`, `readlockacquire`, `readlockrelease`, `writelockrequest`, `writelockacquire`, `writelockrelease`.
