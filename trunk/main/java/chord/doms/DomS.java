/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramDom;
import chord.visitors.IMethodVisitor;

/**
 * Domain of method signatures.
 * <p>
 * A method signature is a string of the form <tt>m(t1,...,tn)</tt> where
 * <tt>m</tt> is the name of the method and <tt>t1</tt>, ..., <tt>tn</tt>
 * are the fully-qualified names of the types of its arguments in order,
 * including the <tt>this</tt> argument in the case of instance methods.
 * <p>
 * Examples are <tt>main(java.util.String[])</tt> and
 * <tt>equals(java.util.String,java.lang.Object)</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "S"
)
public class DomS extends ProgramDom<String>
		implements IMethodVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		set(Program.getSign(m));
	}
}
