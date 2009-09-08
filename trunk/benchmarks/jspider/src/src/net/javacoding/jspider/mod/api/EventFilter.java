/**
 * This source is subject to the LGPL Open Source license.  See the license
 * file that came with the distribution, or visit http://www.opensource.org
 * for more information.
 */
package net.javacoding.jspider.mod.api;


/* project imports */

import net.javacoding.jspider.api.event.JSpiderEvent;


/**
 * Interface that will be implemented upon a component in a JSpider module that
 * allows to filter JSpider event coming from the engine.
 *
 * <p>
 * This can be handy, for instance, if you're writing a module that checks all
 * error conditions on a server.  In that case, you would not be interested in
 * the HTTP statuses 2xx, but you would definitely want to receive
 * notifications about 4xx and 5xx states.
 * </p>
 *
 * $Id: EventFilter.java,v 1.1.1.1 2002/11/20 17:02:32 vanrogu Exp $
 *
 * @author Günther Van Roey (gunther@javacoding.net)
 * @version $Revision: 1.1.1.1 $ $Date: 2002/11/20 17:02:32 $ $Name:  $
 */
public interface EventFilter {

    /**
     * Method that will filter an events and tells the dispatcher whether we're
     * interested in it or not.  Returns true if the event must be dispatched,
     * false if it can be ignored.
     * @param event - the JSpider event the be filtered
     * @return boolean value telling whether to dispatch the event or not
     */
    public boolean filterEvent(JSpiderEvent event);

}
