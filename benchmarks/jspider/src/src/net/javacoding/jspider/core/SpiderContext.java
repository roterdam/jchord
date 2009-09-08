package net.javacoding.jspider.core;


import net.javacoding.jspider.api.model.Cookie;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.core.rule.Ruleset;
import net.javacoding.jspider.core.storage.Storage;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


/**
 *
 * $Id: SpiderContext.java,v 1.17 2003/04/29 17:53:47 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface SpiderContext {

    public void setCookies(Site site, Cookie[] cookies);

    public void preHandle(URLConnection connection, Site site);

    public void postHandle(URLConnection connection, Site site);

    public Storage getStorage();

    public Agent getAgent();

    public void setAgent(Agent agent);

    public URL getBaseURL();

    public EventDispatcher getEventDispatcher();

    public Ruleset getGeneralSpiderRules();

    public Ruleset getGeneralParserRules();

    public Ruleset getSiteSpiderRules(Site site);

    public Rule getSiteRobotsTXTRule(Site site);

    public Ruleset getSiteParserRules(Site site);

    public void throttle(Site site);

    public void registerRobotsTXT(Site site, InputStream inputStream);

    public void registerRobotsTXTError ( Site site );

    public void registerRobotsTXTSkipped(Site site);

    public void registerNewSite ( Site site );

    public boolean getUseProxy ( );

    public String getUserAgent ( );

}
