package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.DecisionStep;
import net.javacoding.jspider.core.rule.impl.RuleSetImpl;

/**
 * $Id: DecisionStepInternal.java,v 1.3 2003/04/04 20:06:06 vanrogu Exp $
 */
public class DecisionStepInternal implements DecisionStep {

    protected int ruleType;
    protected int decision;
    protected String comment;
    protected String rule;

    public DecisionStepInternal ( String rule, int ruleType, int decision, String comment) {
        this.rule = rule;
        this.ruleType = ruleType;
        this.decision = decision;
        this.comment = comment;
    }

    public String getRule ( ) {
        return rule;
    }

    public int getRuleType() {
        return ruleType;
    }

    public int getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public String toString ( ) {
        return "[DecisionStep] " + RuleSetImpl.translate(ruleType) + " rule " + rule + " - " + DecisionInternal.translate(decision) + " - " + comment;
    }

}
