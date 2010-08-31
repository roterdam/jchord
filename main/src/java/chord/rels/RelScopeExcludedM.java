/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import joeq.Class.jq_Method;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;

/**
 * @author Ariel Rabkin (asrabkin@gmail.com)
 */
@Chord(
    name = "scopeExcludedM",
    sign = "M0:M0"
  )
public class RelScopeExcludedM extends ProgramRel {

  public static boolean isOutOfScope(String cName) {
    String[] scopeExcludeAry = Config.scopeExcludeAry;
    for (String prefix : scopeExcludeAry) {
      if (cName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
  
  public static boolean isOutOfScope(jq_Method m) {
    String cName = m.getDeclaringClass().getName();
    return isOutOfScope(cName);
  }
  
  public void fill() {
    Program program = Program.g();
    IndexSet<jq_Method> methods = program.getMethods();
    for(jq_Method m: methods) {
      if(isOutOfScope(m))
        add(m);
    }
  }
}

