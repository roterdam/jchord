package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.task.dispatch.BaseDispatchTaskImpl;
import net.javacoding.jspider.core.threading.WorkerThreadPool;

/**
 * Mock implementation of a dispatcher task.  It will simply dispatch a
 * configured amount of Wait tasks to the pool.
 *
 * $Id: WaitTaskDispatcherTask.java,v 1.3 2003/03/27 17:44:32 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class WaitTaskDispatcherTask extends BaseDispatchTaskImpl {

    /** The thread pool we'll be dispatching tasks to. */
    protected WorkerThreadPool threadPool;

    /** number of tasks to dispatch. */
    protected int count;

    /** amount of milliseconds to wait in a task. */
    protected int wait;

    /**
     * Public constructor.
     * @param count nr of tasks to dispatch
     * @param wait nr of ms to wait in these tasks
     */
    public WaitTaskDispatcherTask(WorkerThreadPool threadPool, SpiderContext context, int count, int wait) {
        super(context);
        this.threadPool = threadPool;
        this.count = count;
        this.wait = wait;
    }

    public void execute() {
        for ( int i = 0; i < count; i++ ) {
           threadPool.assign(new WaitTask(context, wait));
        }
    }

}
