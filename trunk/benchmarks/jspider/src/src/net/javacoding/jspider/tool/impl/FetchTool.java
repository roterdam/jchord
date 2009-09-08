package net.javacoding.jspider.tool.impl;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.resource.ResourceFetchedEvent;

import java.io.IOException;
import java.io.InputStream;

/**
 * $Id: FetchTool.java,v 1.3 2003/04/01 19:44:42 vanrogu Exp $
 */
public class FetchTool extends BaseToolImpl {

    public String getName() {
        return "fetch";
    }

    public void notify(JSpiderEvent event) {
        if ( event instanceof ResourceFetchedEvent ) {
            ResourceFetchedEvent rfe = (ResourceFetchedEvent)event;
            try {
                InputStream is = rfe.getResource().getInputStream();
                int read = is.read();
                while ( read != -1 ) {
                    System.out.write(read);
                    read = is.read();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
