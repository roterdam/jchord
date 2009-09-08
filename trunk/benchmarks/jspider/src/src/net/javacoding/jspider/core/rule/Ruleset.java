package net.javacoding.jspider.core.rule;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.spi.Rule;

import java.net.URL;


/**
 * Interface of a RuleSet, a set of Rules that will be applied as a group
 * on URLs.
 *
 * $Id: Ruleset.java,v 1.8 2003/04/03 16:24:53 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Ruleset {

    /** A general ruleset applied to all urls in the system. */
    public static final int RULESET_GENERAL = 1;

    /** A site-specific ruleset, only applied to urls of that site. */
    public static final int RULESET_SITE = 2;

    /**
     * Applies the ruleset to the given URL.
     * @param context context we're spidering under
     * @param url the url to decide on
     * @param currentSite the site currently being spidered
     * @return Decision object expressing the ruleset's decision about the
     * given url in the given context.
     */
    public Decision applyRules(SpiderContext context, Site currentSite, URL url);

    /**
     * Adds a rule to the ruleset.
     * @param rule rule to be added
     */
    public void addRule(Rule rule);

    /**
     * Returns the type of the ruleset - GENERAL or SITE.
     * @return type of the ruleset
     */
    public int getType ( );

}
