package net.javacoding.jspider.core.model;


import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.storage.exception.InvalidStateForActionException;
import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.util.URLUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.*;


/**
 *
 * $Id: ResourceInternal.java,v 1.13 2003/04/11 16:37:04 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceInternal implements ParsedResource, ParseErrorResource, ParseIgnoredResource, ForbiddenResource, FetchIgnoredResource, FetchErrorResource {

    protected StorageSPI storage;

    protected int site;

    protected URL url;
    protected Date discoveryTime;
    protected FolderInternal folder;
    protected int state;
    protected int id;

    protected int httpStatus;
    protected int size;
    protected int timeMs;
    protected String mimeType;
    protected Date fetchTime;
    protected HTTPHeader[] headers;

    protected Decision spiderDecision;
    protected Decision parseDecision;


    public ResourceInternal(StorageSPI storage, int id, int siteId, URL url, Date discoveryTime, FolderInternal folder) {
        this.site = siteId;
        this.storage = storage;
        this.id = id;
        this.url = url;
        this.discoveryTime = discoveryTime;
        this.folder = folder;

        this.state = Resource.STATE_DISCOVERED;
    }

    public ResourceInternal(StorageSPI storage, int id, Site site, URL url, Date discoveryTime, FolderInternal folder) {
        this(storage, id, ((SiteInternal) site).getId(), url, discoveryTime, folder);
    }

    public ResourceInternal(StorageSPI storage, Site site, URL url, Date discoveryTime, FolderInternal folder) {
        this(storage, 0, site, url, discoveryTime, folder);
    }

    public void setFetched(int httpStatus, int size, int timeMs, String mimeType, Date fetchTime, HTTPHeader[] headers) {
        if (state != Resource.STATE_DISCOVERED) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource fetched - it's not in the discovered state - was " + state);
        }
        this.httpStatus = httpStatus;
        this.size = size;
        this.timeMs = timeMs;
        this.mimeType = mimeType;
        this.fetchTime = fetchTime;
        this.headers = headers;
        state = Resource.STATE_FETCHED;
    }

    public void setFetchError(int httpStatus, HTTPHeader[] headers) {
        if (state != Resource.STATE_DISCOVERED && state != Resource.STATE_FETCH_ERROR) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource fetch error - it's not in the discovered state - was" + state);
        }
        this.httpStatus = httpStatus;
        this.headers = headers;
        state = Resource.STATE_FETCH_ERROR;
    }

    public void setParseError() {
        if (state != Resource.STATE_FETCHED && state != Resource.STATE_PARSE_ERROR) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource parse error - it's not in the fetched state - was " + state);
        }
        state = Resource.STATE_PARSE_ERROR;
    }

    public void setParsed() {
        if (state != Resource.STATE_FETCHED && state != Resource.STATE_PARSED) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource parsed - it's not in the fetched state - was " + state);
        }
        state = Resource.STATE_PARSED;
    }

    public void setFetchIgnored() {
        if (state != Resource.STATE_DISCOVERED && state != Resource.STATE_FETCH_IGNORED) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource fetch_ignored - it's not in the discovered state - was " + state);
        }
        state = Resource.STATE_FETCH_IGNORED;
    }

    public void setParseIgnored() {
        if (state != Resource.STATE_FETCHED && state != Resource.STATE_PARSE_IGNORED) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource parse_ignored - it's not in the fetched state - was " + state);
        }
        state = Resource.STATE_PARSE_IGNORED;
    }

    public void setForbidden() {
        if (state != Resource.STATE_DISCOVERED && state != Resource.STATE_FETCH_FORBIDDEN) {
            LogFactory.getLog(Resource.class).error("error in state transition for resource " + url + ":\n" + this);
            throw new InvalidStateTransitionException("cannot set resource forbidden - it's not in the discovered state - was " + state);
        }
        state = Resource.STATE_FETCH_FORBIDDEN;
    }

    public int getId() {
        return id;
    }

    public void setInt(int id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public String getFileName() {
        return URLUtil.getFileName(url);
    }

    public URL getURL() {
        return url;
    }

    public Site getSite() {
        return folder.getSite();
    }

    public Folder getFolder() {
        return folder;
    }

    public String getName() {
        return url.getFile();
    }

    public Date getDiscoveryTime() {
        return discoveryTime;
    }

    public Resource[] getReferers() {
        return storage.getResourceDAO().getRefereringResources(this);
    }

    public Resource[] getReferencedResources() {
        if (state != Resource.STATE_PARSED) {
            throw new InvalidStateForActionException("cannot get referenced resources if not parsed");
        }
        return storage.getResourceDAO().getReferencedResources(this);
    }

    public int getHttpStatus() {
        if (state == Resource.STATE_DISCOVERED) {
            throw new InvalidStateForActionException("cannot get http status for a resource that's not fetched");
        }
        return httpStatus;
    }

    public int getHttpStatusInternal() {
        return httpStatus;
    }

    public void setHttpStatus(int status) {
        this.httpStatus = status;
    }

    public HTTPHeader[] getHeaders() {
        return headers;
    }

    public int getTimeMs() {
        if (state < Resource.STATE_FETCHED) {
            throw new InvalidStateForActionException("cannot get timing for non-fetched resource");
        }
        return timeMs;
    }

    public int getTimeMsInternal() {
        return timeMs;
    }

    public int getSize() {
        if (state < Resource.STATE_FETCHED) {
            throw new InvalidStateForActionException("cannot get size for non-fetched resource");
        }
        return size;
    }

    public int getSizeInternal() {
        return size;
    }

    public String getMime() {
        if (state < Resource.STATE_FETCHED) {
            throw new InvalidStateForActionException("cannot get mime type for non-fetched resource");
        }
        return mimeType;
    }

    public String getMimeInternal() {
        return mimeType;
    }

    public Date getFetchTime() {
        if (state < Resource.STATE_FETCHED) {
            throw new InvalidStateForActionException("cannot get fetch time for non-fetched resource");
        }
        return fetchTime;
    }

    public Date getFetchTimeInternal() {
        return fetchTime;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(url.toString());
        sb.append("\n  STATUS : ");
        sb.append(translateState(state));
        sb.append("\n  ");
        sb.append("\n  SPIDER DECISION : ");
        Decision sd = getSpiderDecision();
        if (sd == null) {
            sb.append("\n  ");
            sb.append("[Not yet taken]");
        } else {
            DecisionStep[] steps = sd.getSteps();
            for (int i = 0; i < steps.length; i++) {
                DecisionStep step = steps[i];
                sb.append("\n  ");
                sb.append(step.toString());
            }
        }
        sb.append("\n  ");
        sb.append("\n  PARSE DECISION : ");
        Decision pd = getParseDecision();
        if (pd == null) {
            sb.append("\n  ");
            sb.append("[Not yet taken]");
        } else {
            DecisionStep[] steps = pd.getSteps();
            for (int i = 0; i < steps.length; i++) {
                DecisionStep step = steps[i];
                sb.append("\n  ");
                sb.append(step.toString());
            }
        }
        sb.append("\n");

        switch (state) {
            case STATE_DISCOVERED:
                break;
            case STATE_FETCH_ERROR:
                sb.append("  HTTP Status: ");
                sb.append(this.getHttpStatus());
                Resource[] referers = this.getReferers();
                sb.append("\n  REFERERS: " + referers.length);
                for (int i = 0; i < referers.length; i++) {
                    Resource referer = referers[i];
                    sb.append("\n    ");
                    sb.append(referer.getURL());
                }
                break;
            case STATE_FETCH_IGNORED:
                break;
            case STATE_FETCH_FORBIDDEN:
                break;
            case STATE_FETCHED:
                sb.append("  HTTP Status: ");
                sb.append(this.getHttpStatus());
                sb.append(", Content size: ");
                sb.append(this.getSize());
                sb.append(", Mime Type: ");
                sb.append(this.getMime());
                sb.append(", Fetch time: ");
                sb.append(this.getTimeMs());
                break;
            case STATE_PARSE_ERROR:
                break;
            case STATE_PARSE_IGNORED:
                break;
            case STATE_PARSED:
                sb.append("  HTTP Status: ");
                sb.append(this.getHttpStatus());
                sb.append(", Content size: ");
                sb.append(this.getSize());
                sb.append(", Mime Type: ");
                sb.append(this.getMime());
                sb.append(", Fetch time: ");
                sb.append(this.getTimeMs());

                referers = this.getReferers();
                sb.append("\n  REFERERS: " + referers.length);
                for (int i = 0; i < referers.length; i++) {
                    Resource referer = referers[i];
                    sb.append("\n    ");
                    sb.append(referer.getURL());
                }

                if (state == STATE_PARSED) {

                    Resource[] references = this.getReferencedResources();
                    sb.append("\n  REFERENCES: " + references.length);
                    for (int i = 0; i < references.length; i++) {
                        Resource reference = references[i];
                        sb.append("\n    ");
                        sb.append(reference.getURL());
                    }

                    EMailAddress[] emails = this.getEmailAddresses();
                    sb.append("\n  E-MAIL ADDRESSES: " + emails.length);
                    for (int i = 0; i < emails.length; i++) {
                        EMailAddress email = emails[i];
                        sb.append("\n    ");
                        sb.append(email.getAddress());
                    }

                } else {
                    sb.append("\n  EMAIL ADDRESSES and REFERENCES not known [Resource not parsed]");
                }
                break;
        }

        sb.append("\n");

        return sb.toString();
    }

    protected String translateState(int state) {
        switch (state) {
            case Resource.STATE_DISCOVERED:
                return "DISCOVERED";
            case Resource.STATE_FETCH_ERROR:
                return "FETCH_ERROR";
            case Resource.STATE_PARSE_ERROR:
                return "PARSE_ERROR";
            case Resource.STATE_FETCHED:
                return "FETCHED";
            case Resource.STATE_FETCH_FORBIDDEN:
                return "FETCH_FORBIDDEN";
            case Resource.STATE_FETCH_IGNORED:
                return "FETCH_IGNORED";
            case Resource.STATE_PARSE_IGNORED:
                return "PARSE_IGNORED";
            case Resource.STATE_PARSED:
                return "PARSED";
            default:
                return "?!? UNKNOWN STATE ?!?";

        }
    }

    public InputStream getInputStream() {
        return storage.getContentDAO().getInputStream(id);
    }

    public void setBytes(byte[] bytes) {
        storage.getContentDAO().setBytes(id, bytes);
    }

    public Date getFetchTimeStamp() {
        return fetchTime;
    }

    public String getStateName() {
        return translateState(state);
    }

    public Decision getSpiderDecision() {
        return storage.getDecisionDAO().findSpiderDecision(this);
    }

    public Decision getParseDecision() {
        return storage.getDecisionDAO().findParseDecision(this);
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setMime(String mime) {
        this.mimeType = mime;
    }

    public void setTime(int ms) {
        this.timeMs = ms;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSiteId() {
        return site;
    }

    public ResourceReference[] getOutgoingReferences() {
        return storage.getResourceDAO().getOutgoingReferences(this) ;
    }

    public ResourceReference[] getIncomingReferences() {
        return storage.getResourceDAO().getIncomingReferences(this) ;
    }

    public EMailAddress[] getEmailAddresses() {
        return storage.getEMailAddressDAO().findByResource(this);
    }

    public EMailAddressReference[] getEmailAddressReferences() {
        return storage.getEMailAddressDAO().findReferencesByResource(this);
    }
}
