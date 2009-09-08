package net.javacoding.jspider.core.event;


import net.javacoding.jspider.core.event.impl.*;

import java.net.URL;


/**
 *
 * $Id: CoreEventVisitor.java,v 1.3 2003/02/13 20:12:53 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface CoreEventVisitor {

    public void visit(URL url, CoreEvent event);

    public void visit(URL url, URLSpideredOkEvent event);

    public void visit(URL url, URLSpideredErrorEvent event);

    public void visit(URL url, ResourceParsedOkEvent event);

    public void visit(URL url, ResourceParsedErrorEvent event);

    public void visit(URL url, URLFoundEvent event);

    public void visit(URL url, RobotsTXTSpideredOkEvent event);

    public void visit(URL url, RobotsTXTSpideredErrorEvent event);

    public void visit(URL url, RobotsTXTUnexistingEvent event);

}
