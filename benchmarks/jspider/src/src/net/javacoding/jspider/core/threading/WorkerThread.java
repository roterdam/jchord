package net.javacoding.jspider.core.threading;


import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.task.WorkerTask;


/**
 * Implementation of  a Worker Thread.
 * This thread will accept WorkerTasks and execute them.
 *
 * $Id: WorkerThread.java,v 1.11 2003/04/02 20:55:26 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
class WorkerThread extends Thread {

    public static final int WORKERTHREAD_IDLE = 0;
    public static final int WORKERTHREAD_BLOCKED = 1;
    public static final int WORKERTHREAD_BUSY = 2;


    /** the current state of this thread - idle, blocked, or busy. */
    protected int state;

    /** Whether this instance is assigned a task. */
    protected boolean assigned;

    /** Whether we should keep alive this thread. */
    protected boolean running;

    /** Threadpool this worker is part of. */
    protected WorkerThreadPool stp;

    /** Task this worker is assigned to. */
    protected WorkerTask task;

    /**
     * Public constructor.
     * @param stp thread pool this worker is part of
     * @param name name of the thread
     * @param i index in the pool
     */
    public WorkerThread(WorkerThreadPool stp, String name, int i) {
        super(stp, name + " " + i);
        this.stp = stp;
        running = false;
        assigned = false;
        state = WORKERTHREAD_IDLE;
    }

    /**
     * Tests whether this worker thread instance can be assigned a task.
     * @return whether we're capable of handling a task.
     */
    public boolean isAvailable() {
        return (!assigned) && running;
    }

    /**
     * Determines whether we're occupied.
     * @return boolean value representing our occupation
     */
    public boolean isOccupied() {
        return assigned;
    }

    /**
     * Method that allows the threadPool to assign the worker a Task.
     * @param task WorkerTask to be executed.
     */
    public synchronized void assign(WorkerTask task) {
        if ( !running ) {
            //SHOULDN'T HAPPEN WITHOUT BUGS
            throw new RuntimeException("THREAD NOT RUNNING, CANNOT ASSIGN TASK !!!");
        }
        if (assigned) {
            //SHOULDN'T HAPPEN WITHOUT BUGS
            throw new RuntimeException("THREAD ALREADY ASSIGNED !!!");
        }
        this.task = task;
        assigned = true;
        notify();
    }

    /**
     * Tells this thread not to accept any new tasks.
     */
    public synchronized void stopRunning() {
        if ( ! running ) {
            throw new RuntimeException ("THREAD NOT RUNNING - CANNOT STOP !");
        }
        if ( assigned ) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        running = false;
        notify();
    }

    /**
     * Returns the state of this worker thread (idle, blocked or busy).
     * @return
     */
    public int getState1 ( ) {
        return state;
    }

    /**
     * Thread's overridden run method.
     */
    public synchronized void run() {
        running = true;

        Log log = LogFactory.getLog(WorkerThread.class);
        log.debug("Worker thread (" + this.getName() + ") born");

        synchronized (stp) {
            stp.notify();
        }

        while (running) {
            if (assigned) {
                state = WORKERTHREAD_BLOCKED;
                task.prepare();
                state = WORKERTHREAD_BUSY;
                try {
                    task.execute();
                    task.tearDown();
                } catch (Exception e) {
                    log.fatal("PANIC! Task " + task + " threw an excpetion!", e);
                    System.exit(1);
                }

                synchronized (stp) {
                    assigned = false;
                    task = null;
                    state = WORKERTHREAD_IDLE;
                    stp.notify();
                    this.notify(); // if some thread is blocked in stopRunning();
                }

            }
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /* notify the thread pool that we died. */
        log.debug("Worker thread (" + this.getName() + ") dying");
    }
}
