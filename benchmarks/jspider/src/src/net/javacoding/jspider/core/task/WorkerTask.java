package net.javacoding.jspider.core.task;

/**
 * Interface that will be implemented upon each class that represents a
 * JSpider workertask that needs to be executed by a Worker Thread.
 *
 * JSpider has two types of tasks: spider tasks (that fetch data from a
 * web server), and thinker tasks (that interpret data, take decision,
 * etc...)
 *
 * $Id: WorkerTask.java,v 1.5 2003/04/25 21:29:02 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface WorkerTask extends Task {

    /**
     * Task type that is used for every task that will require the fetching
     * of data from a site.
     */
    public static final int WORKERTASK_SPIDERTASK = 1;

    /**
     * Task type used for all tasks that don't require any fetching of data
     */
    public static final int WORKERTASK_THINKERTASK = 2;

    /**
     * Returns the type of the task - spider or thinker.
     * @return the type of the task
     */
    public int getType ( );

    /**
     * Allows some work to be done before the actual Task is carried out.
     * During the invocation of prepare, the WorkerThread's state will be
     * WORKERTHREAD_BLOCKED.
     */
    public void prepare ( );

    /**
     * Allows us to put common code in the abstract base class.
     */
    public void tearDown ( );

}
