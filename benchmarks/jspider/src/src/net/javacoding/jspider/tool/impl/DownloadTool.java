package net.javacoding.jspider.tool.impl;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.resource.ResourceFetchedEvent;

import java.io.*;

/**
 * $Id: DownloadTool.java,v 1.3 2003/04/01 19:44:42 vanrogu Exp $
 */
public class DownloadTool extends BaseToolImpl {

    public String getName() {
        return "download";
    }

    public String getUsage() {
        return "[filename]";
    }

    public int getParameterCount() {
        return 1;
    }

    public void notify(JSpiderEvent event) {

        String fileName = parameters.getValues()[0];

        if ( event instanceof ResourceFetchedEvent ) {
            ResourceFetchedEvent rfe = (ResourceFetchedEvent) event;
            int counter = 0;
            try {
                InputStream is = rfe.getResource().getInputStream();
                OutputStream os = new FileOutputStream(fileName);
                int read = is.read();
                while ( read != -1 ) {
                    counter++;
                    os.write(read);
                    read = is.read ( );
                }
                os.flush();
                os.close();
                is.close();
                System.out.println("Downloaded resource to '" + fileName + "'  (" + counter + " bytes)" );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
