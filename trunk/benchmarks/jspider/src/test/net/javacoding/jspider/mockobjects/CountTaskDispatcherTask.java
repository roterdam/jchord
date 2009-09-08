package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.task.dispatch.BaseDispatchTaskImpl;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.threading.WorkerThreadPool;
import net.javacoding.jspider.mockobjects.util.Counter;

/**
 * $Id: CountTaskDispatcherTask.java,v 1.4 2003/04/09 17:08:15 vanrogu Exp $
 */
public class CountTaskDispatcherTask extends BaseDispatchTaskImpl {

    protected WorkerThreadPool pool;
    protected Counter counter;
    protected int number;

    public CountTaskDispatcherTask ( SpiderContext context, WorkerThreadPool pool, Counter counter, int number ) {
        super ( context );
        this.counter = counter;
        this.pool = pool;
        this.number = number;
    }

    public void execute() {
        for ( int i = 0; i < number; i++ ) {
            pool.assign(new CountTask(counter));
        }
    }

    public int getType() {
        return WorkerTask.WORKERTASK_THINKERTASK;
    }

}
