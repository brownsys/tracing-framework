### Lock Instrumentation

Retro has the ability to instrument locks in Java, including regular `ReadWriteLock` and monitor locks (eg, use of the `synchronized` keyword).  However, due to the prevalent use of monitor locks in many Java applications, and because lock instrumentation can be delicate, it is disabled by default except for a few specially selected HDFS locks.

To enable lock instrumentation, build the `retro/aspects` project with the `-Plocks` flag:

    mvn clean package install -Plocks

This will instrument any `java.util.concurrent.lock.Lock` and any use of the `synchronized` keyword.

This should be used carefully.  Most locks should not be instrumented, only those that that are bottlenecks in the system.  Our recommendation is to enable all lock instrumentation so that you can run the system and identify the subset of important locks.  Then extend Retro to instrument only those locks.  See the `XHDFS.aj` aspect in `retro/aspects` for example usage in HDFS.