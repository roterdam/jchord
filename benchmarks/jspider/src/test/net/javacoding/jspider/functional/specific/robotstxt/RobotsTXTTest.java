package net.javacoding.jspider.functional.specific.robotstxt;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.functional.TestingConstants;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.mockobjects.OverridingJSpiderConfiguration;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;

import java.net.URL;

/**
 * $Id: RobotsTXTTest.java,v 1.4 2003/04/10 16:19:24 vanrogu Exp $
 */
public class RobotsTXTTest extends TestCase {

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;
    protected JSpiderConfiguration config2;

    /**
     * Public constructor giving a name to the test.
     */
    public RobotsTXTTest ( ) {
        super ( "RobotsTXTTest ");
    }

    /**
     * JUnit's overridden setUp method
     * @throws java.lang.Exception in case something fails during setup
     */
    protected void setUp() throws Exception {
        System.err.println("setUp");
        config = ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        config2 = new OverridingJSpiderConfiguration ( config );
        ((OverridingPropertySet)config2.getJSpiderConfiguration()).setValue("jspider.userAgent", "JSpiderUnitTest");
        ConfigurationFactory.setConfiguration(config);
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

    public void testAllowedNormalUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/resource.html" );
        ConfigurationFactory.setConfiguration(config);

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

    public void testDisallowedResourceNormalUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedResource1.html" );
        ConfigurationFactory.setConfiguration(config);

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testDisallowedFolderNormalUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedFolder1/resource.html" );
        ConfigurationFactory.setConfiguration(config);

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testSometimesAllowedResourceNormalUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedResource2.html" );
        ConfigurationFactory.setConfiguration(config);

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

    public void testSometimesAllowedFolderNormalUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedFolder2/resource.html" );
        ConfigurationFactory.setConfiguration(config);

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







    public void testAllowedTestUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/resource.html" );
        ConfigurationFactory.setConfiguration(config2);

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

    public void testDisallowedResourceTestUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedResource2.html" );
        ConfigurationFactory.setConfiguration(config2);

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testDisallowedFolderTestUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedFolder2/resource.html" );
        ConfigurationFactory.setConfiguration(config2);

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,0);
    }

    public void testSometimesAllowedResourceTestUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedResource1.html" );
        ConfigurationFactory.setConfiguration(config2);

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

    public void testSometimesAllowedFolderTestUserAgent ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/robotstxt/disallowedFolder1/resource.html" );
        ConfigurationFactory.setConfiguration(config2);

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
