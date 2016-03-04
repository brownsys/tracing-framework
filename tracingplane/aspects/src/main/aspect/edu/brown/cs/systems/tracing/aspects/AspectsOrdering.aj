package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.retro.aspects.cpu.Sleeping;
import edu.brown.cs.systems.retro.aspects.cpu.XTraceAPICalls;
import edu.brown.cs.systems.xtrace.logging.BaggageWrappers;

/** Declares aspect ordering for aspects from Retro and XTrace that modify the same classes */
public aspect AspectsOrdering {
    declare precedence : BaggageWrappers, Futures, Runnables, /* --- */ XTraceAPICalls, Sleeping;
}