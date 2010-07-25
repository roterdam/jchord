/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.instr;

import java.util.Map;
import java.util.HashMap;

import javassist.NotFoundException;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;
import javassist.expr.NewArray;
import javassist.expr.ArrayAccess;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.MonitorEnter;
import javassist.expr.MonitorExit;
import javassist.CtClass;

import chord.project.Messages;
import chord.project.Config;

/**
 * Bytecode instrumentor providing hooks for transforming classes,
 * methods, and instructions.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class BasicInstrumentor extends ExprEditor {
	private static final String EXPLICITLY_EXCLUDING_CLASS =
		"WARN: Not instrumenting class %s as it is excluded by chord.scope.exclude.";
	private static final String IMPLICITLY_EXCLUDING_CLASS =
		"WARN: Not instrumenting class %s.";
	protected boolean verbose;
	protected final JavassistPool pool;
	protected String[] scopeExcludeAry;
	protected Map<String, String> argsMap;

	// called by online transformer
	// argsMap contains (key,value) pairs passed to online transformer agent 
	public BasicInstrumentor(Map<String, String> argsMap) {
		this.argsMap = argsMap;
		scopeExcludeAry = Config.scopeExcludeAry;
 		verbose = Config.verbose;
		String mainClassPathName = Config.mainClassPathName;
		String userClassPathName = Config.classPathName;
		pool = new JavassistPool(mainClassPathName, userClassPathName);
	}

	// called by offline transformer
	// argsMap will be null
	public BasicInstrumentor() {
		this(null);
    }

    public JavassistPool getPool() {
        return pool;
    }

	public boolean isExplicitlyExcluded(String cName) {
		for (String s : scopeExcludeAry) {
			if (cName.startsWith(s))
				return true;
		}
		return false;
	}
	
	public boolean isImplicitlyExcluded(String cName) {
		return cName.equals("java.lang.J9VMInternals") ||
			cName.startsWith("sun.reflect.Generated") ||
			cName.startsWith("java.lang.ref.");
	}

	public boolean isExcluded(String cName) {
		if (isImplicitlyExcluded(cName)) {
			if (verbose) Messages.log(IMPLICITLY_EXCLUDING_CLASS, cName);
			return true;
		}
		if (isExplicitlyExcluded(cName)) {
			if (verbose) Messages.log(EXPLICITLY_EXCLUDING_CLASS, cName);
			return true;
		}
		return false;
	}

	public CtClass edit(String cName) throws NotFoundException, CannotCompileException {
		if (isExcluded(cName))
			return null;
		CtClass clazz = pool.get(cName);
		return edit(clazz);
	}

	public CtClass edit(CtClass clazz) throws CannotCompileException {
		CtBehavior clinit = clazz.getClassInitializer();
		if (clinit != null)
			edit(clinit);
        CtBehavior[] inits = clazz.getDeclaredConstructors();
        CtBehavior[] meths = clazz.getDeclaredMethods();
        for (CtBehavior m : inits)
			edit(m);
		for (CtBehavior m : meths)
			edit(m);
		return clazz;
	}

	public void edit(CtBehavior method) throws CannotCompileException {
		method.instrument(this);
	}

    public String insertBefore(int pos) {
		return null;
	}

	public void edit(NewExpr e) throws CannotCompileException { }

	public void edit(NewArray e) throws CannotCompileException { }

	public void edit(FieldAccess e) throws CannotCompileException { }

	public void edit(ArrayAccess e) throws CannotCompileException { }

	public void edit(MonitorEnter e) throws CannotCompileException { }

	public void edit(MonitorExit e) throws CannotCompileException { }

	public void edit(MethodCall e) throws CannotCompileException { }
}

