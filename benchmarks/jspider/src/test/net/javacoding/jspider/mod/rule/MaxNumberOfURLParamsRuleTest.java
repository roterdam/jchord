package net.javacoding.jspider.mod.rule;

import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;

import java.net.URL;

import junit.framework.TestCase;

/**
 * $Id: MaxNumberOfURLParamsRuleTest.java,v 1.1 2003/04/07 15:51:05 vanrogu Exp $
 */
public class MaxNumberOfURLParamsRuleTest extends TestCase {

    protected OverridingPropertySet config;

    public MaxNumberOfURLParamsRuleTest ( ) {
        super ( "MaxNumberOfURLParamsRuleTest" );
    }

    protected void setUp() throws Exception {
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        config = new OverridingPropertySet ( null );
    }

    public void testNoParams ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net";
        int max = 10;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testQuestionMarkOneAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net?";
        int max = 1;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testFileWithQuestionMarkOneAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?";
        int max = 1;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testQuestionMarkZeroAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net?";
        int max = 0;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testFileWithQuestionMarkZeroAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?";
        int max = 0;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testNoParamsZeroAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net";
        int max = 0;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testSingleParam ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value";
        int max = 10;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testSingleParamOneAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value";
        int max = 1;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testSingleParamZeroAllowed ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value";
        int max = 0;
        int expected = Decision.RULE_IGNORE;

        applyTest ( max, urlString, expected );
    }

    public void testWithParams ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value&param2=value2";
        int max = 10;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testWithParamsOnBoundary ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value&param2=value2";
        int max = 2;
        int expected = Decision.RULE_ACCEPT;

        applyTest ( max, urlString, expected );
    }

    public void testWithParamsViolation ( ) throws Exception {
        String urlString = "http://j-spider.sourceforge.net/index.html?param=value&param2=value2&param3=value3";
        int max = 2;
        int expected = Decision.RULE_IGNORE;

        applyTest ( max, urlString, expected );
    }


    public void applyTest ( int max, String urlString, int expected ) throws Exception {
        config.setValue(MaxNumberOfURLParamsRule.MAX, new Integer(max));
        URL url = new URL ( urlString );
        Rule rule = new MaxNumberOfURLParamsRule(config);
        Decision decision = rule.apply(null, null, url);
        assertEquals("decision not as expected", expected, decision.getDecision());
    }

}
