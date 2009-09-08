package net.javacoding.jspider.mod.rule;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.rule.impl.BaseRuleImpl;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.net.URL;


/**
 * Rule implementation that only accepts a resource URL if it points to a
 * resource that is accessible over HTTP.
 *
 * $Id: OnlyHttpProtocolRule.java,v 1.1 2003/04/03 16:10:52 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class OnlyHttpProtocolRule extends BaseRuleImpl {

    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        Decision decision = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("http")) {
            decision = new DecisionInternal(Decision.RULE_ACCEPT);
        } else {
            decision = new DecisionInternal(Decision.RULE_IGNORE);
        }

        return decision;
    }
}
