package net.javacoding.jspider.core.task.dispatch;


import net.javacoding.jspider.core.Agent;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.exception.*;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.threading.WorkerThreadPool;


/**
 *
 * $Id: DispatchThinkerTasks.java,v 1.6 2003/03/27 17:44:19 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DispatchThinkerTasks extends BaseDispatchTaskImpl {

    protected WorkerThreadPool thinkers;
    protected Agent agent;
    protected SpiderContext context;

    public DispatchThinkerTasks(WorkerThreadPool thinkers, SpiderContext context) {
        super(context);
        this.thinkers = thinkers;
        this.agent = context.getAgent();
        this.context = context;
    }

    public void execute() {
        LogFactory.getLog(DispatchThinkerTasks.class).debug("Thinker task dispatcher running ...");
        while (running) {
            try {
                thinkers.assign(agent.getThinkerTask());
            } catch (NoSuitableItemFoundException e) {
            } catch (SpideringDoneException e) {
                running = false;
            } catch (TaskAssignmentException e) {
                // We dealt with all subclasses, so this shouldn't happen !!!
            }
        }
        LogFactory.getLog(DispatchThinkerTasks.class).debug("Thinker task dispatcher dying ...");
    }
}
