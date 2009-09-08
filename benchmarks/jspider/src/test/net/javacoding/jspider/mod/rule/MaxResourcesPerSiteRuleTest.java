package net.javacoding.jspider.mod.rule;

import junit.framework.TestCase;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;

import java.net.URL;

/**
 * $Id: MaxResourcesPerSiteRuleTest.java,v 1.1 2003/04/07 15:51:06 vanrogu Exp $
 */
public class MaxResourcesPerSiteRuleTest extends TestCase {

    protected OverridingPropertySet config;

    public MaxResourcesPerSiteRuleTest ( ) {
        super ( "MaxResourcesPerSiteRuleTest" );
    }

    protected void setUp() throws Exception {
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        config = new OverridingPropertySet ( null );
    }

    public void testSimple ( ) throws Exception {
        int max = 1;
        int times = 1;
        String urlString = "http://j-spider.sourceforge.net/test.html";
        int expected = Decision.RULE_ACCEPT;

        applyTest(max, urlString, times, expected );
    }

    public void testSimpleViolation ( ) throws Exception {
        int max = 1;
        int times = 2;
        String urlString = "http://j-spider.sourceforge.net/test.html";
        int expected = Decision.RULE_IGNORE;

        applyTest(max, urlString, times, expected );
    }

    public void testZeroAllowed ( ) throws Exception {
        int max = 0;
        int times = 1;
        String urlString = "http://j-spider.sourceforge.net/test.html";
        int expected = Decision.RULE_IGNORE;

        applyTest(max, urlString, times, expected );
    }

    public void testTwoSites ( ) throws Exception {
        URL url1 = new URL("http://j-spider.sourceforge.net/index.html");
        URL url2 = new URL("http://www.somehost.com/index.html");
        URL url3 = new URL("http://j-spider.sourceforge.net/test.html");

        int max = 3;

        config.setValue(MaxResourcesPerSiteRule.MAX, new Integer(max));
        Rule rule = new MaxResourcesPerSiteRule(config) ;
        Decision decision = null;
        decision = rule.apply(null, null, url1);  // site1:1 site2:0
        assertEquals("decision not as expected", Decision.RULE_ACCEPT, decision.getDecision());
        decision = rule.apply(null, null, url2);  // site1:1 site2:1
        assertEquals("decision not as expected", Decision.RULE_ACCEPT, decision.getDecision());
        decision = rule.apply(null, null, url3);  // site1:2 site2:1
        assertEquals("decision not as expected", Decision.RULE_ACCEPT, decision.getDecision());
        decision = rule.apply(null, null, url1);  // site1:3 site2:1
        assertEquals("decision not as expected", Decision.RULE_ACCEPT, decision.getDecision());
        decision = rule.apply(null, null, url2);  // site1:3 site2:2
        assertEquals("decision not as expected", Decision.RULE_ACCEPT, decision.getDecision());
        decision = rule.apply(null, null, url3);  // site1 -- violation
        assertEquals("decision not as expected", Decision.RULE_IGNORE, decision.getDecision());
    }

    public void applyTest ( int max, String urlString, int times, int lastExpected ) throws Exception {
        config.setValue(MaxResourcesPerSiteRule.MAX, new Integer(max));

        URL url = new URL(urlString);
        Rule rule = new MaxResourcesPerSiteRule(config) ;
        Decision decision = null;
        for ( int i = 0; i < times; i++ ) {
            decision = rule.apply(null, null, url);
        }
        assertEquals("final decision not as expected", lastExpected, decision.getDecision());
    }

}
