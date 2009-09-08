package net.javacoding.jspider.core.storage.memory;

import net.javacoding.jspider.core.storage.spi.DecisionDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.ResourceInternal;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.util.Map;
import java.util.HashMap;

/**
 * $Id: DecisionDAOImpl.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 */
class DecisionDAOImpl implements DecisionDAOSPI {

    protected StorageSPI storage;

    protected Map spiderDecisions;
    protected Map parseDecisions;

    public DecisionDAOImpl ( StorageSPI storage ) {
        this.storage = storage;
        this.spiderDecisions = new HashMap ( );
        this.parseDecisions = new HashMap ( );
    }

    public void saveSpiderDecision(ResourceInternal resource, DecisionInternal decision) {
        spiderDecisions.put(new Integer(resource.getId()), decision);
    }

    public void saveParseDecision(ResourceInternal resource, DecisionInternal decision) {
        parseDecisions.put(new Integer(resource.getId()), decision);
    }

    public DecisionInternal findSpiderDecision(ResourceInternal resource) {
        return (DecisionInternal)spiderDecisions.get(new Integer(resource.getId()));
    }

    public DecisionInternal findParseDecision(ResourceInternal resource) {
        return (DecisionInternal)parseDecisions.get(new Integer(resource.getId()));
    }

}
