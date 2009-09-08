package net.javacoding.jspider.core.throttle;


/**
 * Interface that will be implemented upon all classes that will act as a
 * throttle.  This type of component will control the load put upon a
 * certain host, thus keeping the spider threads from doing too much
 * requests in a too short time frame.
 *
 * <p>
 * If no throttling would be used, our JSpider engine could cause a DOS
 * attack on the target machine.
 * </p>
 * <p>
 * Please remark that the throttling is done on a per-host base, not on
 * a per-site base.  This means that if you have 2 sites, eg:
 * <ol>
 *   <li>http://localhost:80/</li>
 *   <li>http://localhost:8080/</li>
 * <ol>
 * and you would allow 2 hits/second by configuring a throttle, these
 * sites would _together_ get 2 hits/second at max, because they're
 * on the same machine.
 * Two sites on a difference hostname are throttled independently of
 * each other.
 * </p>
 *
 * $Id: Throttle.java,v 1.1 2002/11/20 17:26:09 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Throttle {

    /**
     * Method to which a spider thread will be directed before making
     * a call on a webserver.  This allows the throttle component
     * implementation to hold the thread under control.
     */
    public void throttle();

}
