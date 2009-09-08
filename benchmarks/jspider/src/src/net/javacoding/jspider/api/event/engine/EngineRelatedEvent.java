package net.javacoding.jspider.api.event.engine;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.event.JSpiderEvent;


/**
 *
 * $Id: EngineRelatedEvent.java,v 1.3 2003/04/07 15:50:45 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public abstract class EngineRelatedEvent extends JSpiderEvent {

    public int getType() {
        return JSpiderEvent.EVENT_TYPE_ENGINE;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public String toString() {
        return super.toString() + " " + getComment();
    }
}
