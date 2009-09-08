package net.javacoding.jspider.tool.impl;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.resource.ResourceFetchedEvent;
import net.javacoding.jspider.api.model.HTTPHeader;
import net.javacoding.jspider.tool.Parameters;

/**
 * $Id: InfoTool.java,v 1.3 2003/04/01 19:44:42 vanrogu Exp $
 */
public class InfoTool extends BaseToolImpl {

    public String getName() {
        return "info";
    }

    public boolean validateParams(Parameters parameters) {
        return parameters.getValues().length == 0;
    }

    public void notify(JSpiderEvent event) {
        if ( event instanceof ResourceFetchedEvent ) {
            ResourceFetchedEvent rfe = (ResourceFetchedEvent) event;
            System.out.println("URL          : " + rfe.getResource().getURL());
            System.out.println("HTTP Headers : ");
            HTTPHeader[] headers = rfe.getResource().getHeaders();
            for (int i = 0; i < headers.length; i++) {
                HTTPHeader header = headers[i];
                System.out.println("  " + header.getName() + ":" + header.getValue());
            }
            System.out.println("Mime Type    : " + rfe.getResource().getMime());
            System.out.println("Size         : " + rfe.getResource().getSize());
            System.out.println("Time (ms)    : " + rfe.getResource().getTimeMs());
        }
    }

}
