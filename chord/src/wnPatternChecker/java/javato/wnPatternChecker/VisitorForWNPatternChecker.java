package javato.wnPatternChecker;


import soot.SootMethod;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.util.Chain;
import javato.hybridracedetection.VisitorForHybridRaceChecking;
import javato.instrumentor.Visitor;
import javato.instrumentor.contexts.InvokeContext;
import javato.instrumentor.contexts.RHSContextImpl;
import javato.instrumentor.contexts.RefContext;

/**
 * Copyright (c) 2007-2008
 * Pallavi Joshi	<pallavi@cs.berkeley.edu>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class VisitorForWNPatternChecker extends VisitorForHybridRaceChecking {

	public VisitorForWNPatternChecker(Visitor visitor) {
		super(visitor);
	}

	//we do not need to instrument calls to myRead or myWrite as in VisitorForHybridRaceChecking
	//we just need to instrument calls to myReadAfter and myWriteAfter
	
	public void visitArrayRef(SootMethod sm, Chain units, Stmt s, ArrayRef arrayRef, RefContext context) {
        if (context == RHSContextImpl.getInstance()) {
            addCallWithObjectInt(units, s, "myReadAfter", arrayRef.getBase(), arrayRef.getIndex(), false);
        } else {
            addCallWithObjectInt(units, s, "myWriteAfter", arrayRef.getBase(), arrayRef.getIndex(), false);
        }
        nextVisitor.visitArrayRef(sm, units, s, arrayRef, context);
    }

    public void visitInstanceFieldRef(SootMethod sm, Chain units, Stmt s, InstanceFieldRef instanceFieldRef, RefContext context) {
        if (!sm.getName().equals("<init>") || !instanceFieldRef.getField().getName().equals("this$0")) {
            Value v = IntConstant.v(st.get(instanceFieldRef.getField().getName()));
            if (context == RHSContextImpl.getInstance()) {
                addCallWithObjectInt(units, s, "myReadAfter", instanceFieldRef.getBase(), v, false);
            } else {
                addCallWithObjectInt(units, s, "myWriteAfter", instanceFieldRef.getBase(), v, false);
            }
        }
        nextVisitor.visitInstanceFieldRef(sm, units, s, instanceFieldRef, context);
    }


    public void visitStaticFieldRef(SootMethod sm, Chain units, Stmt s, StaticFieldRef staticFieldRef, RefContext context) {
        Value v1 = IntConstant.v(st.get(staticFieldRef.getField().getDeclaringClass().getName()));
        Value v2 = IntConstant.v(st.get(staticFieldRef.getField().getName()));
        if (context == RHSContextImpl.getInstance()) {
            addCallWithIntInt(units, s, "myReadAfter", v1, v2, false);
        } else {
            addCallWithIntInt(units, s, "myWriteAfter", v1, v2, false);
        }
        nextVisitor.visitStaticFieldRef(sm, units, s, staticFieldRef, context);
    }
    
    public void visitInstanceInvokeExpr(SootMethod sm, Chain units, Stmt s, InstanceInvokeExpr invokeExpr, InvokeContext context) {
        Value base = invokeExpr.getBase();
        String sig = invokeExpr.getMethod().getSubSignature();
        if (sig.equals("void wait()")) {
            addCallWithObject(units, s, "myWaitBefore", base, true);
        } else if (sig.equals("void notify()")) {
            addCallWithObject(units, s, "myNotifyAfter", base, false);
        } else if (sig.equals("void notifyAll()")) {
            addCallWithObject(units, s, "myNotifyAllAfter", base, false);
        } else if (sig.equals("void start()") && isThreadSubType(invokeExpr.getMethod().getDeclaringClass())) {
            addCallWithObject(units, s, "myStartBefore", base, true);
        } else if (sig.equals("void join()") && isThreadSubType(invokeExpr.getMethod().getDeclaringClass())) {
            addCallWithObject(units, s, "myJoinAfter", base, false);
        } 
        nextVisitor.visitInstanceInvokeExpr(sm, units, s, invokeExpr, context);
    }
	
}
