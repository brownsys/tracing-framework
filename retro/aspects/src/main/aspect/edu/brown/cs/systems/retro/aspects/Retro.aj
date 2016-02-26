package edu.brown.cs.systems.retro.aspects;

import edu.brown.cs.systems.clockcycles.CPUCycles;
import edu.brown.cs.systems.retro.logging.GarbageCollection;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport.Decorator;

public aspect Retro {

    private static boolean initialized = false;

    public static synchronized void init() {
        if (!initialized) {
            initialized = true;

            if (XTraceSettings.On()) {
                // Turn on garbage collection logging
                GarbageCollection.register();

                // Add CPU cycles to reports
                XTraceReport.addDecorator(new Decorator() {
                    @Override
                    public void decorate(XTraceReport report) {
                        report.builder.setCycles(CPUCycles.get());
                    }
                });
            }
        }
    }

    before(): execution(public static void main(String[])) {
        init();
    }

}
