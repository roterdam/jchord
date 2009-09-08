package net.javacoding.jspider.core.storage;

import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Resource;

import java.net.URL;

/**
 * $Id: DecisionDAO.java,v 1.1 2003/04/04 20:06:10 vanrogu Exp $
 */
public interface DecisionDAO {

    public void saveSpiderDecision ( Resource resource, Decision decision );

    public void saveParseDecision ( Resource resource, Decision decision );

    public Decision findSpiderDecision ( Resource resource );

    public Decision findParseDecision ( Resource resource );
}
