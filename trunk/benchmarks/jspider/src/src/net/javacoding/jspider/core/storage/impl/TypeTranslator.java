package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.model.*;

/**
 * $Id: TypeTranslator.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
class TypeTranslator {

    public static ResourceInternal translate ( Resource resource ) {
        return (ResourceInternal) resource;
    }

    public static ResourceReferenceInternal translate ( ResourceReference resourceReference ) {
        return (ResourceReferenceInternal) resourceReference;
    }

    public static SiteInternal translate ( Site site ) {
        return (SiteInternal) site;
    }

    public static FolderInternal translate ( Folder folder ) {
        return (FolderInternal) folder;
    }

    public static DecisionInternal translate ( Decision decision ) {
        return (DecisionInternal) decision;
    }

    public static EMailAddressInternal translate ( EMailAddress address) {
        return (EMailAddressInternal) address;
    }

}
