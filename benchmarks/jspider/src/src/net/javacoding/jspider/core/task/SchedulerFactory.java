/**
 * $Id: SchedulerFactory.java,v 1.7 2003/04/02 20:55:17 vanrogu Exp $
 */
package net.javacoding.jspider.core.task;

import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.task.impl.DefaultSchedulerProvider;
import net.javacoding.jspider.core.task.impl.SchedulerMonitorThread;
import net.javacoding.jspider.core.util.config.*;


public class SchedulerFactory {

    public static final int DEFAULT_MONITORING_INTERVAL = 1000;

    public Scheduler createScheduler(SpiderContext context) {

        PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
        PropertySet schedulerProps = new MappedPropertySet ( ConfigConstants.CONFIG_SCHEDULER, props);
        Class providerClass = schedulerProps.getClass(ConfigConstants.CONFIG_SCHEDULER_PROVIDER, DefaultSchedulerProvider.class);
        Log log = LogFactory.getLog(SchedulerFactory.class);
        log.info("TaskScheduler provider class is '" + providerClass + "'");

        try {
            SchedulerProvider provider = (SchedulerProvider) providerClass.newInstance();
            Scheduler scheduler = provider.createScheduler();
            PropertySet monitoringProps = new MappedPropertySet(ConfigConstants.CONFIG_SCHEDULER_MONITORING, schedulerProps);
            if ( monitoringProps.getBoolean(ConfigConstants.CONFIG_SCHEDULER_MONITORING_ENABLED, false ) ) {
              int interval = monitoringProps.getInteger(ConfigConstants.CONFIG_SCHEDULER_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL);
              new SchedulerMonitorThread ( scheduler, context.getEventDispatcher(), interval );
            }
            return scheduler;
        } catch (InstantiationException e) {
            log.error("InstantiationException on Scheduler", e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException on Scheduler", e);
            return null;
        }
    }
}
