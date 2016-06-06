# PubSub #

This is a simple Java pub-sub server and client.  It is used by several of our projects (X-Trace, Retro, and Pivot Tracing) for communicating between distributed components.

Our implementation is pretty simple, but has not undergone extensive testing, so we make no claims about it being robust.  We rolled our own due to poor experiences with more heavyweight libraries like ZeroMQ and Akka.

## Running the PubSub server ##

After building, the `server` executable in `target/appassembler/bin` will run a pubsub server:

    tracingplane/pubsub/target/appassembler/bin/server

You should see output similar to the following

    11:30:33,053  INFO PubSubServer:59 - Creating server hostname 0.0.0.0, port 5563

## Configuring the PubSub server ##

The server can be configured to bind to a specific hostname and port.  It uses the typesafe config and supports the following configuration options:

	pubsub {
	  
	  server {
	  	hostname = "127.0.0.1"
	  	bindto = "0.0.0.0"
	  	port = 5563
	  }
	  
	  topics {
	  	control = "_"
	  }
	    
	}

## Using PubSub from code ##

Clients can send and receive pubsub messages.  Before running your code, make sure `pubsub.server.hostname` and `pubsub.server.port` are configured correctly (the default values will work if you are running locally).

Clients can send and receive pubsub messages using the static PubSub API.  The following code will publish the message "Hello World!" on the topic "GreetingsTopic"

    import edu.brown.cs.systems.pubsub.PubSub;

    PubSub.publish("GreetingsTopic", "Hello World!");

To receive messages published on topics, you must create an instance of `Subscriber` that overrides the `OnMessage` function.  If you are only publishing strings, then the `SimpleSubscriber` class will suffice.  The following code will subscribe to the "GreetingsTopic" topic and print to the command line whenever a message is received:

    import edu.brown.cs.systems.pubsub.PubSub;
    import edu.brown.cs.systems.pubsub.PubSubClient.SimpleSubscriber;
    
    PubSub.subscribe("GreetingsTopic", new SimpleSubscriber() {
        @Override
        protected void OnMessage(String message) {
            System.out.println("Received: " + message);
        }
    });

## Using PubSub with Protocol Buffers ##

PubSub supports publishing and receiving arbitrary protocol buffers messages.  Suppose we have a protocol buffers message defined called `MyProtoBuf` and an instance of it, `myProtoInstance`.  The following code will publish the message on the topic "MyTopic":

    import edu.brown.cs.systems.pubsub.PubSub;
    import my.package.MyProtoBuf;

    PubSub.publish("MyTopic", myProtoInstance);

The following code will subscribe to the "MyTopic" topic and print to the command line whenever a message is received:

    import edu.brown.cs.systems.pubsub.PubSub;
    import edu.brown.cs.systems.pubsub.PubSubClient.Subscriber;
    import my.package.MyProtoBuf;

    PubSub.subscribe("MyTopic", new Subscriber<MyProtoBuf>() {
        @Override
        protected void OnMessage(MyProtoBuf message) {
            System.out.println("Received: " + message);
        }
    });

Using protocol buffers messages makes it easy to pass data structures around, but make sure that you do don't mix message types on the same topic.  If this happens, you will see errors printed to stderr, because of invalid deserialization.

## PubSub Internals ##

PubSub clients automatically attempt to reconnect to the server if they are unable to connect.  Any pending or partially sent messages will remain queued until the client reconnects, at which point they will be retransmitted.

Messages are not buffered on the server.  A newly-connected client will not receive messages that were transmitted before the client connected.



