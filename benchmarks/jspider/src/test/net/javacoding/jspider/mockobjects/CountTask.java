package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.mockobjects.util.Counter;


/**
 * $Id: CountTask.java,v 1.5 2003/04/09 17:08:15 vanrogu Exp $
 */
public class CountTask implements WorkerTask {

    protected Counter counter;

    public CountTask ( Counter counter ) {
        this.counter = counter;
    }

    public void prepare() {
    }

    public void execute() {
        counter.increment ( );
    }

    public void tearDown() {
    }

    public int getType() {
        return WorkerTask.WORKERTASK_THINKERTASK;
    }

}
