package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.mockobjects.SimpleSpiderContext;
import net.javacoding.jspider.mod.rule.OnlyDeeperInSiteRule;

import java.net.URL;

/**
 * $Id: OnlyDeeperInSiteRuleTest.java,v 1.4 2003/04/11 16:37:10 vanrogu Exp $
 */
public class OnlyDeeperInSiteRuleTest extends TestCase {

    protected Rule rule;
    protected SpiderContext context;
    protected Site site;

    public OnlyDeeperInSiteRuleTest ( ) {
        super ( "OnlyDeeperInSiteRuleTest" );
    }

    protected void setUp() throws Exception {
        rule = new OnlyDeeperInSiteRule ( );
        URL baseURL = new URL("http://j-spider.sourceforge.net/folder/subfolder/index.html");
        URL siteURL = new URL("http://j-spider.sourceforge.net");
        site = new SiteInternal(0, null, siteURL);
        context = new SimpleSpiderContext(baseURL);
    }

    public void testEqualURL ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/folder/subfolder/index.html");
        Decision decision = rule.apply(context, site, url );

        boolean accepted = decision.getDecision() == Decision.RULE_ACCEPT;

        assertTrue("url that should be accepted not accepterd", accepted);
    }

    public void testDeeperURL ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/folder/subfolder/anothersubfolder/index.html");
        Decision decision = rule.apply(context, site, url );

        boolean accepted = decision.getDecision() == Decision.RULE_ACCEPT;

        assertTrue("url that should be accepted not accepterd", accepted);
    }

    public void testHigherURL ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/folder/index.html");
        Decision decision = rule.apply(context, site, url );

        boolean forbidden = decision.getDecision() == Decision.RULE_FORBIDDEN;

        assertTrue("url that should be forbidden not forbidden", forbidden);
    }

    public void testSameLevelURL ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/folder/subfolder/anotherresource.html");
        Decision decision = rule.apply(context, site, url );

        boolean accepted = decision.getDecision() == Decision.RULE_ACCEPT;

        assertTrue("url that should be accepted not accepted", accepted);
    }

}
