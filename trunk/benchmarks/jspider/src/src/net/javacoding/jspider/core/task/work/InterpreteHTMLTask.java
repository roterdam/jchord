package net.javacoding.jspider.core.task.work;


import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.api.event.resource.*;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.EMailAddressInternal;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.event.CoreEvent;
import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.util.html.URLFinder;
import net.javacoding.jspider.core.util.html.URLFinderCallback;
import net.javacoding.jspider.core.util.EMailAddressUtil;

import java.io.*;
import java.net.URL;


/**
 *
 * $Id: InterpreteHTMLTask.java,v 1.15 2003/04/10 16:19:14 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class InterpreteHTMLTask extends BaseWorkerTaskImpl implements URLFinderCallback {

    protected FetchedResource spideredResource;
    protected URL url;

    protected URL contextURL;

    public InterpreteHTMLTask(SpiderContext context, FetchedResource resource) {
        super(context, WorkerTask.WORKERTASK_THINKERTASK);
        this.spideredResource = resource;
        url = spideredResource.getURL();
        contextURL = url;
    }

    public void prepare() {
    }

    public void execute() {
        CoreEvent event = null;
        try {
            InputStream inputStream = spideredResource.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line = br.readLine();
            while (line != null) {
                URLFinder.findURLs(this, line);
                line = br.readLine();
            }
            event = new ResourceParsedOkEvent(context, url);
        } catch (IOException e) {
            LogFactory.getLog(InterpreteHTMLTask.class).error("i/o exception during parse", e);
            event = new ResourceParsedErrorEvent(context, url, e);
        } catch (Exception e) {
            LogFactory.getLog(InterpreteHTMLTask.class).error("exception during parse", e);
            event = new ResourceParsedErrorEvent(context, url, e);
        } finally {
            notifyEvent(url, event );
        }
    }

    public void urlFound(URL foundURL) {
        if (EMailAddressUtil.isEMailAddress(foundURL)) {
            String emailAddress = EMailAddressUtil.getEMailAddress(foundURL);
            EMailAddress address = context.getStorage().getEMailAddressDAO().find(emailAddress);
            if (address == null) {
                address = new EMailAddressInternal(emailAddress);
                context.getEventDispatcher().dispatch(new EMailAddressDiscoveredEvent(this.spideredResource, emailAddress));
            }
            context.getStorage().getEMailAddressDAO().register(spideredResource, address);
            context.getEventDispatcher().dispatch(new EMailAddressReferenceDiscoveredEvent(this.spideredResource, address));
        } else {
            notifyEvent(url, new URLFoundEvent(context, url, foundURL));
        }
    }

    public void malformedUrlFound(String malformedURL) {
        context.getEventDispatcher().dispatch(new MalformedURLFoundEvent(context.getStorage().getResourceDAO().getResource(url), malformedURL));
    }

    public URL getContextURL() {
        return contextURL;
    }

    public void setContextURL(URL url) {
        this.contextURL = url;
    }

    public void malformedContextURLFound(String malformedURL) {
        context.getEventDispatcher().dispatch(new MalformedBaseURLFoundEvent(spideredResource, malformedURL));
    }

}
