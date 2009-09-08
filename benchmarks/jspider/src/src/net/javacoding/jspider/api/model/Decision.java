package net.javacoding.jspider.api.model;


/**
 *
 * $Id: Decision.java,v 1.1 2003/03/09 09:25:22 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Decision {

    public static final int RULE_DONTCARE = 0;
    public static final int RULE_ACCEPT = 1;
    public static final int RULE_IGNORE = 2;
    public static final int RULE_FORBIDDEN = 3;

    public boolean isVetoable();

    public int getDecision();

    public String getComment();

    public void addStep ( String rule, int ruleType, int type, String comment );

    public void merge(Decision other);

    public void change ( Decision other );

    public DecisionStep[] getSteps ( );

}
