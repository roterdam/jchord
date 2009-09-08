package net.javacoding.jspider.functional.specific.fetch;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.functional.TestingConstants;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;

import java.net.URL;

/**
 * $Id: FetchTest.java,v 1.8 2003/04/10 16:19:21 vanrogu Exp $
 */
public class FetchTest extends TestCase {

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;

    /**
     * Public constructor giving a name to the test.
     */
    public FetchTest ( ) {
        super ( "FetchTest ");
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
     * Test a simple fetch.
     */
    public void testSimpleFetch ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/testSimpleFetch.html" );

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
    }

    public void testFetchUnexisting ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/unexisting.html" );

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testFetchRedirect  ( ) throws Exception {
        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/testFetchRedirect.php" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,3);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,3);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,2);
    }

    public void testFetch500 ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/testFetch500.php" );

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testFetch404 ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/testFetch404.php" );

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testFetchNullSizeResource ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/fetch/testFetchNullSizeResource.html" );

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
    }

    protected void testEventCount ( Class eventClass, int expectedCount  ) {
        assertEquals(eventClass.getName(), expectedCount, sink.getEventCount(eventClass));
    }

}

