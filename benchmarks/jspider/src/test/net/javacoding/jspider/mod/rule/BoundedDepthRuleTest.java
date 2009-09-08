package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.api.model.Decision;

import java.net.URL;

/**
 * $Id: BoundedDepthRuleTest.java,v 1.1 2003/04/07 15:51:05 vanrogu Exp $
 */
public class BoundedDepthRuleTest extends TestCase {

    OverridingPropertySet config;

    public BoundedDepthRuleTest ( ) {
        super ( "BoundedDepthRuleTest" );
    }

    protected void setUp() throws Exception {
        config = new OverridingPropertySet ( null );
    }

    public void testSimple ( ) throws Exception {
        int min = 0;
        int max = 0;
        String urlString = "http://j-spider.sourceforge.net/test.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testMaxOK ( ) throws Exception {
        int min = 0;
        int max = 3;
        String urlString = "http://j-spider.sourceforge.net/test/abc/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testMaxOnBoundary ( ) throws Exception {
        int min = 0;
        int max = 2;
        String urlString = "http://j-spider.sourceforge.net/test/abc/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testMaxError ( ) throws Exception {
        int min = 0;
        int max = 1;
        String urlString = "http://j-spider.sourceforge.net/test/abc/index.html";
        int expected = Decision.RULE_IGNORE;

        applyTest(min,max,urlString, expected);
    }

    public void testMinOK ( ) throws Exception {
        int min = 2;
        int max = 999;
        String urlString = "http://j-spider.sourceforge.net/test/abc/def/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testMinOnBoundary ( ) throws Exception {
        int min = 3;
        int max = 999;
        String urlString = "http://j-spider.sourceforge.net/test/abc/def/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testBothOnBoundary ( ) throws Exception {
        int min = 3;
        int max = 3;
        String urlString = "http://j-spider.sourceforge.net/test/abc/def/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(min,max,urlString, expected);
    }

    public void testMinError ( ) throws Exception {
        int min = 4;
        int max = 999;
        String urlString = "http://j-spider.sourceforge.net/test/abc/def/index.html";
        int expected = Decision.RULE_IGNORE;

        applyTest(min,max,urlString, expected);
    }

    public void applyTest ( int min, int max, String urlString, int expected ) throws Exception {
        URL url = new URL(urlString);
        config.setValue(BoundedDepthRule.MIN_DEPTH, new Integer(min));
        config.setValue(BoundedDepthRule.MAX_DEPTH, new Integer(max));
        Rule rule = new BoundedDepthRule(config);
        Decision decision = rule.apply(null, null, url);
        assertEquals("wrong decision taken on url", expected, decision.getDecision() );
    }

}
