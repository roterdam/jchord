package net.javacoding.jspider.core.task.work;

import net.javacoding.jspider.api.model.HTTPHeader;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.event.CoreEvent;
import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.util.http.HTTPHeaderUtil;

import java.io.*;
import java.net.*;

/**
 * $Id: FetchRobotsTXTTaskImpl.java,v 1.19 2003/04/25 21:29:05 vanrogu Exp $
 */
public class FetchRobotsTXTTaskImpl extends BaseWorkerTaskImpl {

    protected URL url;
    protected Site site;

    public FetchRobotsTXTTaskImpl(SpiderContext context, URL url, Site site) {
        super(context, WorkerTask.WORKERTASK_SPIDERTASK);
        this.url = url;
        this.site = site;
    }

    public void prepare() {
        context.throttle(site);
    }

    public void execute() {

        CoreEvent event = null;
        URLConnection connection = null;

        InputStream inputStream = null;

        int httpStatus = 0;
        HTTPHeader[] headers = null;

        try {
            connection = url.openConnection();

            // RFC states that redirects should be followed.
            // see: http://www.robotstxt.org/wc/norobots-rfc.txt
            ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-agent", site.getUserAgent() );
            context.preHandle(connection, site);

            long start = System.currentTimeMillis();
            connection.connect();

            if (connection instanceof HttpURLConnection) {
                httpStatus = ((HttpURLConnection) connection).getResponseCode();
                switch (httpStatus) {
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                        return;
                    default:
                        break;
                }
            }
            inputStream = new BufferedInputStream(connection.getInputStream());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = new BufferedInputStream(inputStream);
            try {
                    int i = is.read();
                    while (i != -1) {
                        os.write(i);
                        i = is.read();
                    }
            } catch (IOException e) {
                LogFactory.getLog(FetchRobotsTXTTaskImpl.class).error("i/o exception during fetch robots.txt",e);
            }
            String contentType = connection.getContentType();
            int size = connection.getContentLength();
            int timeMs = (int) (System.currentTimeMillis() - start);

            headers = HTTPHeaderUtil.getHeaders(connection);

            if (httpStatus >= 200 && httpStatus < 303) {
                event = new RobotsTXTSpideredOkEvent(url,context, url, httpStatus, connection, contentType, timeMs, size, os.toByteArray(), headers);
            } else if (httpStatus >= 400 && httpStatus < 500) {
                event = new RobotsTXTUnexistingEvent(url,context, url, httpStatus, connection, headers, null);
            } else {
                event = new RobotsTXTSpideredErrorEvent(url,context, url, httpStatus, connection, headers, null);
            }
        } catch (FileNotFoundException e) {
            headers = HTTPHeaderUtil.getHeaders(connection);
            event = new RobotsTXTUnexistingEvent(url,context, url, 404, connection, headers, e);
        } catch (Exception e) {
            event = new RobotsTXTSpideredErrorEvent(url,context, url, httpStatus, connection, headers, e);
        } finally {
            notifyEvent(url, event);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LogFactory.getLog(FetchRobotsTXTTaskImpl.class).error("i/o exception closing inputstream",e);
                }
            }
        }
    }

}
