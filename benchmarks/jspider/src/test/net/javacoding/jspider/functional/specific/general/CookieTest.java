package net.javacoding.jspider.functional.specific.general;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.functional.TestingConstants;
import net.javacoding.jspider.api.event.EventSink;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.resource.ResourceFetchedEvent;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.mockobjects.OverridingJSpiderConfiguration;
import net.javacoding.jspider.mockobjects.OverridingPropertySet;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;

import java.io.*;
import java.net.URL;

/**
 * $Id: CookieTest.java,v 1.13 2003/04/10 16:19:22 vanrogu Exp $
 */
public class CookieTest extends TestCase implements EventSink {
    public final String TEST_URL = "http://" + TestingConstants.HOST + "/testcases/specific/general/testCookies1.php";
    public final String TEST_URL2 = "http://" + TestingConstants.HOST + "/testcases/specific/general/testCookies2.php";

    public final String TEST_URL_MULTIPLE = "http://" + TestingConstants.HOST + "/testcases/specific/general/testMultipleCookies1.php";
    public final String TEST_URL_MULTIPLE2 = "http://" + TestingConstants.HOST + "/testcases/specific/general/testMultipleCookies2.php";

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;
    protected JSpiderConfiguration config2;

    protected String cookieString;

    /**
     * Public constructor giving a name to the test.
     */
    public CookieTest() {
        super("CookieTest");
    }

    /**
     * JUnit's overridden setUp method
     * @throws java.lang.Exception in case something fails during setup
     */
    protected void setUp() throws Exception {
        System.err.println("setUp");
        cookieString = null;
        config = ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        config2 = new OverridingJSpiderConfiguration(config);
        ((OverridingPropertySet) config2.getBaseSiteConfiguration()).setValue("site.cookies.use", new Boolean(false));
        ConfigurationFactory.setConfiguration(config);
        sink = JUnitEventSink.getInstance();
        sink.setOtherSink(this);
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

    public void testWithCookies() throws Exception {
        URL url = new URL(TEST_URL);
        ConfigurationFactory.setConfiguration(config);

        JSpider jspider = new JSpider(url);
        jspider.start();
        assertEquals("testValue", cookieString);
    }

    public void testWithoutCookies() throws Exception {
        URL url = new URL(TEST_URL);
        ConfigurationFactory.setConfiguration(config2);

        JSpider jspider = new JSpider(url);
        jspider.start();
        assertEquals("noValueGiven", cookieString);
    }

    public void testMultipleCookies() throws Exception {
        URL url = new URL(TEST_URL_MULTIPLE);
        ConfigurationFactory.setConfiguration(config);

        JSpider jspider = new JSpider(url);
        jspider.start();
        assertEquals("testValue1testValue2testValue3testValue4testValue5", cookieString);
    }

    public void notify(JSpiderEvent event) {
        if (event instanceof ResourceFetchedEvent) {
            ResourceFetchedEvent e = (ResourceFetchedEvent) event;

            if (e.getResource().getURL().toString().equalsIgnoreCase(TEST_URL2) ||
                e.getResource().getURL().toString().equalsIgnoreCase(TEST_URL_MULTIPLE2)    ) {
                System.out.println("URL = " + e.getResource().getURL() + ", saving cookie!");
                cookieString = "noValueGiven";
                InputStream is = e.getResource().getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                try {
                    String line = br.readLine();
                    while (line != null) {
                        cookieString = line;
                        line = br.readLine();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    public void initialize() {
    }

    public void shutdown() {
    }
}
