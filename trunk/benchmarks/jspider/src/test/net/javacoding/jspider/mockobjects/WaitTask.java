package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.task.work.BaseWorkerTaskImpl;
import net.javacoding.jspider.core.task.WorkerTask;

/**
 * Mock implementation of a Worker task.  No real work is done, but there is
 * simply done a wait for a certain amount of milliseconds.
 *
 * $Id: WaitTask.java,v 1.8 2003/04/10 16:19:25 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public class WaitTask extends BaseWorkerTaskImpl {

    /** the number of milliseconds to wait. */
    protected int wait;

    /** the number of milliseconds to wait during prepare method. */
    protected int waitDuringPrepare;

    /**
     * Public constructor.
     * @param context the spidercontext in user
     * @param wait the number of milliseconds to wait
     */
    public WaitTask ( SpiderContext context, int wait ) {
        this ( context, wait, 0);
    }

    /**
     * Public constructor.
     * @param context the spidercontext in user
     * @param wait the number of milliseconds to wait
     */
    public WaitTask ( SpiderContext context, int wait, int waitDuringPrepare ) {
        super(context, WorkerTask.WORKERTASK_THINKERTASK);
        this.wait = wait;
        this.waitDuringPrepare = waitDuringPrepare;
    }

    public void prepare() {
        if ( waitDuringPrepare > 0 ) {
            //System.out.println("Waiting in prepare() : " + waitDuringPrepare + " ms");
            try {
                Thread.currentThread().wait(waitDuringPrepare);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Worker method implementation.  Will just wait the specified number
     * of milliseconds, and then return.
     */
    public void execute() {
        //System.out.println("Waiting in execute() : " + wait + " ms");
        try {
            Thread.currentThread().wait(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // overridden here to avoid NullPointerException when BaseImpl accesses Agent
    public void tearDown() {
    }

    public int getType() {
        return WorkerTask.WORKERTASK_THINKERTASK;
    }

}
