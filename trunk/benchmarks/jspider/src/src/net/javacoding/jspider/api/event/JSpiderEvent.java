package net.javacoding.jspider.api.event;


import java.util.Date;


/**
 * Base class of all JSpider API events.  All events that will be dispatched
 * towards JSpider modules will extends directly or indirectly from this
 * class.
 *
 * $Id: JSpiderEvent.java,v 1.5 2003/03/23 15:44:48 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public abstract class JSpiderEvent implements EventVisitable {

    public static final String EVENT_PACKAGE = "net.javacoding.jspider.api.event.";

    /** Event type used for events related to the JSpider engine. */
    public static final int EVENT_TYPE_ENGINE = 1;

    /** Event type used for events related to monitoring. */
    public static final int EVENT_TYPE_MONITORING = 2;

    /** Event type used for real spider events. */
    public static final int EVENT_TYPE_SPIDER = 3;

    /** Timestamp of the event. */
    protected Date date;


    /**
     * Public constructor.
     */
    public JSpiderEvent() {
        date = new Date();
    }

    /**
     * Returns the name of the event
     * @return name of the event
     */
    public String getName() {
        return getClass().getName().substring(EVENT_PACKAGE.length());
    }

    /**
     * Returns a Date object containing the event's timestamp
     * @return Date object containing the event's timestamp
     */
    public Date getRaisedDate() {
        return date;
    }

    /**
     * Returns optional comments on the event.
     * @return comment on the event.
     */
    public abstract String getComment();

    /**
     * Returns whether this event describes some sort of error situation.
     * @return boolean that is true if this event describes an error
     */
    public boolean isError ( ) {
        return false;
    }

    /**
     * Returns whether this event can be filtered out.  Only the most critical
     * events will return FALSE.
     * @return whether this event can be filtered out
     */
    public boolean isFilterable ( ) {
        return true;
    }

    /**
     * Returns the type of the event.
     * @return the type of the Event - Engine / Monitoring / Spider
     */
    public int getType ( ) {
      // By default, it will be a normal spider event.
      return JSpiderEvent.EVENT_TYPE_SPIDER;
    }

    /**
     * Accept method for the Visitor-pattern that's used for handling events.
     * @param visitor the visitor instance that wants to visit the event
     */
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Object's overridden toString() method.
     * @return String representation of the event
     */
    public String toString() {
        return this.getClass().getName();
    }
}
