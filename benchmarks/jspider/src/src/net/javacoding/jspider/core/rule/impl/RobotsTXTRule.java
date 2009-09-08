package net.javacoding.jspider.core.rule.impl;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.DecisionInternal;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.util.URLUtil;
import net.javacoding.jspider.core.util.html.RobotsTXTLine;
import net.javacoding.jspider.core.util.html.RobotsTXTLineSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * Rule implementation that applies the rules expressed by a site's robots.txt
 * file to the resources we want to fetch on that site.
 * This file allows webmasters to exclude certain resources and folders not to
 * be spidered by web robots, to disallow inclusion in search engines, etc ...
 *
 * $Id: RobotsTXTRule.java,v 1.13 2003/03/28 17:26:28 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RobotsTXTRule extends BaseRuleImpl {

    /** user agent under which we're operating. */
    protected String effectiveUserAgent;

    /** user agent in the robots.txt file we're obeying */
    protected String obeyedUserAgent;

    /** all lines in the robots.txt file that apply to us and forbid access to a part of the site. */
    protected RobotsTXTLine[] forbiddenPaths;


    /**
     * Public constructor.
     * @param userAgent the userAgent under which we're operating
     * @param is the inputstream to read the robots.txt file from
     * @throws IOException in case something goes wrong reading the robots.txt
     */
    public RobotsTXTRule(String userAgent, InputStream is) throws IOException {
        RobotsTXTLineSet lineSet = RobotsTXTLineSet.findLineSet(is, userAgent);
        this.effectiveUserAgent = userAgent;
        if (lineSet == null) {
            this.obeyedUserAgent = null;
            forbiddenPaths = new RobotsTXTLine[0];
        } else {
            this.obeyedUserAgent = lineSet.getUserAgent();
            forbiddenPaths = lineSet.getLines();
        }
    }

    /**
     * Returns the user agent from robots.txt we're obeying. (can be '*').
     * This user agent identification is the first match we encountered in the file,
     * a match is given if our effective user agent contains the user agent
     * identification as a substring in a case-insensitive way.
     * @return the useragent selector we're obeying.
     */
    public String getObeyedUserAgent() {
        return obeyedUserAgent;
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

        if ((context.getStorage().getSiteDAO().find(URLUtil.getSiteURL(url))).getObeyRobotsTXT()) {

            for (int i = 0; i < forbiddenPaths.length; i++) {
                RobotsTXTLine forbiddenPath = forbiddenPaths[i];
                if (forbiddenPath.matches(url)) {
                    decision = new DecisionInternal(Decision.RULE_FORBIDDEN, "access to '" + path + "' forbidden");
                    break;
                }
            }
        }
        return decision;
    }

}
