package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.api.model.Decision;

import java.net.URL;

/**
 * $Id: NoURLParamsRuleTest.java,v 1.1 2003/04/07 15:51:06 vanrogu Exp $
 */
public class NoURLParamsRuleTest extends TestCase {

    protected Rule rule;

    public NoURLParamsRuleTest ( ) {
      super ( "NoURLParamsRuleTest" );
    }

    protected void setUp() throws Exception {
        rule = new NoURLParamsRule();
    }

    public void testNoParams ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void testRootNoParams ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void testRootNoParamsWithSlash ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void testSingleParam ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value";
        int expected = Decision.RULE_IGNORE;

        applyTest ( urlString, expected);
    }

    public void testDoubleParam ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value&param2=value2";
        int expected = Decision.RULE_IGNORE;

        applyTest ( urlString, expected);
    }

    public void testQuestionMarkOnly ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void testQuestionMarkOnlyOnFolder ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/test?";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void testQuestionMarkOnlyOnFolderWithSlash ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/test/?";
        int expected = Decision.RULE_ACCEPT;

        applyTest ( urlString, expected);
    }

    public void applyTest ( String urlString, int expected ) throws Exception {
        URL url = new URL(urlString);
        Decision decision = rule.apply(null,null,url);
        assertEquals("decision not as expected", expected, decision.getDecision());
    }

}
