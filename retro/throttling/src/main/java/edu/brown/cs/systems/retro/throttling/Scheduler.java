package edu.brown.cs.systems.retro.throttling;

import edu.brown.cs.systems.resourcethrottling.schedulers.SchedulerMessages.SchedulerSpecification;

public interface Scheduler {

    public void schedule();

    public void schedule(double cost);

    public void scheduleInterruptably() throws InterruptedException;

    public void scheduleInterruptably(double cost) throws InterruptedException;

    public void complete();

    public void complete(double actualCost);

    /** Update the scheduler settings */
    public void update(SchedulerSpecification spec);

    /** Clear the scheduler settings */
    public void clear();

}
