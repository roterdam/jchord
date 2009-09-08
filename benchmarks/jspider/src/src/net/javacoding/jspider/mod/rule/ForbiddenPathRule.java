package net.javacoding.jspider.mod.rule;

import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.rule.impl.BaseRuleImpl;
import net.javacoding.jspider.core.model.DecisionInternal;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;

import java.net.URL;

/**
 * $Id: ForbiddenPathRule.java,v 1.1 2003/04/03 16:10:51 vanrogu Exp $
 */
public class ForbiddenPathRule extends BaseRuleImpl {

    public static final String PATH = "path";

    protected String forbiddenPath;


    public ForbiddenPathRule(PropertySet config) {
        forbiddenPath = config.getString(PATH, "/");
    }

    /**
     * Applies the rule to a given URL
     * @param context the spider context we're working in
     * @param currentSite the site we're spidering
     * @param url the url of the resource to be tested for spider permission
     * @return Decision object expressing this rule's decision on the resource
     */
    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        String path = url.getPath();
        Decision decision = new DecisionInternal();

        if (matches(url, forbiddenPath)) {
            decision = new DecisionInternal(Decision.RULE_FORBIDDEN, "access to '" + path + "' forbidden");
        }
        return decision;
    }

    public boolean matches(URL url, String forbiddenPath) {
        String path = url.getPath();
        if (path.length() == 0 && forbiddenPath.equals("/")) {
            return true;
        } else {
            return url.getPath().startsWith(forbiddenPath);
        }
    }

}