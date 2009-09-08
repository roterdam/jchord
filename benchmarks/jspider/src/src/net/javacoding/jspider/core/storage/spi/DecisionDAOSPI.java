package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.core.model.ResourceInternal;
import net.javacoding.jspider.core.model.DecisionInternal;


/**
 * $Id: DecisionDAOSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 */
public interface DecisionDAOSPI {

    public void saveSpiderDecision ( ResourceInternal resource, DecisionInternal decision );

    public void saveParseDecision ( ResourceInternal resource, DecisionInternal decision );

    public DecisionInternal findSpiderDecision ( ResourceInternal resource );

    public DecisionInternal findParseDecision ( ResourceInternal resource );

}
