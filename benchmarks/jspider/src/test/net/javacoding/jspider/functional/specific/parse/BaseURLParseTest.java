package net.javacoding.jspider.functional.specific.parse;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.functional.TestingConstants;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;

import java.net.URL;

/**
 * $Id: BaseURLParseTest.java,v 1.3 2003/04/29 17:53:50 vanrogu Exp $
 */
public class BaseURLParseTest extends TestCase {

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;

    /**
     * Public constructor giving a name to the test.
     */
    public BaseURLParseTest ( ) {
        super ( "BaseURLParseTest ");
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

    public void testWithFileReference ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testBaseURLParse.php" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,3);
        testEventCount(net.javacoding.jspider.api.event.resource.MalformedBaseURLFoundEvent.class,0);
    }


    public void testWithFolderReferenceAndSlash ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testBaseURLParseNoFile.php" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,3);
        testEventCount(net.javacoding.jspider.api.event.resource.MalformedBaseURLFoundEvent.class,0);
    }

    public void testWithFolderReferenceNoSlash ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testBaseURLParseNoSlash.php" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );

        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStartedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringSummaryEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.engine.SpideringStoppedEvent.class,1);

        testEventCount(net.javacoding.jspider.api.event.site.SiteDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTMissingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.site.RobotsTXTFetchErrorEvent.class,0);

        testEventCount(net.javacoding.jspider.api.event.resource.ResourceDiscoveredEvent.class,5);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,5);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,4);
        testEventCount(net.javacoding.jspider.api.event.resource.MalformedBaseURLFoundEvent.class,0);
    }

    public void testMalformed ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/parse/testMalformedBaseURLParse.php" );

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
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchedEvent.class,2);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceFetchErrorEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceForbiddenEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForFetchingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent.class,0);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceReferenceDiscoveredEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.ResourceParsedEvent.class,1);
        testEventCount(net.javacoding.jspider.api.event.resource.MalformedBaseURLFoundEvent.class,1);
    }


    protected void testEventCount ( Class eventClass, int expectedCount  ) {
        assertEquals(eventClass.getName(), expectedCount, sink.getEventCount(eventClass));
    }

}
