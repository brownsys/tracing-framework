DistributedPubSub
=================

A simple pub-sub implementation with a single server using Java's NIO

Depends on:
* Typesafe Config
* Protocol Buffers 2.5.0


REQUIRED INSTALLATION:

* Protocol Buffers
	- Install protocol buffers (and make sure the protoc executable is on your PATH): http://code.google.com/p/protobuf/downloads/list
	- Ensure the protoc executable is on your PATH (test with 'protoc --version')
	
	
TODO / TESTS:

- Test reconnect after connection closes
- Test reconnect if server not running
- Test reconnect if server dies
- Test resubscribe after connection closes
- Test publish works
- Test publish work after reconnecting
- Test publish on multiple topics is routed correctly
- Test subscribe receives messages
- Test with multiple clients

- Perf tests (publish speed, throughput)
- User guide