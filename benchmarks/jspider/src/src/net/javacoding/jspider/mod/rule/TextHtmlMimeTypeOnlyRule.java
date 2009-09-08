package net.javacoding.jspider.mod.rule;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.rule.impl.BaseRuleImpl;
import net.javacoding.jspider.core.model.DecisionInternal;

import java.net.URL;

/**
 * $Id: TextHtmlMimeTypeOnlyRule.java,v 1.2 2003/05/02 17:36:59 vanrogu Exp $
 */
public class TextHtmlMimeTypeOnlyRule extends BaseRuleImpl {

    public Decision apply(SpiderContext context, Site currentSite, URL url) {
        FetchedResource resource = (FetchedResource)context.getStorage().getResourceDAO().getResource(url);
        String mime = resource.getMime();
        Decision decision = new DecisionInternal(Decision.RULE_IGNORE, "mimetype is '" + mime + "' - resource ignored");

        if ( mime == null ) {
            decision = new DecisionInternal(Decision.RULE_ACCEPT, "mimetype is null - defaulted to text/html - accepted" );
        } else if (mime.toLowerCase().indexOf("text/html") > -1) { // can also contain charset info
            decision = new DecisionInternal(Decision.RULE_ACCEPT, "mimetype is '" + mime + "' - resource accepted");
        }

        return decision;
    }
}
