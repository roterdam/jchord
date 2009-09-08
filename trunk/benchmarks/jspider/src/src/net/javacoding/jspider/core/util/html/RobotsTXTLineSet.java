package net.javacoding.jspider.core.util.html;

import java.io.*;
import java.util.ArrayList;

/**
 * $Id: RobotsTXTLineSet.java,v 1.2 2003/02/13 20:12:56 vanrogu Exp $
 */
public class RobotsTXTLineSet {

    public static final String USER_AGENT="user-agent:";

    protected String userAgent;
    protected RobotsTXTLine[] lines;

    RobotsTXTLineSet(String userAgent, RobotsTXTLine[] lines) {
        this.userAgent = userAgent;
        this.lines = lines;
    }

    public String getUserAgent ( ) {
        return userAgent;
    }

    public RobotsTXTLine[] getLines ( ) {
        return lines;
    }

    public static RobotsTXTLineSet findLineSet ( InputStream is, String spiderUserAgent) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        return findLineSet(br, spiderUserAgent);
    }

    public static RobotsTXTLineSet findLineSet(BufferedReader br, String spiderUserAgent) throws IOException {
        String userAgent = findUserAgent ( br, spiderUserAgent );
        if ( userAgent == null ) {
          return null;
        } else {
          RobotsTXTLine[] lines = parseRules ( br );
          return new RobotsTXTLineSet(userAgent, lines);
        }
    }

    private static String findUserAgent ( BufferedReader br, String spiderUserAgent ) throws IOException {
        if ( spiderUserAgent == null ) {
            spiderUserAgent = "";
        }
        String spiderUserAgentLowerCase = spiderUserAgent.toLowerCase();
        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            if (line.toLowerCase().startsWith(USER_AGENT)) {
                String userAgent = line.substring(USER_AGENT.length() + 1).trim();
                if (userAgent.equals("*") || spiderUserAgentLowerCase.indexOf(userAgent.toLowerCase()) > -1) {
                    return userAgent;
                }
            }
            line = br.readLine();
        }
        return null;
    }

    private static RobotsTXTLine[] parseRules ( BufferedReader br ) throws IOException {
        ArrayList al = new ArrayList();
        String line = br.readLine();
        while (line != null && (line.toLowerCase().indexOf(USER_AGENT) == -1)) {
            RobotsTXTLine robotsTXTline = RobotsTXTLine.parse(line);
            if ( robotsTXTline != null && robotsTXTline.getType() == RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW ) {
                al.add ( robotsTXTline );
            }
            line = br.readLine();
        }
        return (RobotsTXTLine[]) al.toArray(new RobotsTXTLine[al.size()]);
    }

}
