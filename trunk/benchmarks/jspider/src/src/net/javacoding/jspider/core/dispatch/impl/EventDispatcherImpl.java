package net.javacoding.jspider.core.dispatch.impl;


import net.javacoding.jspider.api.event.EventSink;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.util.config.ConfigConstants;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.spi.EventFilter;
import net.javacoding.jspider.mod.eventfilter.AllowAllEventFilter;


/**
 *
 * $Id: EventDispatcherImpl.java,v 1.11 2003/04/22 16:43:33 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class EventDispatcherImpl implements EventDispatcher {

    protected String name;
    protected boolean filter;

    protected EventSink[] eventSinks;
    protected EventFilter engineEventFilter;
    protected EventFilter monitorEventFilter;
    protected EventFilter spiderEventFilter;

    protected Log log;

    public void initialize() {
        log.debug(name + " intializing...");
        for (int i = 0; i < eventSinks.length; i++) {
            EventSink eventSink = eventSinks[i];
            eventSink.initialize();
        }
        log.debug(name + " intialized.");
    }

    public void shutdown() {
        log.debug(name + " shutting down.");
        for (int i = 0; i < eventSinks.length; i++) {
            EventSink eventSink = eventSinks[i];
            eventSink.shutdown();
        }
        log.debug(name + " shutdown.");
    }

    public EventDispatcherImpl(String name, EventSink[] eventSinks, PropertySet props) {
        log = LogFactory.getLog(EventDispatcher.class);
        log.debug(name + " configuring...");
        this.name = name;
        this.filter = props.getBoolean(ConfigConstants.FILTER_ENABLED, true);
        this.eventSinks = eventSinks;

        if (filter) {

            Class engineEventFilterClass = props.getClass(ConfigConstants.FILTER_ENGINE, AllowAllEventFilter.class);
            Class monitorEventFilterClass = props.getClass(ConfigConstants.FILTER_MONITORING, AllowAllEventFilter.class);
            Class spiderEventFilterClass = props.getClass(ConfigConstants.FILTER_SPIDER, AllowAllEventFilter.class);
            log.debug("EventFilter for engine events = " + engineEventFilterClass.getName());
            log.debug("EventFilter for monitor events = " + monitorEventFilterClass.getName());
            log.debug("EventFilter for spider events = " + spiderEventFilterClass.getName());
            try {
                engineEventFilter = (EventFilter) engineEventFilterClass.newInstance();
                monitorEventFilter = (EventFilter) monitorEventFilterClass.newInstance();
                spiderEventFilter = (EventFilter) spiderEventFilterClass.newInstance();
            } catch (InstantiationException e) {
                log.error("InstantiationException on EventFilter", e);
            } catch (IllegalAccessException e) {
                log.error("IllegalAccessException on instantiation of EventFilter", e);
            }
        } else {
            log.info("Global event filtering is DISABLED");
        }
        log.debug("EventDispatcher " + name + " configured.");
    }

    public void dispatch(JSpiderEvent event) {
        boolean mustDispatch = false;
        if (filter) {
            EventFilter eventFilter = spiderEventFilter;
            if (event.isFilterable()) {
                switch (event.getType()) {
                    case JSpiderEvent.EVENT_TYPE_ENGINE:
                        eventFilter = engineEventFilter;
                        break;
                    case JSpiderEvent.EVENT_TYPE_MONITORING:
                        eventFilter = monitorEventFilter;
                        break;
                    case JSpiderEvent.EVENT_TYPE_SPIDER:
                        eventFilter = spiderEventFilter;
                        break;
                    default:
                        eventFilter = spiderEventFilter;
                }
                if (eventFilter.filterEvent(event)) {
                    mustDispatch = true;
                }
            } else {
                mustDispatch = true;
            }
        } else {
            mustDispatch = true;
        }
        if (mustDispatch) {
            for (int i = 0; i < eventSinks.length; i++) {
                EventSink sink = eventSinks[i];
                sink.notify(event);
            }
        }
    }

}
