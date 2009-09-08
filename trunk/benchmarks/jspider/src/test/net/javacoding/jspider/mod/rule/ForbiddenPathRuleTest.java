package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.api.model.Decision;

import java.net.URL;

/**
 * $Id: ForbiddenPathRuleTest.java,v 1.1 2003/04/07 15:51:05 vanrogu Exp $
 */
public class ForbiddenPathRuleTest extends TestCase {

    OverridingPropertySet config;

    public ForbiddenPathRuleTest ( ) {
        super ( "ForbiddenPathRuleTest" );
    }

    protected void setUp() throws Exception {
        config = new OverridingPropertySet(null);
    }

    public void testSimple ( ) throws Exception {
        String path="/forbidden";
        String urlString="http://j-spider.sourceforge.net/test/index.html";
        int expected = Decision.RULE_DONTCARE;
        applyTest(path, urlString, expected);
    }

    public void testForbidden ( ) throws Exception {
        String path="/forbidden";
        String urlString="http://j-spider.sourceforge.net/forbidden/index.html";
        int expected = Decision.RULE_FORBIDDEN;
        applyTest(path, urlString, expected);
    }

    public void testForbiddenRoot ( ) throws Exception {
        String path="/";
        String urlString="http://j-spider.sourceforge.net/forbidden/index.html";
        int expected = Decision.RULE_FORBIDDEN;
        applyTest(path, urlString, expected);
    }

    public void testForbiddenRootHomePage ( ) throws Exception {
        String path="/";
        String urlString="http://j-spider.sourceforge.net";
        int expected = Decision.RULE_FORBIDDEN;
        applyTest(path, urlString, expected);
    }

    public void testForbiddenRootHomePageWithSlash ( ) throws Exception {
        String path="/";
        String urlString="http://j-spider.sourceforge.net/";
        int expected = Decision.RULE_FORBIDDEN;
        applyTest(path, urlString, expected);
    }

    public void applyTest ( String path, String urlString, int expected ) throws Exception {
        URL url = new URL(urlString);
        config.setValue(ForbiddenPathRule.PATH, path);
        Rule rule = new ForbiddenPathRule(config) ;
        Decision decision = rule.apply(null, null, url);
        assertEquals("wrong decision taken on url", expected, decision.getDecision() );
    }

}
