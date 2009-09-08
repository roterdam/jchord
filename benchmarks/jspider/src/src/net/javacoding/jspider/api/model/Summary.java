package net.javacoding.jspider.api.model;

/**
 * $Id: Summary.java,v 1.3 2003/03/24 16:58:35 vanrogu Exp $
 */
public interface Summary {

    public int getKnown();

    public int getVisited ( );

    public int getNotVisited ( );

    public int getParsed();

    public int getIgnoredForFetching();

    public int getIgnoredForParsing();

    public int getFetchErrors();

    public int getParseErrors();

    public int getForbidden();

    public int getUnvisited();

}
