package net.javacoding.jspider.core.rule.impl;

import junit.framework.TestCase;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.core.model.DecisionInternal;

/**
 * $Id: DecisionImplTest.java,v 1.2 2003/03/09 09:25:23 vanrogu Exp $
 */
public class DecisionImplTest extends TestCase {

    public DecisionImplTest ( ) {
        super ( "DecisionImplTest" );
    }

    public void testDefaultDecision ( ) {
        DecisionInternal di = new DecisionInternal();
        int decision = di.getDecision();
        int expected = Decision.RULE_DONTCARE;
        assertEquals("default decision is not DONTCARE", expected, decision);
    }

    public void testVetoableDefault ( ) {
        DecisionInternal di = new DecisionInternal();
        boolean vetoable = di.isVetoable();
        assertTrue("default decision is not vetoable", vetoable);
    }

    public void testVetoableDontCare ( ) {
        DecisionInternal di = new DecisionInternal(Decision.RULE_DONTCARE);
        boolean vetoable = di.isVetoable();
        assertTrue("dontcare decision is not vetoable", vetoable);
    }

    public void testVetoableAccept ( ) {
        DecisionInternal di = new DecisionInternal(Decision.RULE_ACCEPT);
        boolean vetoable = di.isVetoable();
        assertTrue("accept decision is not vetoable", vetoable);
    }

    public void testVetoableIgnore ( ) {
        DecisionInternal di = new DecisionInternal(Decision.RULE_IGNORE);
        boolean vetoable = di.isVetoable();
        assertFalse("accept decision is vetoable", vetoable);
    }

    public void testVetoableForbidden( ) {
        DecisionInternal di = new DecisionInternal(Decision.RULE_FORBIDDEN);
        boolean vetoable = di.isVetoable();
        assertFalse("forbidden decision is vetoable", vetoable);
    }

    public void testMergeDontCareDontCare ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_DONTCARE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_DONTCARE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_DONTCARE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeDontCareAccept ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_DONTCARE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_ACCEPT);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_ACCEPT;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeDontCareIgnore ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_DONTCARE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_IGNORE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_IGNORE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeDontCareForbidden( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_DONTCARE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeAcceptDontCare ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_ACCEPT);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_DONTCARE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_ACCEPT;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeAcceptAccept ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_ACCEPT);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_ACCEPT);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_ACCEPT;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeAcceptIgnore ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_ACCEPT);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_IGNORE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_IGNORE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeAcceptForbidden( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_ACCEPT);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeIgnoreDontCare ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_IGNORE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_DONTCARE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_IGNORE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeIgnoreAccept ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_IGNORE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_ACCEPT);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_IGNORE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeIgnoreIgnore ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_IGNORE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_IGNORE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_IGNORE;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeIgnoreForbidden( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_IGNORE);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeForbiddenDontCare ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_DONTCARE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeForbiddenAccept ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_ACCEPT);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeForbiddenIgnore ( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_IGNORE);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }

    public void testMergeForbiddenForbidden( ) {
        DecisionInternal d1 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        DecisionInternal d2 = new DecisionInternal(Decision.RULE_FORBIDDEN);
        d1.merge(d2);
        int decision = d1.getDecision();
        int expected = Decision.RULE_FORBIDDEN;
        assertEquals ( "merge didn't work correctly", expected, decision );
    }


}
