# TracingPlane - Baggage Client

Note: this Baggage implementation works, but is **not** the most up to date work on Baggage -- for that, see https://github.com/JonathanMace/tracingplane

The `tracingplane/client` project contains our reference implementation of Baggage.  Baggage is a generic container of key-value pairs that is propagated alongside requests as they execute in a system.  

Each request has its own baggage.  When a request is running in a thread, its baggage resides in a thread local variable.  At any point in time, key-value pairs can be added to the request's baggage, inspected, or removed.

## Baggage Overview ##

There are two considerations when using Baggage:

**Instrumentation** Your system must be *instrumented* to pass baggage around alongside requests as they runs.  This is a pre-requisite to using Baggage -- if your system is not instrumented, then your request will *leave behind* its key-value pairs when it crosses execution boundaries.  Worse, other requests will *find and pick up* key-value pairs that don't belong to them.  [Adding Baggage To Your System](docs/tutorials/baggage.html) is a tutorial on how to instrument your system.

**Exploitation** Once your system is instrumented with Baggage, you can write your own monitoring, diagnosis, or more exotic systems that take advantage of Baggage.  You do this by putting key-value pairs into Baggage at one point in a request's execution, then later on (potentially on a different machine or in a different component), retrieving key-value pairs.  Baggage APIs are simple.  They are based on *sets* rather than single values.  [Using Baggage Yourself](docs/tutorials/tracingapplication.html) is a tutorial which explains how to use the Baggage APIs and why they have a design based on sets.

We have already instrumented several systems with Baggage, which means you can use them out-of-the-box without having to add instrumentation.  This includes HDFS, YARN, Tez, and Spark.

Furthermore, we have several applications that you can use out-of-the-box once your system is instrumented with Baggage.  These include X-Trace, Retro, and Pivot Tracing.

## Interaction API: BaggageContents ##

The interaction API is a static API in `edu.brown.cs.systems.baggage.BaggageContents`.  

Baggage is sub-divided into *namespaces*.  These are user-defined, and the goal is to make sure no two different applications accidentally conflict and try to store data under the same key.  For example, the key "id" is a generic, commonly-used key.

The following code adds the key-value pair "RequestType", "WriteRow" to the namespace "MyNamespace":

    import edu.brown.cs.systems.baggage.BaggageContents;

    BaggageContents.add("MyNamespace", "RequestType", "WriteRow");

Under the covers, Baggage is stored in a thread-local variable.  By default, all threads have an implicit empty baggage.  After making the previous API call, the calling thread's Baggage will now contain "RequestType": "WriteRow" under the namespace "MyNamespace".  From the same thread, we can retrieve the value by calling:

    BaggageContents.get("MyNamespace", "RequestType");

This API call returns a *set* of values.  In this case, the only value in the set will be "WriteRow".  If we make the following additional calls:

    BaggageContents.add("MyNamespace", "RequestType", "ReadRow");
    BaggageContents.add("MyNamespace", "RequestType", "WriteRow");
    BaggageContents.add("MyNamespace", "RequestType", "WriteRow");

Then the returned set will contain "ReadRow" and "WriteRow" -- the duplicate additions of "WriteRow" will be ignored.

Other methods include the following:

- `removeAll` removes all key-value pairs under a namespace
- `replace` replaces all values for a key
- `keys` returns all keys under a namespace
- `contains` tests the existence of a key

## Instrumentation API: Baggage ##

The instrumentation API is a static API in `edu.brown.cs.systems.baggage.Baggage`.  It has the following API calls:

    public static void Baggage.discard();

This method simply discards all of the contents of the current thread's baggage, effectively starting a new, empty baggage in the current thread.

    public static DetachedBaggage Baggage.stop();

This method takes the contents of the current thread's baggage and saves them in the returned `DetachedBaggage` object.  After making this call, the current thread's baggage will be empty.  The thread's previous baggage contents are persisted in the `DetachedBaggage`.

    public static DetachedBaggage Baggage.fork();

This method is like `Baggage.stop()`, but instead it creates and returns a copy of the current thread's baggage.  After making this call, the current thread's baggage will remain unchanged, and the returned DetachedBaggage is a copy that can be saved and used elsewhere.

DetachedBaggage objects do **not** have an interaction API like BaggageContents.  You cannot add key-value pairs directly to DetachedBaggage instances.  `Baggage.stop()` never returns null; if a thread had no baggage contents, then a DetachedBaggage instance representing the empty baggage is returned.

    public static byte[] DetachedBaggage.toByteArray();
    public static ByteString DetachedBaggage.toByteString();
    public static String DetachedBaggage.toStringBase64();

These member methods of the DetachedBaggage class encode an instance of DetachedBaggage into various byte or String representations.  There are similar static methods to decode DetachedBaggage from byte or string representations:

    public static DetachedBaggage DetachedBaggage.deserialize(byte[]);
    public static DetachedBaggage DetachedBaggage.deserialize(ByteString);
    public static DetachedBaggage DetachedBaggage.decode(String);

These methods attempt to deserialize an instance of DetachedBaggage from a serialized / encoded representation.  If the provided bytes are null or invalid, the method call will return a DetachedBaggage instance representing the empty baggage.

    public static void Baggage.start(DetachedBaggage);
    public static void Baggage.start(byte[]);
    public static void Baggage.start(ByteString);
    public static void Baggage.start(String);

These static methods reinstate in the current thread the DetachedBaggage provided as argument.  If byte or String representations are provided, an attempt is made to deserialize a DetachedBaggage instance from the provided bytes, equivalent to `Baggage.start(DetachedBaggage.deserialize(bytes))`.

After this call returns, the current thread's baggage will contain exactly the contents of whatever the DetachedBaggage contained.  Thus, a call to `Baggage.stop()` followed by a later call to `Baggage.start()` are conceptually saving and then restoring Baggage.

If the current thread had some baggage contents before making this call, then those contents are discarded.  If you wish to save the old contents for later, then the method `Baggage.swap()` should be used, which swaps out and returns the old contents.  If you wish to merge the old contents with the new contents, then the following `join` methods should be used:

    public static void Baggage.join(DetachedBaggage);
    public static void Baggage.join(byte[]);
    public static void Baggage.join(ByteString);
    public static void Baggage.join(String);

These methods merge the calling thread's current baggage contents with the contents of the provided baggage.  After this call, the calling thread's baggage will contain both its contents from before the call, plus the new contents of the provided baggage.

    public static DetachedBaggage DetachedBaggage.merge(DetachedBaggage, DetachedBaggage);

This method merges the contents of two DetachedBaggage instances into one.
