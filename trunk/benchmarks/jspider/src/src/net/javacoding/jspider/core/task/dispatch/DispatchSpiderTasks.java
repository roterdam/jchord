package net.javacoding.jspider.core.task.dispatch;


import net.javacoding.jspider.core.Agent;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.exception.*;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.threading.WorkerThreadPool;


/**
 *
 * $Id: DispatchSpiderTasks.java,v 1.13 2003/03/27 17:44:18 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DispatchSpiderTasks extends BaseDispatchTaskImpl {

    protected WorkerThreadPool spiders;
    protected Agent agent;
    protected SpiderContext context;

    public DispatchSpiderTasks(WorkerThreadPool spiders, SpiderContext context) {
        super(context);
        this.spiders = spiders;
        this.agent = context.getAgent();
        this.context = context;
    }

    public void execute() {
        LogFactory.getLog(DispatchSpiderTasks.class).debug("Spider task dispatcher running ...");
        while (running) {
            try {
                spiders.assign(agent.getSpiderTask());
            } catch (NoSuitableItemFoundException e) {
            } catch (SpideringDoneException e) {
                running = false;
            } catch (TaskAssignmentException e) {
                // We dealt with all subclasses, so this shouldn't happen !!!
            }
        }
        LogFactory.getLog(DispatchSpiderTasks.class).debug("Spider task dispatcher dying ...");

    }

}
