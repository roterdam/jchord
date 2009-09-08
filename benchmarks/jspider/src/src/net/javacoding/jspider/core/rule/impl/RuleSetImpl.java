package net.javacoding.jspider.core.rule.impl;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.model.DecisionInternal;
import net.javacoding.jspider.spi.Rule;
import net.javacoding.jspider.core.rule.Ruleset;

import java.net.URL;
import java.util.List;


/**
 *
 * $Id: RuleSetImpl.java,v 1.9 2003/04/03 16:24:59 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RuleSetImpl implements Ruleset {

    protected int type;
    protected Ruleset generalRules;
    protected List localRules;

    public RuleSetImpl(int type, List rules) {
        this(type, null, rules);
    }

    public RuleSetImpl(int type, Ruleset generalRules, List rules) {
        this.type = type;
        this.generalRules = generalRules;
        this.localRules = rules;
    }

    public Decision applyRules(SpiderContext context, Site site, URL url) {
        Decision decision = null;

        if (generalRules != null) {
            decision = generalRules.applyRules(context, site, url);
        } else {
            decision = new DecisionInternal();
        }

        if (decision.isVetoable()) {

            Rule[] rules = (Rule[]) localRules.toArray(new Rule[localRules.size()]);
            for (int i = 0; i < rules.length; i++) {
                Rule rule = rules[i];
                Decision lastDecision = rule.apply(context, site, url);
                decision.addStep(rule.getName(), type, lastDecision.getDecision(), lastDecision.getComment() );
                decision.merge(lastDecision);

                if (!lastDecision.isVetoable()) {
                    break;
                }
            }
        }

        decision.addStep("Ruleset",type, decision.getDecision(), "ruleset final decision");

        return decision;
    }

    public void addRule(Rule rule) {
        localRules.add(rule);
    }

    public int getType() {
        return type;
    }

    public String toString ( ) {
        StringBuffer sb = new StringBuffer ( );
        sb.append ( "[" );
        sb.append (translate(type));
        sb.append ( " ruleset]");
        return sb.toString ( );
    }

    public static String translate ( int type ) {
        switch(type){
            case Ruleset.RULESET_GENERAL:
                return "GENERAL";
            case Ruleset.RULESET_SITE:
                return "SITE";
        }
        return "ERROR!";
    }


}
