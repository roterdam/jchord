package net.javacoding.jspider.core.task.work;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEvent;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.task.WorkerTask;

import java.net.URL;


/**
 *
 * $Id: BaseWorkerTaskImpl.java,v 1.4 2003/04/10 16:19:08 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public abstract class BaseWorkerTaskImpl implements WorkerTask {

    protected Log log;
    protected int type;
    protected SpiderContext context;

    public BaseWorkerTaskImpl(SpiderContext context, int type) {
        this.log = LogFactory.getLog(this.getClass());
        this.type = type;
        this.context = context;
    }

    public void tearDown() {
        context.getAgent().flagDone(this);
    }

    public int getType() {
        return type;
    }

    protected void notifyEvent(URL url, CoreEvent event) {
        if ( event == null ) {
            log.error("PANIC! event is null!");
            log.error("URL: " + url);
        } else {
          context.getAgent().registerEvent(url, event);
        }
    }

}
