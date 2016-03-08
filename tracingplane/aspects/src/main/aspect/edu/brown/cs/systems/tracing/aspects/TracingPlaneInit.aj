package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.baggage.BaggageUtils;

/**
 * Instruments all main methods
 */
public aspect TracingPlaneInit {
    
    declare precedence: TracingPlaneInit, *;

    before(): execution(public static void main(String[])) {
        BaggageUtils.checkEnvironment(System.getenv());
    }

}
