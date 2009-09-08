package net.javacoding.jspider.core.impl;


import net.javacoding.jspider.api.event.engine.*;
import net.javacoding.jspider.core.Spider;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.task.dispatch.DispatchSpiderTasks;
import net.javacoding.jspider.core.task.dispatch.DispatchThinkerTasks;
import net.javacoding.jspider.core.threading.ThreadPoolMonitorThread;
import net.javacoding.jspider.core.threading.WorkerThreadPool;
import net.javacoding.jspider.core.util.config.*;


/**
 *
 * $Id: SpiderImpl.java,v 1.18 2003/04/02 20:55:06 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SpiderImpl implements Spider {

    public static final int DEFAULT_MONITORING_INTERVAL = 1000;

    protected WorkerThreadPool spiders;
    protected WorkerThreadPool thinkers;

    public SpiderImpl(SpiderContext context, int spiderThreads, int thinkerThreads) {
        LogFactory.getLog(Spider.class).info("Spider born - threads: spiders: " + spiderThreads + ", thinkers: " + thinkerThreads);
        spiders = new WorkerThreadPool("Spiders", "Spider", spiderThreads);
        thinkers = new WorkerThreadPool("Thinkers", "Thinker", thinkerThreads);

        PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
        PropertySet threadProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING, props);
        PropertySet spidersProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_SPIDERS, threadProps);
        PropertySet thinkerProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_THINKERS, threadProps);
        PropertySet spidersMonitoringProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_MONITORING, spidersProps);
        PropertySet thinkerMonitoringProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_MONITORING, thinkerProps);

        if (spidersMonitoringProps.getBoolean(ConfigConstants.CONFIG_THREADING_MONITORING_ENABLED, false)) {
            int interval = spidersMonitoringProps.getInteger(ConfigConstants.CONFIG_THREADING_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL);
            new ThreadPoolMonitorThread(context.getEventDispatcher(), interval, spiders);
        }
        if (thinkerMonitoringProps.getBoolean(ConfigConstants.CONFIG_THREADING_MONITORING_ENABLED, false)) {
            int interval = thinkerMonitoringProps.getInteger(ConfigConstants.CONFIG_THREADING_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL);
            new ThreadPoolMonitorThread(context.getEventDispatcher(), interval, thinkers);
        }
    }

    public void crawl(SpiderContext context) {

        long start = System.currentTimeMillis();

        context.getEventDispatcher().dispatch(new SpideringStartedEvent(context.getBaseURL()));

        DispatchSpiderTasks dispatchSpiderTask = new DispatchSpiderTasks(spiders, context);
        DispatchThinkerTasks dispatchThinkerTask = new DispatchThinkerTasks(thinkers, context);

        synchronized (dispatchSpiderTask) {

            context.getAgent().start();

            spiders.assignGroupTask(dispatchSpiderTask);
            thinkers.assignGroupTask(dispatchThinkerTask);

            try {
                // now wait for the spidering to be ended.
                dispatchSpiderTask.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Log log = LogFactory.getLog(Spider.class);
        log.debug("Stopping spider workers...");
        spiders.stopAll();
        log.info("Stopped spider workers...");
        log.debug("Stopping thinker workers...");
        thinkers.stopAll();
        log.info("Stopped thinker workers...");

        context.getEventDispatcher().dispatch(new SpideringSummaryEvent(context.getStorage().getSummary()));
        context.getEventDispatcher().dispatch(new SpideringStoppedEvent(context.getStorage()));

        context.getEventDispatcher().shutdown();

        log.info("Spidering done!");
        log.info("Elapsed time : " + (System.currentTimeMillis() - start));
    }

}
