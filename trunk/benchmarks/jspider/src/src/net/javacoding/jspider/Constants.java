package net.javacoding.jspider;


/**
 * Interface with some constants we need.
 *
 * $Id: Constants.java,v 1.10 2003/04/10 16:19:03 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Constants {

    /** Version of this source. */
    public static final String VERSION = "@jspider.version.number@";


    /** Versionstring for this source. */
    public static final String VERSIONSTRING = "@jspider.version.string@";


    /** Versionstring for this source. */
    public static final String BUILDTIMESTAMP = "@build.DSTAMP@";



    /**
     * Useragent for this build.
     */
    public static final String USERAGENT = "JSpider v" + VERSION + " (http://j-spider.sourceforge.net)";

}