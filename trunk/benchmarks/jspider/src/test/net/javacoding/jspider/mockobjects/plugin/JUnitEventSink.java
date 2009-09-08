package net.javacoding.jspider.mockobjects.plugin;

import net.javacoding.jspider.api.event.EventSink;
import net.javacoding.jspider.api.event.JSpiderEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * $Id: JUnitEventSink.java,v 1.7 2003/03/27 17:44:32 vanrogu Exp $
 */
public class JUnitEventSink implements EventSink {

    protected static JUnitEventSink instance;
    protected EventSink otherSink;

    protected Map counters;

    private JUnitEventSink ( ) {
        reset ( );
    }

    public static synchronized JUnitEventSink getInstance ( ) {
        if ( instance == null ) {
            instance = new JUnitEventSink();
        }
        return instance;
    }

    public void notify(JSpiderEvent event) {
        System.out.println("JUnit Plugin : " + event.getClass() );

        if ( event.isError() ) {
            System.out.println(event);
        }

        Integer count = (Integer)counters.get(event.getClass());
        if ( count == null ) {
            count = new Integer ( 0 );
        }
        counters.put ( event.getClass(), new Integer(count.intValue()+1) );
        if ( otherSink != null ) {
            otherSink.notify(event);
        }
    }

    public int getEventCount ( Class eventClass ) {
        Integer count = (Integer)counters.get(eventClass);
        if ( count == null ) {
            return 0;
        } else {
            return count.intValue();
        }
    }

    public void reset ( ) {
      this.counters = new HashMap ( );
      this.otherSink = null;
    }

    public void setOtherSink ( EventSink otherSink ) {
        this.otherSink = otherSink;
    }

    public void initialize() {
    }

    public void shutdown() {
    }
}
