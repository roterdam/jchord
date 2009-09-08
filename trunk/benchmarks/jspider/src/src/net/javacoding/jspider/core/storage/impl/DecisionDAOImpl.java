package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.core.storage.DecisionDAO;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.storage.spi.DecisionDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.model.DecisionInternal;
import net.javacoding.jspider.core.model.ResourceInternal;
import net.javacoding.jspider.api.model.Resource;
import net.javacoding.jspider.api.model.Decision;

/**
 * $Id: DecisionDAOImpl.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
class DecisionDAOImpl implements DecisionDAO {

    protected Log log;
    protected StorageSPI storage;
    protected DecisionDAOSPI spi;

    public DecisionDAOImpl ( Log log, StorageSPI storage, DecisionDAOSPI spi ) {
        this.log = log;
        this.storage = storage;
        this.spi = spi;
    }

    public void saveSpiderDecision(Resource resource, Decision decision) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        DecisionInternal di = TypeTranslator.translate(decision);
        spi.saveSpiderDecision(ri, di);
    }

    public void saveParseDecision(Resource resource, Decision decision) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        DecisionInternal di = TypeTranslator.translate(decision);
        spi.saveParseDecision(ri,di);
    }

    public Decision findSpiderDecision(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.findSpiderDecision(ri);
    }

    public Decision findParseDecision(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.findParseDecision(ri);
    }
}
