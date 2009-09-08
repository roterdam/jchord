/**
 * $Id: BaseSiteOnlyRuleTest.java,v 1.4 2003/04/11 16:37:09 vanrogu Exp $
 */
package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.mockobjects.SimpleSpiderContext;
import net.javacoding.jspider.mod.rule.BaseSiteOnlyRule;

import java.net.URL;

public class BaseSiteOnlyRuleTest extends TestCase {

    protected Rule rule;
    protected SpiderContext context;
    protected Site jspiderSite;
    protected Site otherSite;

    public BaseSiteOnlyRuleTest ( ) {
        super ( "BaseSiteOnlyRuleTest" );
    }

    protected void setUp() throws Exception {
        rule = new BaseSiteOnlyRule();
        URL jspiderUrl = new URL ( "http://j-spider.sourceforge.net");
        jspiderSite = new SiteInternal(0, null, jspiderUrl);
        URL otherUrl = new URL ( "http://www.javacoding.net");
        otherSite = new SiteInternal(0, null, otherUrl);
        context = new SimpleSpiderContext(jspiderUrl);
    }

    public void testBaseSite ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/some/doc.html");
        Decision decision = rule.apply(context, jspiderSite, url);
        assertEquals("resource within same site not accepted", Decision.RULE_ACCEPT, decision.getDecision());
    }

    public void testBaseSiteOtherContext ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/some/doc.html");
        Decision decision = rule.apply(context, otherSite, url);
        assertEquals("resource within same site not accepted", Decision.RULE_ACCEPT, decision.getDecision());
    }

    public void testOtherSite ( ) throws Exception {
        URL url = new URL ( "http://www.javacoding.net/some/doc.html");
        Decision decision = rule.apply(context, jspiderSite, url);
        assertEquals("resource within same site not ignored", Decision.RULE_IGNORE, decision.getDecision());
    }

    public void testOtherSiteOtherContext ( ) throws Exception {
        URL url = new URL ( "http://www.javacoding.net/some/doc.html");
        Decision decision = rule.apply(context, jspiderSite, url);
        assertEquals("resource within same site not ignored", Decision.RULE_IGNORE, decision.getDecision());
    }

}
