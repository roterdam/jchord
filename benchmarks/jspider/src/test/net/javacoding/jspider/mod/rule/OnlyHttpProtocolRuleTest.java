/**
 * $Id: OnlyHttpProtocolRuleTest.java,v 1.4 2003/04/11 16:37:10 vanrogu Exp $
 */
package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.mockobjects.SimpleSpiderContext;
import net.javacoding.jspider.mod.rule.OnlyHttpProtocolRule;

import java.net.URL;

public class OnlyHttpProtocolRuleTest extends TestCase {

    protected Rule rule;
    protected SpiderContext context;
    protected Site jspiderSite;

    public OnlyHttpProtocolRuleTest ( ) {
        super ( "OnlyHttpProtocolRuleTest");
    }

    protected void setUp() throws Exception {
        rule = new OnlyHttpProtocolRule();
        context = new SimpleSpiderContext();
        URL jspiderUrl = new URL ( "http://j-spider.sourceforge.net");
        jspiderSite = new SiteInternal(0, null, jspiderUrl);
    }

    public void testHttpURL ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net");
        Decision decision = rule.apply(context, jspiderSite, url);
        assertEquals("http protocol not accepted", Decision.RULE_ACCEPT, decision.getDecision());
    }

    public void testFtpURL ( ) throws Exception {
        URL url = new URL ( "ftp://ftp.sourceforge.net");
        Decision decision = rule.apply(context, jspiderSite, url);
        assertEquals("ftp protocol not ignored", Decision.RULE_IGNORE , decision.getDecision());
    }

}
