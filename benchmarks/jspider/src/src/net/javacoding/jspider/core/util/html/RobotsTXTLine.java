package net.javacoding.jspider.core.util.html;

import java.net.URL;

/**
 * $Id: RobotsTXTLine.java,v 1.1 2003/02/11 17:33:03 vanrogu Exp $
 */
public class RobotsTXTLine {

    public static final String ALLOW="allow:";
    public static final String DISALLOW="disallow:";

    public static final int ROBOTSTXT_RULE_ALLOW = 0;
    public static final int ROBOTSTXT_RULE_DISALLOW = 1;

    protected int type;
    protected String resourceURI;

    RobotsTXTLine(String resourceURI, int type) {
        this.type = type;
        this.resourceURI = resourceURI;
    }

    public boolean matches(URL url) {
        String path = url.getPath();
        if ( path.length() == 0 && resourceURI.equals("/") ) {
            return true;
        } else {
          return url.getPath().startsWith(resourceURI);
        }
    }

    public int getType() {
        return type;
    }

    public String getResourceURI() {
        return resourceURI;
    }

    public static RobotsTXTLine parse(String line) {
        if (line == null) {
            return null;
        } else {
            line = line.trim();
            String lineLowerCase = line.toLowerCase();
            String resourceURI = "";
            int type = 0;
            if (lineLowerCase.startsWith(DISALLOW)) {
                resourceURI = line.substring(DISALLOW.length()).trim();
                type = RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW;
            } else if (lineLowerCase.startsWith(ALLOW)) {
                resourceURI = line.substring(ALLOW.length()).trim();
                type = RobotsTXTLine.ROBOTSTXT_RULE_ALLOW;
            } else {
                return null;
            }

            if ( resourceURI.length() > 0 ) {
              return new RobotsTXTLine(resourceURI, type);
            } else {
                return null;
            }
        }
    }

}
