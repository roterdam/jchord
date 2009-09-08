package net.javacoding.jspider.core.threading;


import net.javacoding.jspider.core.task.DispatcherTask;


/**
 *
 * $Id: DispatcherThread.java,v 1.4 2003/02/02 15:30:21 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
class DispatcherThread extends Thread {

    protected DispatcherTask task;
    protected WorkerThreadPool pool;

    public DispatcherThread(ThreadGroup group, String name, WorkerThreadPool pool) {
        super(group, name);
        this.pool = pool;
    }

    public void assign(DispatcherTask task) {
        this.task = task;
        start();
    }

    public void run() {
        synchronized (task) {
            task.execute();
            task.notify();
        }
    }


}
