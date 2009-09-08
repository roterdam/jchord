package net.javacoding.jspider.mod.rule;

import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.rule.impl.BaseRuleImpl;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.net.URL;

/**
 * Rule implementation that accepts all given URLs.
 *
 * $Id: AcceptAllRule.java,v 1.1 2003/04/03 16:10:48 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class AcceptAllRule extends BaseRuleImpl {

    /**
     * Applies the rule to the given url.
     * @param context the context we're spidering under
     * @param currentSite the site we're currently spidering
     * @param url the url to be evaluated
     * @return Decision object telling whether the url should be accepted.
     * This implementation always returns an accept decision.
     */
    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        return new DecisionInternal(Decision.RULE_ACCEPT, "accept all rule - so resource is accepted");
    }

}
