package chord.analyses.typestate;

import chord.util.ArraySet;
import joeq.Compiler.Quad.Quad;
/***
 * This is abstract state that will be maintained for each heap object
 * 
 * @author machiry
 *
 */
public class AbstractState {
	Quad alloc;
	TypeState curentState;
	ArraySet<AccessPath> mustSet;
	boolean may;
}

