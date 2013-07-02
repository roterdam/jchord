package chord.analyses.inst;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p,v,h) such that the statement at program
 * point p is an object allocation statement h which assigns to local variable
 * v.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "PobjNullAsgnInst", sign = "P0,V0:P0_V0")
public class RelPobjNullAsgnInst extends ProgramRel {
	public void visit(jq_Class c) {
	}

	public void visit(jq_Method m) {
	}

	public void visitMoveInst(Quad q) {
		Operand rx = Move.getSrc(q);
		if (!(rx instanceof RegisterOperand)) {
			RegisterOperand lo = Move.getDest(q);
			if (lo.getType().isReferenceType()) {
				Register l = lo.getRegister();
				add(q, l);
			}
		}
	}
}
