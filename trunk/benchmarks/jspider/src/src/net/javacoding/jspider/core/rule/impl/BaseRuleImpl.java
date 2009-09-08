package net.javacoding.jspider.core.rule.impl;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.spi.Rule;

import java.net.URL;


/**
 * Base implemenation of a Rule.  To be subclasses by concrete rules.
 *
 * $Id: BaseRuleImpl.java,v 1.10 2003/04/03 16:24:54 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public abstract class BaseRuleImpl implements Rule {

    /**
     * Returns the name of the rule.
     * @return the name of the rule
     */
    public String getName() {
        return this.getClass().getName();
    }

    public abstract Decision apply(SpiderContext context, Site currentSite, URL url);

}
