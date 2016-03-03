## Pivot Tracing - Privileged Agent

Under normal circumstances, Pivot Tracing cannot instrument privileged Java classes (eg, classes that are part of the Java standard library), because Pivot Tracing is unprivileged.  While it is actually allowed to rewrite and reload privileged Java classes, it is invalid for a privileged class to directly invoke code on a non-privileged class or interface.  If this occurs, you'll see a `NoClassDefFoundError`.

The privileged agent provides a way to instrument privileged classes.  To instrument privileged classes, you must add the Pivot Tracing Privileged Jar to the boot classpath of your JVM, ie, run the JVM with the following command:

    -Xbootclasspath/p:${path.to.edu.brown.cs.systems:pivottracing-privileged:jar}

This will enable privileged classes to invoke advice by proxying the call through a privileged interface.  However, the privileged advice may be unable to access some unprivileged variables (not fundamental, just not currently supported).

