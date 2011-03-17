package chord.analyses.invk;

import chord.analyses.method.DomM;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Used to rewrite method calls, to facilitate stub implementations or
 * analyzable models.
 * If property chord.methodRemapFile is set, will read a map from that file.
 * Format is source-method dest-method, separated by a space.
 * Both source and dest should be fully qualified method names, of the form
 * methodName:desc@declaringClass
 * 
 * Blank lines and lines starting with a # are ignored as comments.
 * 
 * Note that for virtual function calls, the rewrite happens AFTER
 * the call target is resolved. So if you have a stub implementation for
 * Derived.foo, then a call to Base.foo on an instance of Derived should
 * call the stub.
 * 
 * Be careful about the prototype for the function being mapped; the remap
 * will fail with a warning message if any details do not match.
 * 
 * Note also that there is no checking performed that the old and new functions
 * have the compatible prototypes. Arguments and return values may wind up
 * not propagating correctly if, e.g., a 2-argument function is remapped
 * to a 3-argument function.
 *
 */
public class StubRewrite {
	
	private static jq_Method getMeth(String clName, String methname, String desc) {
//		clName = clName.replace('$', '.');
		jq_Class cl = (jq_Class) jq_Type.parseType(clName);
		cl.prepare();

		return (jq_Method) cl.getDeclaredMember(methname,desc);
	}
	
	private static HashMap<jq_Method,jq_Method> lookupTable; //initialized only once
	
	public static jq_Method maybeReplaceCallDest(jq_Method m) {
		jq_Method replacement = lookupTable.get(m);
		if(replacement == null)
			return m;
		else return replacement;
	}
	
	static Pattern linePat = Pattern.compile("([^:]*):([^@]*)@([^ ]*) ([^:]*):([^@]*)@([^ ]*)");

	static {
		init();
	}
	
	public static void init() {
		if(lookupTable != null)
			return;
		else
			lookupTable = new LinkedHashMap<jq_Method,jq_Method>();
		try {
			String fileName = System.getProperty("chord.methodRemapFile");
			
			if(fileName == null)
				return;
			
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String ln = null;
			while((ln = br.readLine()) != null) {
				if(ln.length() < 1 || ln.startsWith("#"))
					continue;
				
				Matcher match = linePat.matcher(ln);
				if(!match.find())
					System.out.println("WARN: StubRewrite couldn't parse line "+ ln);
				else {
					String srcMethName = match.group(1);
					String srcDesc = match.group(2);
					String srcClassName = match.group(3);

					String destMethName = match.group(4);
					String destDesc = match.group(5);
					String destClassName = match.group(6);
//					if(ln.indexOf(' ') != ln.lastIndexOf(' ')) //two spaces

					jq_Method src = getMeth(srcClassName, srcMethName, srcDesc);
					jq_Method dest = getMeth(destClassName, destMethName, destDesc);
					if(src != null && dest != null) {
						//can do more checks here, for e.g., arity matching
						lookupTable.put(src, dest);
						System.out.println("StubRewrite mapping "+ srcClassName + "." + srcMethName + " to " + destClassName+"."+destMethName);
					} else {
						if(src == null)
							System.out.println("WARN: StubRewrite failed to map "+ srcClassName + "." + srcMethName+", couldn't resolve source");
						else
							System.out.println("WARN: StubRewrite failed to map "+ destClassName + "." + destMethName+ " " + destDesc+" -- couldn't resolve dest");
					}
				}
			}
			
			br.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void addNewDests(Collection<jq_Method> publicMethods) {
		publicMethods.addAll(lookupTable.values());
	}

}
