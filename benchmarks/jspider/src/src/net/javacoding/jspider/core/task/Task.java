package net.javacoding.jspider.core.task;

/**
 * Interface that will be implemented upon each Task to be carried out
 * by a Thread.
 *
 * $Id: Task.java,v 1.4 2003/04/25 21:29:02 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Task {

    /**
     * Has the task executed.  The thread calling this method will do it's
     * time in there :).
     */
    public void execute();

}
