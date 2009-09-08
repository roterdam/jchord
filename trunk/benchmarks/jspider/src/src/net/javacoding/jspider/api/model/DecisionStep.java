package net.javacoding.jspider.api.model;

/**
 * Interface to be implemented upon classes that will represent a
 * DesisionStep, a step in the process of the decision taking on a
 * resource URL whether it should be spidered and/or parsed.
 *
 * $Id: DecisionStep.java,v 1.2 2003/04/04 20:06:02 vanrogu Exp $
 *
 * @author Günther Van Roey.
 */
public interface DecisionStep {

    /**
     * Determines whether it is a GENERAL rule or SITE local rule.
     * @return the type of the rule that took the decision
     */
    public int getRuleType ( );

    /**
     * Returns the name of the rule that took this decision step
     * @return the name of the rule that took this decision step
     */
    public String getRule ( );

    /**
     * The decision that was taken by the rule (DONTCARE, ACCEPT, IGNORE, FORBIDDEN)
     * @return the decision taken by the rule
     */
    public int getDecision ( );

    /**
     * Returns any comment or explanation given by the rule about it's explanation
     * @return comment on the decisionstep
     */
    public String getComment ( );

}
