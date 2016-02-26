package edu.brown.cs.systems.pivottracing.aspects;

import edu.brown.cs.systems.pivottracing.agent.PivotTracing;

public aspect PivotTracingInit {

    before(): execution(public static void main(String[])) {
        PivotTracing.initialize();
    }

}
