package net.javacoding.jspider.functional.specific.parse;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.functional.TestingConstants;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;

import java.net.URL;

/**
 * $Id: ParseTest.java,v 1.7 2003/04/11 16:37:09 vanrogu Exp $
 */
public class ParseTest extends TestCase {

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;

    /**
     * Public constructor giving a name to the test.
     */
    public ParseTest ( ) {
        super ( "ParseTest ");
    }

    /**
     * JUnit's overridden setUp method
     * @throws java.lang.Exception in case something fails during setup
     */
    protected void setUp() throws Exception {
        System.err.println("setUp");
        config = ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        sink = JUnitEventSink.getInstance();
    }

    /**
     * JUnit's overridden tearDown method
     * @throws java.lang.Exception in case something fails during tearDown
     */
    protected void tearDown() throws Exception {
        System.err.println("tearDown");
        ConfigurationFactory.cleanConfiguration();
        sink.reset();
    }

    /**
     * Test a simple parse.
     */
    public void testSimpleParse ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testSimpleParse.html" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,6);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,1);
    }

    public void testSimpleParseWithEMailAddresses ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testSimpleParseWithEMailAddresses.html" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.resource.EMailAddressDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.EMailAddressReferenceDiscoveredEvent.class,3);
    }

    /**
     * Tests parsing of malformed "a href" constructs.
     */
    public void testMalformedParse ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testMalformedParse.html" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,11);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,1);
    }

    protected void testEventCount ( Class eventClass, int expectedCount  ) {
        assertEquals(eventClass.getName(), expectedCount, sink.getEventCount(eventClass));
    }

}
