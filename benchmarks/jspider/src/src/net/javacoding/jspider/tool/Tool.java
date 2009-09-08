package net.javacoding.jspider.tool;

import net.javacoding.jspider.api.event.EventSink;

/**
 * $Id: Tool.java,v 1.2 2003/04/01 19:44:41 vanrogu Exp $
 */
public interface Tool extends EventSink {

    public String getName ( );

    public void setParameters ( Parameters parameters );

    public String getUsage ( );

    public int getParameterCount ( );

}
