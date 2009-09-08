package net.javacoding.jspider.core.rule.impl;

import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.net.URL;

/**
 * $Id: RobotsTXTErrorRule.java,v 1.2 2003/04/03 15:57:16 vanrogu Exp $
 */
public class RobotsTXTErrorRule extends BaseRuleImpl {
    /**
     * Applies the rule to the given url.
     * @param context the context we're spidering under
     * @param currentSite the site we're currently spidering
     * @param url the url to be evaluated
     * @return Decision object telling whether the url should be accepted.
     * This implementation always returns a reject decision.
     */
    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        return new DecisionInternal(Decision.RULE_IGNORE, "robots.txt fetch resulted in an error - all resources in site are ignored");
    }

}
