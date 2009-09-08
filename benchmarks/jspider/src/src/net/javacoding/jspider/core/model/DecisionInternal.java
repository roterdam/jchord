package net.javacoding.jspider.core.model;


import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.api.model.DecisionStep;

import java.util.ArrayList;


/**
 *
 * $Id: DecisionInternal.java,v 1.1 2003/03/09 09:25:22 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DecisionInternal implements Decision {

    protected int type;
    protected String comment;
    protected ArrayList history;

    public DecisionInternal() {
        this(Decision.RULE_DONTCARE);
    }

    public DecisionInternal(int type) {
        this(type, "(no comment given)");
    }

    public DecisionInternal(int type, String comment) {
        this.type = type;
        this.comment = comment;
        this.history = new ArrayList();
    }

    public int getDecision() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public boolean isVetoable() {
        if (type == Decision.RULE_ACCEPT || type == Decision.RULE_DONTCARE) {
            return true;
        } else {
            return false;
        }
    }

    public void change ( Decision other ) {
        this.type = other.getDecision();
        this.comment = other.getComment();
    }

    public void merge(Decision other) {
        if (other.getDecision() > this.getDecision()) {
            this.change(other);
        }
    }

    public void addStep(String rule, int ruleType, int type, String comment) {
        history.add(new DecisionStepInternal(rule, ruleType, type, comment));
    }

    public DecisionStep[] getSteps() {
        return (DecisionStep[])history.toArray(new DecisionStep[history.size()]);
    }

    public String toString() {
        return "decision : " + translate ( type ) + " - " + getComment();
    }

    public static String translate ( int type ) {
        switch ( type ) {
            case Decision.RULE_ACCEPT:
                return "ACCEPT";
            case Decision.RULE_FORBIDDEN:
                return "FORBIDDEN";
            case Decision.RULE_IGNORE:
                return "IGNORE";
            case Decision.RULE_DONTCARE:
                return "DON'T CARE";
        }
        return "ERROR!!!";
    }

}
