package net.javacoding.jspider.api.event.site;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.model.Site;


/**
 *
 * $Id: SiteRelatedEvent.java,v 1.3 2003/04/07 15:50:47 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public abstract class SiteRelatedEvent extends JSpiderEvent {

    protected Site site;

    public SiteRelatedEvent(Site site) {
        this.site = site;
    }

    public Site getSite() {
        return site;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public String toString ( ) {
        return super.toString() + " " + getComment();
    }
}
