/**
 * $Id: SpiderContextImpl.java,v 1.32 2003/04/10 16:19:05 vanrogu Exp $
 */
package net.javacoding.jspider.core.impl;


import net.javacoding.jspider.Constants;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.api.event.site.UserAgentObeyedEvent;
import net.javacoding.jspider.api.model.Cookie;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.Agent;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.rule.*;
import net.javacoding.jspider.core.rule.impl.*;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.throttle.Throttle;
import net.javacoding.jspider.core.throttle.ThrottleFactory;
import net.javacoding.jspider.core.util.Base64Encoder;
import net.javacoding.jspider.core.util.URLUtil;
import net.javacoding.jspider.core.util.config.*;
import net.javacoding.jspider.core.util.http.CookieUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * $Id: SpiderContextImpl.java,v 1.32 2003/04/10 16:19:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SpiderContextImpl implements SpiderContext {

    protected Agent agent;
    protected URL baseURL;
    protected EventDispatcher eventDispatcher;
    protected ThrottleFactory throttleFactory;
    protected Map throttles;
    protected Map spiderRules;
    protected Map parserRules;
    protected Map robotsTXTRules;
    protected Storage storage;
    protected CookieUtil cookieUtil;
    protected String authenticationString;
    protected boolean useProxyAuthentication;
    protected boolean useProxy;
    protected Ruleset generalSpiderRules;
    protected Ruleset generalParserRules;
    protected String defaultUserAgent;
    protected Log log;

    public SpiderContextImpl(URL baseURL, EventDispatcher eventDispatcher, ThrottleFactory throttleFactory, Storage storage) {
        this.baseURL = URLUtil.normalize(baseURL);
        this.eventDispatcher = eventDispatcher;
        this.throttleFactory = throttleFactory;
        this.storage = storage;
        this.cookieUtil = new CookieUtil();
        this.throttles = new HashMap();
        this.spiderRules = new HashMap ( );
        this.parserRules = new HashMap ( );
        this.robotsTXTRules = new HashMap ( );
        this.generalSpiderRules = RuleFactory.createGeneralSpiderRules();
        this.generalParserRules = RuleFactory.createGeneralParserRules();
        this.log = LogFactory.getLog(SpiderContext.class);

        PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();

        this.defaultUserAgent = props.getString(ConfigConstants.CONFIG_USERAGENT, Constants.USERAGENT );
        log.info("default user Agent is '" + defaultUserAgent + "'");

        PropertySet proxyProps = new MappedPropertySet ( ConfigConstants.CONFIG_PROXY, props );

        useProxy = proxyProps.getBoolean(ConfigConstants.CONFIG_PROXY_USE, false);
        if (useProxy) {

            String proxyHost = proxyProps.getString(ConfigConstants.CONFIG_PROXY_HOST, "");
            String proxyPort = proxyProps.getString(ConfigConstants.CONFIG_PROXY_PORT, "");

            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);

            log.info ("Using proxy " + proxyHost + ":" + proxyPort );

            useProxyAuthentication = proxyProps.getBoolean(ConfigConstants.CONFIG_PROXY_AUTHENTICATE, false);
            if (useProxyAuthentication) {
                String proxyUser = proxyProps.getString(ConfigConstants.CONFIG_PROXY_USERNAME, "");
                String proxyPwd = proxyProps.getString(ConfigConstants.CONFIG_PROXY_PASSWORD, "");
                String plain = proxyUser + ":" + proxyPwd;
                authenticationString = "Basic " + Base64Encoder.base64Encode(plain);
                log.info("Authenticating against proxy, user:" + proxyUser);
            }
        }
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public synchronized void setCookies(Site site, Cookie[] cookies) {
        if (cookies != null && cookies.length > 0) {
          storage.getCookieDAO().save(site, cookies);
        }
    }

    public void preHandle(URLConnection connection, Site site) {
        connection.setDefaultUseCaches(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-Control","max-age=0,no-cache");
        connection.setRequestProperty("Pragma","no-cache");

        if (useProxyAuthentication) {
            connection.setRequestProperty("Proxy-Authorization", authenticationString);
        }

        String cookieString = site.getCookieString();
        boolean useCookies = site.getUseCookies();
        if (useCookies && cookieString != null) {
            connection.setRequestProperty("Cookie", cookieString);
        }
    }

    public void postHandle(URLConnection connection, Site site) {
        setCookies(site, cookieUtil.getCookies(connection));
        storage.getSiteDAO().save(site);
    }

    public Agent getAgent() {
        return agent;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void throttle(Site site) {
        Throttle throttle = null;

        throttle = (Throttle) throttles.get(site.getHost());
        if (throttle == null) {
            throttle = throttleFactory.createThrottle(site);
            throttles.put(site.getHost(), throttle);
        }
        throttle.throttle();
    }

    public Ruleset getGeneralSpiderRules ( ) {
        return generalSpiderRules;
    }

    public Ruleset getGeneralParserRules ( ) {
        return generalParserRules;
    }

    public Ruleset getSiteSpiderRules(Site site) {
        Ruleset ruleSet =(Ruleset)spiderRules.get(site);
        if ( ruleSet == null) {
            return generalSpiderRules;
        } else {
            return ruleSet;
        }
    }

    public Rule getSiteRobotsTXTRule(Site site) {
        Rule rule = (Rule) robotsTXTRules.get(site);
        if ( rule == null ) {
            rule = new RobotsTXTSkippedRule();
        }
        return rule;
    }

    public Ruleset getSiteParserRules(Site site) {
        Ruleset ruleSet =(Ruleset)parserRules.get(site);
        if ( ruleSet == null) {
            return generalParserRules;
        } else {
            return ruleSet;
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public void registerRobotsTXT(Site site, InputStream inputStream) {
        try {
            RobotsTXTRule robotsTxtRule = new RobotsTXTRule(defaultUserAgent, inputStream);
            ((Ruleset)spiderRules.get(site)).addRule(robotsTxtRule);
            robotsTXTRules.put(site, robotsTxtRule);
            eventDispatcher.dispatch(new UserAgentObeyedEvent(site, robotsTxtRule.getObeyedUserAgent()));
        } catch (IOException e) {
            log.error("i/o exception while reading robots.txt", e);
        }
    }

    public void registerRobotsTXTError(Site site) {
        RobotsTXTErrorRule robotsTxtRule = new RobotsTXTErrorRule();
        ((Ruleset)spiderRules.get(site)).addRule(robotsTxtRule);
        robotsTXTRules.put(site, robotsTxtRule);
    }

    public void registerRobotsTXTSkipped(Site site) {
        RobotsTXTSkippedRule robotsTxtRule = new RobotsTXTSkippedRule();
        ((Ruleset)spiderRules.get(site)).addRule(robotsTxtRule);
        robotsTXTRules.put(site, robotsTxtRule);
    }

    public void registerNewSite(Site site) {
        SiteInternal sitei = (SiteInternal) site;

        sitei.setBaseSite(URLUtil.getSiteURL(baseURL).equals(site.getURL()));

        PropertySet siteProps = ConfigurationFactory.getConfiguration().getSiteConfiguration(site);
        sitei.setUseCookies(siteProps.getBoolean(ConfigConstants.SITE_COOKIES_USE, true));
        sitei.setUseProxy (siteProps.getBoolean(ConfigConstants.SITE_PROXY_USE, true));
        sitei.setObeyRobotsTXT (siteProps.getBoolean(ConfigConstants.SITE_ROBOTSTXT_OBEY, true));
        sitei.setFetchRobotsTXT (siteProps.getBoolean(ConfigConstants.SITE_ROBOTSTXT_FETCH, true));
        sitei.setUserAgent(siteProps.getString(ConfigConstants.SITE_USERAGENT, defaultUserAgent));
        sitei.setHandle(siteProps.getBoolean(ConfigConstants.SITE_HANDLE, false));

        if ( sitei.mustHandle () ) {

        log.info("using userAgent '" + sitei.getUserAgent() + "' for site '" + site.getURL() + "'");

        if ((!siteProps.getBoolean(ConfigConstants.SITE_PROXY_USE, true)) && getUseProxy()) {
            log.info("Using no proxy for " + site.getURL());
            String nonProxyHosts = System.getProperty("http.nonProxyHosts");
            if (nonProxyHosts == null) {
                nonProxyHosts = site.getURL().getHost();
            } else {
                nonProxyHosts += "|" + site.getURL().getHost();
            }
            System.setProperty("http.nonProxyHosts", nonProxyHosts);
        } else {
            if (getUseProxy()) {
                log.info("Using proxy for " + site.getURL());
            }
        }
        } else {
            log.info("site " + sitei.getURL() + " must not be handled.");
        }

        spiderRules.put(site, RuleFactory.createSiteSpiderRules(site));
        parserRules.put(site, RuleFactory.createSiteParserRules(site));
    }

    public boolean getUseProxy() {
        return useProxy;
    }

    public String getUserAgent() {
        return defaultUserAgent;
    }
}
