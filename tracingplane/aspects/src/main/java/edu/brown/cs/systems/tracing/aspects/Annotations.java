package edu.brown.cs.systems.tracing.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotations used for automatic instrumentation */
public class Annotations {

    /** Indicate that this runnable / thread should not be instrumented to automatically take baggage from caller */
    @Inherited
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BaggageInheritanceDisabled {}

}
