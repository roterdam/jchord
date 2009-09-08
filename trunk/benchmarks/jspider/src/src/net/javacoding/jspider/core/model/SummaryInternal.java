package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.Summary;

/**
 * $Id: SummaryInternal.java,v 1.2 2003/03/24 16:58:35 vanrogu Exp $
 */
public class SummaryInternal implements Summary {

    protected int known;
    protected int parsed;
    protected int ignoredForFetching;
    protected int ignoredForParsing;
    protected int fetchErrors;
    protected int parseErrors;
    protected int forbidden;
    protected int unvisited;

    public SummaryInternal ( int known, int parsed, int ignoredForFetching, int ignoredForParsing, int fetchErrors, int parseErrors, int forbidden, int unvisited) {
        this.known = known;
        this.parsed = parsed;
        this.ignoredForFetching = ignoredForFetching;
        this.ignoredForParsing = ignoredForParsing;
        this.fetchErrors = fetchErrors;
        this.parseErrors = parseErrors;
        this.forbidden = forbidden;
        this.unvisited = unvisited;
    }

    public int getKnown() {
        return known;
    }

    public int getVisited() {
        return (getParsed() + getIgnoredForParsing() + getParseErrors());
    }

    public int getNotVisited() {
        return (getKnown() - (getParsed() + getIgnoredForParsing() + getParseErrors()));
    }

    public int getParsed() {
        return parsed;
    }

    public int getIgnoredForFetching() {
        return ignoredForFetching;
    }

    public int getIgnoredForParsing() {
        return ignoredForParsing;
    }

    public int getFetchErrors() {
        return fetchErrors;
    }

    public int getParseErrors() {
        return parseErrors;
    }

    public int getForbidden() {
        return forbidden;
    }

    public int getUnvisited() {
        return unvisited;
    }

}
