/*
 * Copyright (c) 2010, Ariel Rabkin and Intel Corporation.
 * You may use and modify this code, subject to the license agreement 
 * in the "COPYING" file associated with this project.
 * 
 */

package chord.rels;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import chord.program.ClassHierarchy;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "MentryPoints",
    sign = "M0:M0"
  )
/**
 * A relation over domain M containing additional entry points for the program.
 * The values of this relation are derived from the file indicated by property 
 * chord.entrypoints.file.
 * 
 * File should be a list whose entries are class names, interface names, or fully qualified method names.
 *  (A fully qualified method name is of the form <name>:<desc>@<classname>.)
 *  If a class is listed, all public methods of that class will be added as entry points.
 *  If an interface is listed, all public methods of all concrete instances of that interface will
 *  be added as entry points.
 */
public class RelExtraEntryPoints extends ProgramRel {
  
  public static String extraMethodsFile  = System.getProperty("chord.entrypoints.file");
  static LinkedHashSet<jq_Method> methods;
   
  @Override
  public void fill() {
    Iterable<jq_Method> publicMethods =  slurpMList(Program.g().getClassHierarchy());

    for(jq_Method m: publicMethods) {
      super.add(m);
    }
  }
  
  
  
  public static Iterable<jq_Method> slurpMList(ClassHierarchy ch) {
    
    if(methods == null)
       methods = new LinkedHashSet<jq_Method>();
    else
      return methods;
    
    if(extraMethodsFile == null)
      return methods;

    String s = null;
    try {
    
    BufferedReader br = new BufferedReader(new FileReader(extraMethodsFile));
    while( (s = br.readLine()) != null) {
      if(s.startsWith("#"))
        continue;
      
      try { 
      if(s.contains("@")) {
        //s is a method.
        
        int strudelPos = s.indexOf('@');
        int colonPos = s.indexOf(':');
        if(strudelPos > colonPos && colonPos > 0) {
          String cName = s.substring(strudelPos+1);
          String mName = s.substring(0, colonPos);
          String mDesc = s.substring(colonPos+1, strudelPos);

          jq_Class parentClass  =  (jq_Class) jq_Type.parseType(cName);
          
          parentClass.prepare();  
          jq_Method m = (jq_Method) parentClass.getDeclaredMember(mName, mDesc);
          methods.add(m);
        }
      } else { //s is a class name
        
        jq_Class pubI  =  (jq_Class) jq_Type.parseType(s);
        
        if(pubI == null) {
          System.err.println("ERR: no such class " + s );
          continue;
        } else
          pubI.prepare();  
        
        //two cases: pubI is an interface/abstract class or pubI is a concrete class.
        if(pubI.isInterface() || pubI.isAbstract()) {  
          Set<String> impls =  
//              pubI.isInterface() ? ch.getConcreteImplementors(pubI.getName()) : ch.getConcreteSubclasses(pubI.getName());
            ch.getConcreteSubclasses(pubI.getName());
          if(impls == null) {
            System.err.println("ExtraEntryPoints: found no concrete impls or subclasses of " + pubI.getName());
            continue;
          }
          
          for(String impl:impls) {
            jq_Class implClass = (jq_Class) jq_Type.parseType(impl);
            implClass.prepare();
            for(jq_Method ifaceM:   pubI.getDeclaredInstanceMethods()) {
              
              jq_Class implementingClass = implClass;
              while(implementingClass != null) {
                jq_Method implM = implementingClass.getDeclaredInstanceMethod(ifaceM.getNameAndDesc());
                if(implM != null) {
                  methods.add(implM);
                  break;
                } else {
                  implementingClass = implementingClass.getSuperclass();
                  implementingClass.prepare();
                }
              }

            }
          }
          /* } else if(pubI.isAbstract()) {
          
          for(jq_Method m: pubI.getDeclaredStaticMethods()) {
            if(!m.isPrivate()) {
              methods.add(m);
            }
          }

          System.out.println(pubI.getName() + " has " + subclasses.length + " subclasses in scope");
          for(jq_Class implClass: subclasses) {
            System.out.println("adding methods of " + implClass.getName() + " since it's a subclass of " + pubI.getName());
            for(jq_Method implM:   implClass.getDeclaredInstanceMethods()) {
              System.out.println("\t" + implM.getNameAndDesc());
//              jq_Method implM = implClass.getDeclaredInstanceMethod(ifaceM.getNameAndDesc());
              methods.add(implM);
            }
          }*/
        } else { //class is concrete
          for(jq_Method m: pubI.getDeclaredInstanceMethods()) {
            if(!m.isPrivate()) {
              methods.add(m);
            }
          }
          
          for(jq_Method m: pubI.getDeclaredStaticMethods()) {
            if(m.isPublic()) {
              methods.add(m);
            }
          }
        }
      }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    
    br.close();
    } catch(IOException e) {
      e.printStackTrace();
    } catch(NoClassDefFoundError e) {
//      System.err.println("no such class " + s);
      e.printStackTrace();
    }
    return methods;
  }

}
