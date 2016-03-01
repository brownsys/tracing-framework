### X-Trace

This is an implementation of [X-Trace](https://github.com/rfonseca/X-Trace).  For information on X-Trace, check out the research papers from [NSDI '07](http://www.cs.berkeley.edu/~istoica/papers/2007/xtr-nsdi07.pdf) and [INM '10](http://static.usenix.org/events/inm10/tech/full_papers/Fonseca.pdf).  

Our implementation of X-Trace is built using Baggage as the metadata propagation layer.  Any Baggage-enabled system can deploy X-Trace without additional system instrumentation.

X-Trace runs as a client-server architecture.  X-Trace enabled clients generate *reports*, which are similar to logging statements.  Reports are sent to an X-Trace server, which persists them in the database.  X-Trace reports include identifiers that enable the server to reconstruct causality between reports.  The X-Trace server provides APIs and a web interface for getting reports and visualizing logs as *directed, acyclic graphs*.

This project also includes some automatic instrumentation aspects for wrapping log4j and commons loggers, to automatically generate X-Trace reports any time loggers are used.