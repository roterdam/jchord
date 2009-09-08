package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.task.DispatcherTask;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.threading.WorkerThreadPool;

/**
 * $Id: MockWorkerThreadPool.java,v 1.2 2003/03/27 17:44:32 vanrogu Exp $
 */
public class MockWorkerThreadPool extends WorkerThreadPool {

    public MockWorkerThreadPool() {
        super("testPool", "testThread", 0);
    }

    public synchronized void assign(WorkerTask task) {

    }

    public void assignGroupTask(DispatcherTask task) {
    }

    public int getOccupation() {
        return 0;
    }

    public void stopAll() {
    }

    public int getSize() {
        return 0;
    }

}
