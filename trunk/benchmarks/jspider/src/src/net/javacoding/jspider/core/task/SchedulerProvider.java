package net.javacoding.jspider.core.task;


/**
 * Provider interface that must be implemented upon each Provider class
 * that can generate task schedulers.
 *
 * $Id: SchedulerProvider.java,v 1.2 2002/11/27 22:07:37 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public interface SchedulerProvider {

    /**
     * Creates an instance of a JSpider Task Scheduler.
     * @return Scheduler instance
     */
    public Scheduler createScheduler();

}
