/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.reflect;

import java.util.*;
import chord.program.ClassHierarchy;
import chord.program.Program;
import chord.rels.RelScopeExcludedM;
import chord.util.ArraySet;
import chord.util.IndexSet;
import chord.util.tuple.object.Pair;
import joeq.Class.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.*;
import joeq.Compiler.Quad.Operator.Invoke.*;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class CastBasedStaticReflect extends StaticReflectResolver {
  
  ClassHierarchy ch;
  IndexSet<jq_Reference> reachableAllocClasses;

  // Set of methods containing a "return v" statement where v is in
  // set newInstVars.
  private Map<jq_Method, Set<Quad>> reflectRetMeths;
  private Map<Register, Set<Quad>> newInstReflSites;
  
  public CastBasedStaticReflect(IndexSet<jq_Reference> reachableAllocClasses) {
    System.out.println("CastBasedStaticReflect in use");

    ch =  Program.g().getClassHierarchy();
    newInstReflSites = new HashMap<Register, Set<Quad>>();
    reflectRetMeths = new HashMap<jq_Method, Set<Quad>>();
    this.reachableAllocClasses = reachableAllocClasses;
  }
  @Override
  public void run(jq_Method m) {
    resolvedClsForNameSites.clear();
    resolvedObjNewInstSites.clear();
    cfg = m.getCFG();
    initForNameAndNewInstSites();
//    System.out.println("processing " + m.getName() + " found " + )
    numArgs = m.getParamTypes().length;

    if (!forNameSites.isEmpty()) {
      resolveForNameSites();
    }
    resolveNewInstSites();
  }
  
 
  
  @Override
  protected void processCheckCast(Register l, Register r, Quad q) {
    jq_Reference t = (jq_Reference) CheckCast.getType(q).getType();

    if (newInstReflSites.containsKey(r) && ! RelScopeExcludedM.isOutOfScope(t.getName())) {  
  //      System.out.println("processCast found a newInstVar");
      Set<String> subs = ch.getConcreteSubclasses(t.getName());
      if (subs != null) {
        for (String s : subs) {
          jq_Class d = (jq_Class) jq_Type.parseType(s);
          
          if(!RelScopeExcludedM.isOutOfScope(s)) {
            Set<Quad> allocSites = newInstReflSites.get(r);
            if(allocSites != null)
              for(Quad allocSite: allocSites) //where was r allocated?
                resolvedObjNewInstSites.add(new Pair<Quad, jq_Reference>(allocSite, d));
          }
        }
      }
    }
  }
  
  protected void processCopy(Register l, Register r) {
    super.processCopy(l, r);
    Set<Quad> allocSites = newInstReflSites.get(r);
    if(allocSites != null) {
      Set<Quad> l_allocSites = newInstReflSites.get(l);
      if(l_allocSites != null) {
        if(l_allocSites.addAll(allocSites)) {
          changed = true; 
        }
         //don't set changed; we already processed this copy adequately
      } else { //l_allocSites was null, so the copy is changing something.
        changed = true;
        newInstReflSites.put(l, allocSites);
      }
    }  
  }
  
  @Override
  protected void processInvoke(Quad q) {
    jq_Method caller = q.getMethod();
//    System.out.println("CastBasedStaticReflect.processInvoke on " + caller);
    
    RegisterOperand retOp = Invoke.getDest(q);
    if(retOp == null) {
      return;
    }
    Set<Quad> oldS = newInstReflSites.get(retOp.getRegister());

    if(newInstSites.contains(q)) {
      if(oldS == null) {
        ArraySet<Quad> set = new ArraySet<Quad>();
        set.add(q);
        newInstReflSites.put(retOp.getRegister(), set);
        changed = true;
      } else if(oldS.add(q))
        changed = true;
      return;
    }
    
    jq_Method summarizedCallee = Invoke.getMethod(q).getMethod();
    Set<Quad> allocSites = reflectRetMeths.get(summarizedCallee);
    
    Operator op = q.getOperator();
    if(op instanceof InvokeVirtual || op instanceof InvokeInterface) {
      if(allocSites == null)
        allocSites = new LinkedHashSet<Quad>();
      jq_NameAndDesc nd = summarizedCallee.getNameAndDesc();
  
//      for(String cl: ch.getConcreteSubclasses(summarizedCallee.getDeclaringClass().getName())) {
 //       System.out.println("call might reach method in " + cl);
  //      jq_Type clR = jq_Reference.parseType(cl);
  //      if(reachableAllocClasses.contains(clR)) {
      jq_Class rawCalledClass = summarizedCallee.getDeclaringClass();
      boolean isInterface = rawCalledClass.isInterface();
      for(jq_Reference cl: reachableAllocClasses) {
//        boolean matches = isInterface ? cl.implementsInterface(rawCalledClass) : cl.isSubtypeOf(rawCalledClass);
        boolean matches = (cl instanceof jq_Class) && cl.isSubtypeOf(rawCalledClass);
        if(matches) {
          jq_Method targMeth = cl.getVirtualMethod(nd);
          if(targMeth == null)
            continue;
          Set<Quad> returnedASites = reflectRetMeths.get(targMeth);
          if(returnedASites != null)
            allocSites.addAll(returnedASites);
        }
      }
    }
    if(allocSites == null)
      return;

    if(oldS == null) {
      newInstReflSites.put(retOp.getRegister(),allocSites);
      changed = true;
    } else if(oldS.addAll(allocSites))
      changed = true;
  }
  
  protected void processReturn(Quad q) {
    jq_Method returningM = cfg.getMethod();
    Operand ro = Return.getSrc(q);
    if (ro instanceof RegisterOperand) {
      Register r = ((RegisterOperand) ro).getRegister();
      Set<Quad> allocSites = newInstReflSites.get(r);
      Set<Quad> oldAllocSites = reflectRetMeths.get(returningM);

      if(allocSites != null && (oldAllocSites == null || !oldAllocSites.contains(allocSites)))  {
        propagatedAReturn = true;
        if(DEBUG)
          System.out.println("method " + returningM + " has reflective return value");
        reflectRetMeths.put(returningM, allocSites);
      }
    }
  }
  
  boolean propagatedAReturn = false;
  /**
   * Used to cue RTA to do a new iteration
   * @return
   */
    public boolean needNewIter() {
      return propagatedAReturn;
    }
    
    public void startedNewIter() {
      propagatedAReturn = false;
    }
}
