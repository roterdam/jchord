package net.javacoding.jspider.core.impl;


import net.javacoding.jspider.api.event.resource.*;
import net.javacoding.jspider.api.event.site.*;
import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.Agent;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.core.event.CoreEvent;
import net.javacoding.jspider.core.event.CoreEventVisitor;
import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.exception.SpideringDoneException;
import net.javacoding.jspider.core.exception.TaskAssignmentException;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.task.*;
import net.javacoding.jspider.core.task.work.*;
import net.javacoding.jspider.core.util.URLUtil;

import java.io.ByteArrayInputStream;
import java.net.URL;


/**
 *
 * $Id: AgentImpl.java,v 1.32 2003/04/29 17:53:47 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class AgentImpl implements Agent, CoreEventVisitor {

    protected Storage storage;
    protected SpiderContext context;
    protected EventDispatcher eventDispatcher;
    protected Scheduler scheduler;
    protected Log log;


    public AgentImpl(SpiderContext context) {
        this.context = context;
        this.storage = context.getStorage();
        this.eventDispatcher = context.getEventDispatcher();
        this.scheduler = new SchedulerFactory().createScheduler(context);

        log = LogFactory.getLog(Agent.class);

    }

    public synchronized void start() {
        URL baseURL = context.getBaseURL();
        visit(null, new URLFoundEvent(context, null, baseURL));
        notifyAll();
    }

    public synchronized void flagDone(WorkerTask task) {
        scheduler.flagDone(task);
        notifyAll();
    }

    public synchronized WorkerTask getThinkerTask() throws TaskAssignmentException {
        while (true) {
            try {
                return scheduler.getThinkerTask();
            } catch (SpideringDoneException e) {
                throw e;
            } catch (TaskAssignmentException e) {
                try {
                    wait();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public synchronized WorkerTask getSpiderTask() throws TaskAssignmentException {
        while (true) {
            try {
                return scheduler.getFethTask();
            } catch (SpideringDoneException e) {
                throw e;
            } catch (TaskAssignmentException e) {
                try {
                    wait();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * @param foundURL
     */
    public synchronized void scheduleForSpidering(URL foundURL) {
        URL siteURL = URLUtil.getSiteURL(foundURL);
        Site site = storage.getSiteDAO().find(siteURL);
        scheduler.schedule(new SpiderHttpURLTask(context, foundURL, site));
        notifyAll();
    }

    public synchronized void scheduleForParsing(URL url) {
        scheduler.schedule(new InterpreteHTMLTask(context, (FetchedResource) storage.getResourceDAO().getResource(url)));
        notifyAll();
    }

    public synchronized void registerEvent(URL url, CoreEvent event) {
        event.accept(url, this);
        notifyAll();
    }


    public void visit(URL url, CoreEvent event) {
        log.error("ERROR -- UNHANDLED COREEVENT IN AGENT !!!");
    }

    public void visit(URL url, URLSpideredOkEvent event) {
        storage.getResourceDAO().setSpidered(url, event);
        eventDispatcher.dispatch(new ResourceFetchedEvent(storage.getResourceDAO().getResource(url)));
        scheduler.schedule(new DecideOnParsingTask(context, url));
    }

    public void visit(URL url, URLSpideredErrorEvent event) {
        storage.getResourceDAO().setError(url, event);
        eventDispatcher.dispatch(new ResourceFetchErrorEvent(storage.getResourceDAO().getResource(url), event.getHttpStatus()));
    }

    public void visit(URL url, ResourceParsedOkEvent event) {
        storage.getResourceDAO().setParsed(url, event);
        eventDispatcher.dispatch(new ResourceParsedEvent(storage.getResourceDAO().getResource(url)));
    }

    public void visit(URL url, ResourceParsedErrorEvent event) {
        storage.getResourceDAO().setError(url, event);
    }

    public void visit(URL url, URLFoundEvent event) {
        URL foundURL = event.getFoundURL();
        URL siteURL = URLUtil.getSiteURL(foundURL);
        Site site = storage.getSiteDAO().find(siteURL);

        boolean newResource = (storage.getResourceDAO().getResource(foundURL) == null);

        if (site == null) {
            site = storage.getSiteDAO().createSite(siteURL);
            context.registerNewSite(site);
            storage.getSiteDAO().save(site);

            eventDispatcher.dispatch(new SiteDiscoveredEvent(site));

            if (site.getFetchRobotsTXT()) {
                if (site.mustHandle()) {
                    URL robotsTXTUrl = URLUtil.getRobotsTXTURL(siteURL);
                    scheduler.schedule(new FetchRobotsTXTTaskImpl(context, robotsTXTUrl, site));
                    if (newResource) {
                        scheduler.block(siteURL, new DecideOnSpideringTask(context, new URLFoundEvent(context, url, foundURL)));
                    }
                }

            } else {
                if (site.mustHandle()) {
                    ((SiteInternal) site).registerRobotsTXTSkipped();
                    context.registerRobotsTXTSkipped(site);
                    eventDispatcher.dispatch(new RobotsTXTSkippedEvent(site));
                    if (newResource) {
                        scheduler.schedule(new DecideOnSpideringTask(context, event));
                    }
                }
                notifyAll();
            }
        } else if (site.isRobotsTXTHandled()) {
            if (newResource) {
                scheduler.schedule(new DecideOnSpideringTask(context, event));
            }
            notifyAll();
        } else {
            if (site.mustHandle()) {
                if (newResource) {
                    scheduler.block(siteURL, new DecideOnSpideringTask(context, new URLFoundEvent(context, url, foundURL)));
                }
            }
        }

        if (newResource) {
            storage.getResourceDAO().registerURL(foundURL);
            if ( !site.mustHandle()) {
                storage.getResourceDAO().setIgnoredForFetching(foundURL, event);
            }
            eventDispatcher.dispatch(new ResourceDiscoveredEvent(storage.getResourceDAO().getResource(foundURL)));
        }
        storage.getResourceDAO().registerURLReference(foundURL, url);
        if (url != null) {
            eventDispatcher.dispatch(new ResourceReferenceDiscoveredEvent(storage.getResourceDAO().getResource(url), storage.getResourceDAO().getResource(foundURL)));
        }

    }

    public void visit(URL url, RobotsTXTSpideredOkEvent event) {
        URL robotsTxtURL = event.getRobotsTXTURL();
        URL siteURL = URLUtil.getSiteURL(robotsTxtURL);
        SiteInternal site = (SiteInternal) storage.getSiteDAO().find(siteURL);

        DecideOnSpideringTask[] tasks = scheduler.unblock(siteURL);
        for (int i = 0; i < tasks.length; i++) {
            scheduler.schedule(tasks[i]);
        }

        storage.getResourceDAO().registerURL(robotsTxtURL);
        storage.getResourceDAO().setSpidered(robotsTxtURL, event);
        storage.getResourceDAO().setIgnoredForParsing(robotsTxtURL);
        Resource resource = storage.getResourceDAO().getResource(robotsTxtURL);
        byte[] bytes = event.getBytes();
        site.registerRobotsTXT();
        eventDispatcher.dispatch(new ResourceDiscoveredEvent(resource));
        eventDispatcher.dispatch(new ResourceFetchedEvent(resource));
        eventDispatcher.dispatch(new RobotsTXTFetchedEvent(site, new String(bytes)));
        context.registerRobotsTXT(site, new ByteArrayInputStream(bytes));
        storage.getSiteDAO().save(site);
    }

    public void visit(URL url, RobotsTXTSpideredErrorEvent event) {
        URL robotsTxtURL = event.getRobotsTXTURL();
        URL siteURL = URLUtil.getSiteURL(robotsTxtURL);
        Site site = storage.getSiteDAO().find(siteURL);
        ((SiteInternal) site).registerRobotsTXTError();

        DecideOnSpideringTask[] tasks = scheduler.unblock(siteURL);
        for (int i = 0; i < tasks.length; i++) {
            scheduler.schedule(tasks[i]);
        }

        storage.getResourceDAO().registerURL(robotsTxtURL);
        storage.getResourceDAO().setError(robotsTxtURL, event);
        eventDispatcher.dispatch(new RobotsTXTFetchErrorEvent(site, event.getException()));
        context.registerRobotsTXTError(site);
        storage.getSiteDAO().save(site);
    }

    public void visit(URL url, RobotsTXTUnexistingEvent event) {
        URL robotsTxtURL = event.getRobotsTXTURL();
        URL siteURL = URLUtil.getSiteURL(robotsTxtURL);
        Site site = storage.getSiteDAO().find(siteURL);
        ((SiteInternal) site).registerNoRobotsTXTFound();

        DecideOnSpideringTask[] tasks = scheduler.unblock(siteURL);
        for (int i = 0; i < tasks.length; i++) {
            scheduler.schedule(tasks[i]);
        }
        storage.getSiteDAO().save(site);
        eventDispatcher.dispatch(new RobotsTXTMissingEvent(site));
    }


}
