package net.javacoding.jspider.mod.rule;

import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.rule.impl.BaseRuleImpl;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.net.URL;

/**
 * $Id: BaseURLOnlyRule.java,v 1.1 2003/04/03 16:10:49 vanrogu Exp $
 *
 * @author Günther Van Roey.
 */
public class BaseURLOnlyRule extends BaseRuleImpl {

    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        boolean equals = context.getBaseURL().equals(url);
        if (equals) {
            return new DecisionInternal(Decision.RULE_ACCEPT, "url accepted");
        } else {
            return new DecisionInternal(Decision.RULE_IGNORE, "url ignored because it is not the base url");
        }
    }

}

