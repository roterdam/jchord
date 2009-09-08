package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.api.model.Cookie;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.Agent;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.dispatch.EventDispatcher;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.core.rule.Ruleset;
import net.javacoding.jspider.core.storage.Storage;

import java.io.InputStream;
import java.net.*;

/**
 * Mock implementation of a SpiderContext.
 *
 * $Id: SimpleSpiderContext.java,v 1.14 2003/04/29 17:53:50 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SimpleSpiderContext implements SpiderContext {

    protected URL baseUrl;

    public SimpleSpiderContext ( ) throws MalformedURLException{
        this ( new URL("http://j-spider.sourceforge.net") );
    }

    public SimpleSpiderContext ( URL url ) {
        this.baseUrl = url;
    }

    public void setCookies(Site site, Cookie[] cookies) {
    }

    public void preHandle(URLConnection connection) {
    }

    public void preHandle(URLConnection connection, Site site) {
    }

    public void postHandle(URLConnection connection, Site site) {
    }

    public Storage getStorage() {
        return null;
    }

    public Agent getAgent() {
        return null;
    }

    public void setAgent(Agent agent) {
    }

    public URL getBaseURL() {
        return baseUrl;
    }

    public EventDispatcher getEventDispatcher() {
        return null;
    }

    public Ruleset getSiteSpiderRules(Site site) {
        return null;
    }

    public Ruleset getSiteParserRules(Site site) {
        return null;
    }

    public Rule getSiteRobotsTXTRule(Site site) {
        return null;
    }

    public Ruleset getGeneralSpiderRules() {
        return null;
    }

    public Ruleset getGeneralParserRules() {
        return null;
    }

    public Ruleset getSiteRules(Site site) {
        return null;
    }

    public void throttle(Site site) {
    }

    public void registerRobotsTXT(Site site, InputStream inputStream) {
    }

    public void registerRobotsTXTError(Site site) {
    }

    public void registerRobotsTXTSkipped(Site site) {
    }

    public void registerNewSite(Site site) {
    }

    public boolean getUseProxy() {
        return false;
    }

    public String getUserAgent() {
        return null;
    }
}
