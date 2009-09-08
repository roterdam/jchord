package net.javacoding.jspider.spi;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;

import java.net.URL;


/**
 * Interface to be implemented upon each class that will act as a Decision-
 * taking class.
 * A group of Rule objects will together decide whether a certain URL is
 * eligible for spidering and/or parsing in a given context.
 *
 * $Id: Rule.java,v 1.1 2003/04/03 16:25:22 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Rule {

    /**
     * Returns the name of the rule.
     * @return the name of the rule
     */
    public String getName ( );

    /**
     * Applies the rule to the given URL.
     * @param context the context we're spidering in
     * @param currentSite the site we're currently spidering
     * @param url the URL to be evaluated
     * @return Decision object telling whether the URL is accepted for a
     * specific purpose.
     */
    public Decision apply(SpiderContext context, Site currentSite, URL url);

}
