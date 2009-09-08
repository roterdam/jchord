package net.javacoding.jspider.api.event.engine;

import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Summary;

/**
 * $Id: SpideringSummaryEvent.java,v 1.3 2003/03/24 16:58:35 vanrogu Exp $
 */
public class SpideringSummaryEvent extends EngineRelatedEvent {

    protected Summary summary;

    public SpideringSummaryEvent ( Summary summary ) {
        this.summary = summary;
    }

    public String getComment() {
        return this.toString();
    }

    public String toString() {
        Summary s = this.summary;
        StringBuffer sb = new StringBuffer();
        sb.append("\nSPIDERING SUMMARY : ");
        sb.append("\nknown urls ............. : " + s.getKnown());
        sb.append("\n");
        sb.append("\n  visited urls ........... : " + s.getVisited());
        sb.append("\n    parsed urls ............ : " + s.getParsed());
        sb.append("\n    parse ignored urls ..... : " + s.getIgnoredForParsing());
        sb.append("\n    parse error urls ....... : " + s.getParseErrors());
        sb.append("\n");
        sb.append("\n  not visited urls ....... : " + s.getNotVisited());
        sb.append("\n    fetching ignored urls .. : " + s.getIgnoredForFetching());
        sb.append("\n    forbidden urls ......... : " + s.getForbidden());
        sb.append("\n    fetch error urls ....... : " + s.getFetchErrors());
        sb.append("\n");
        sb.append("\n  not yet  visited urls .. : " + s.getUnvisited());

        return sb.toString();

    }

    public Summary getSummary ( ) {
        return summary;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

}
