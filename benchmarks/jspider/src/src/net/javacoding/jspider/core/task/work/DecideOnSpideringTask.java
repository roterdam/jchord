package net.javacoding.jspider.core.task.work;


import net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent;
import net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent;
import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.core.event.impl.URLFoundEvent;
import net.javacoding.jspider.core.rule.Ruleset;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.util.URLUtil;

import java.net.URL;


/**
 *
 * $Id: DecideOnSpideringTask.java,v 1.21 2003/04/10 16:19:13 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DecideOnSpideringTask extends BaseWorkerTaskImpl {

    protected Storage storage;
    protected URLFoundEvent event;
    protected EventDispatcher eventDispatcher;

    public DecideOnSpideringTask(SpiderContext context, URLFoundEvent urlFoundEvent) {
        super(context, WorkerTask.WORKERTASK_THINKERTASK);
        this.event = urlFoundEvent;
        this.eventDispatcher = context.getEventDispatcher();
        this.storage = context.getStorage();
    }

    public void prepare() {
    }

    public synchronized void execute() {
        URL url = event.getURL();
        URL foundURL = event.getFoundURL();

        URL currentSiteURL = URLUtil.getSiteURL(url);
        URL siteURL = URLUtil.getSiteURL(foundURL);

        Site currentSite = null;
        if ( currentSiteURL != null ) {
            currentSite = storage.getSiteDAO().find(currentSiteURL);
        }
        Site site = storage.getSiteDAO().find(siteURL);

        Resource foundResource = storage.getResourceDAO().getResource(foundURL);

        Ruleset spiderRules = context.getSiteSpiderRules(site);
        Decision spiderDecision = spiderRules.applyRules(context, currentSite, foundURL);

        storage.getDecisionDAO().saveSpiderDecision(foundResource, spiderDecision);

        switch (spiderDecision.getDecision()) {
            case Decision.RULE_IGNORE:
                storage.getResourceDAO().setIgnoredForFetching(foundURL, event);
                eventDispatcher.dispatch(new ResourceIgnoredForFetchingEvent(foundResource));
                break;
            case Decision.RULE_FORBIDDEN:
                storage.getResourceDAO().setForbidden(foundURL, event);
                eventDispatcher.dispatch(new ResourceForbiddenEvent(foundResource));
                break;
            case Decision.RULE_ACCEPT:
            case Decision.RULE_DONTCARE:
            default:
                context.getAgent().scheduleForSpidering(foundURL);
                break;
        }
    }

}
