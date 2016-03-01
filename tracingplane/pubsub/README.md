## PubSub

This project is a simple pub-sub implementation with a single server using Java's NIO.  It is not too reliable, and not too robust, but it is simple.  We rolled our own pubsub system because we had poor experiences with heavyweight libraries like ZeroMQ and Akka, and because this is research code.

To run a standalone pub sub server, run the `server` executable in `target/appassembler/bin`.

Once a pub sub server is running, clients can send and receive messages using the static API in `edu.brown.cs.systems.pubsub`.

PubSub depends on protocol buffers, and provides convenience methods for sending and receiving protobuf messages.

To send any protocol buffers message on a topic, invoke `PubSub.publish(String topic, Message message)`.  PubSub will internally serialize the method and send it to the pub sub server.  The server will forward the message to any clients that have subscribed to the specified topic.

To subscribe to messages on a topic, extend the class `PubSubClient.Subscriber<T extends Message>` to override the `OnMessage(T message)` method.  Then call `PubSub.subscribe(String topic, Subscriber subscriber)`.  PubSub will internally register with the pub sub server to subscribe to the topic.  When anyone publishes a message on that topic, the pub sub server will forward the message to this client, and the callback will be invoked with the deserialized message.

PubSub clients automatically attempt to reconnect to the server if they are unable to connect.  Any pending or partially sent messages will remain queued until the client reconnects, at which point they will be retransmitted.

PubSub uses the following default configuration options:

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

