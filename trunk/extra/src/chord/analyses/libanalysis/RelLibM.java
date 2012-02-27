/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.libanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.program.Program;
import chord.program.visitors.IClassVisitor;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.Messages;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * Relation containing each (concrete or abstract) class type
 * (as opposed to interface types, primitive types, etc.).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "libM",
	sign = "M0"
)
public class RelLibM extends ProgramRel implements IMethodVisitor {
	
	private Map<String, String> map = new HashMap<String, String>();
	

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		String cName = m.getDeclaringClass().getName();
		for (String c : Config.scopeExcludeAry) {
			if (cName.startsWith(c))
				add(m);
		}
	}
	
	public String getCompleteName(jq_Method m){
		String sign = m.getName().toString() + ":" + m.getDesc().toString() +
				"@" + m.getDeclaringClass().getName();
		return sign;
	}
}
