/**
 * 
 */
package chord.program;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import chord.project.Messages;
import chord.project.Properties;
import joeq.Class.Classpath;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ClassHierarchy {
	// These members are for debugging purposes.
	private static final boolean DEBUG = false;
	
	private final Map<String, Set<String>> interface2implementors = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> class2subclasses = new HashMap<String, Set<String>>();
	private final Classpath classpath = new Classpath();
	private final String[] scopeExcludeAry;

	public ClassHierarchy() {
		this.scopeExcludeAry = Properties.scopeExcludeAry;
	}
	
	public void run() {
	}
	public Set<jq_Class> getImplementors(jq_Class interfaceType) {
		return null;
	}
	public Set<jq_Class> getSubclasses(jq_Class classType) {
		return null;
	}
/*
	public void run() {
        String systemClasspath = System.getProperty("sun.boot.class.path");
        for (Iterator it = PrimordialClassLoader.classpaths(systemClasspath); it.hasNext();) {
            String s = (String) it.next();
            PrimordialClassLoader.loader.addToClasspath(s);
        }
        String javaHomeDir = System.getProperty("java.home");
        assert (javaHomeDir != null);
        File libExtDir = new File(javaHomeDir, File.separator + "lib" + File.separator + "ext");
        if (libExtDir.exists()) {
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (name.endsWith(".jar"))
                        return true;
                    return false;
                }
            };
            File[] subFiles = libExtDir.listFiles(filter);
            for (File file : subFiles) {
                String fileName = file.getAbsolutePath();
                PrimordialClassLoader.loader.addToClasspath(fileName);
            }
        }
        String classpath = System.getProperty("java.class.path");
        for (Iterator it = PrimordialClassLoader.classpaths(classpath); it.hasNext();) {
            String s = (String) it.next();
            PrimordialClassLoader.loader.addToClasspath(s);
        }

		ClassHierarchy result = new ClassHierarchy();
		for (Iterator<String> it = listPackages(); it.hasNext(); ) {
			String packageName = it.next();
			result.loadTypes(packageName);
		}
		// We don't need <code>loadedTypes</code> anymore.
		result.loadedTypes = null;
		result.computeRelations();
		return result;
	}
	
	private void computeRelations() {
		if (DEBUG) {
			Messages.logAnon("CHA status msg: Started computing relations.");
		}
		for (int i=0; i<allTypes.size(); ++i) {
			jq_Type t = allTypes.get(i);
			if (t instanceof jq_Class) {
				jq_Class c = (jq_Class) t;
				for (jq_Class intrface : c.getInterfaces()) {
		        	int interfaceIndex = allTypes.indexOf(intrface);
		        	if (interfaceIndex >= 0) {
			        	TIntHashSet S = interface2implementors.get(interfaceIndex);
			        	if (S == null) {
			        		interface2implementors.put(interfaceIndex, S = new TIntHashSet());
			        	}
			        	S.add(i);
		        	} else {
		        		Messages.logAnon("CHA warning: Interface " + intrface + " not previously loaded!");
		        		loadType(intrface);
		        	}
		        }
		    	jq_Class superclass = c.getSuperclass();
		        while (superclass != null && superclass != PrimordialClassLoader.JavaLangObject) {
		        	int superclassIndex = allTypes.indexOf(superclass);
		        	if (superclassIndex >= 0) {
			            TIntHashSet S = class2subclasses.get(superclassIndex);
			        	if (S == null) {
			        		class2subclasses.put(superclassIndex, S = new TIntHashSet());
			        	}
			        	S.add(i);
			        	superclass = superclass.getSuperclass();
		        	} else {
		        		Messages.logAnon("CHA warning: Class " + superclass + " not previously found!");
		        		loadType(superclass);
		        	}
		        }
			}
		}
		if (DEBUG) {
			Messages.logAnon("CHA status msg: Done computing relations.");
		}
	}

	public Set<jq_Class> getImplementors(jq_Class interfaceType) {
		assert (interfaceType.isInterface());
		int index = allTypes.indexOf(interfaceType);
		if (index >= 0) {
			TIntHashSet implementors = interface2implementors.get(index);
			if (implementors == null) {
				return Collections.emptySet();
			} else {
				final Set<jq_Class> result = new HashSet<jq_Class>(implementors
						.size());
				implementors.forEach(new TIntProcedure() {
					public boolean execute(int arg0) {
						result.add((jq_Class) allTypes.get(arg0));
						return true;
					}
				});
				return result;
			}
		} else {
			Messages.logAnon("CHA warning: " + interfaceType
					+ " not loaded, but was queried for implementors.");
			return Collections.emptySet();
		}
	}
	
	public Set<jq_Class> getSubclasses(jq_Class classType) {
		assert (!classType.isInterface());
		int index = allTypes.indexOf(classType);
		if (index >= 0) {
			TIntHashSet subclasses = class2subclasses.get(index);
			if (subclasses == null) {
				return Collections.emptySet();
			} else {
				final Set<jq_Class> result = new HashSet<jq_Class>(subclasses.size());
				subclasses.forEach(new TIntProcedure() {
					public boolean execute(int arg0) {
						result.add((jq_Class) allTypes.get(arg0));
						return true;
					}
				});
				return result;
			}
		} else {
			Messages.logAnon("CHA warning: " + classType + " not loaded, but was queried for subclasses.");
			return Collections.emptySet();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadTypes(String packageName) {
		if (DEBUG) {
			Messages.logAnon("CHA status msg: Started loading types in package " + packageName + ".");
		}
		Set<String> visited = new HashSet<String>(16);
		for(Iterator<String> classIter = PrimordialClassLoader.loader.listPackage(packageName, true); classIter.hasNext(); ) {
            String className = classIter.next();
            if (className.endsWith(".class")) { 
            	className = className.substring(0, className.length() - ".class".length());
            }
            if (visited.contains(className)){
                continue;
            }
            visited.add(className);
            try {
                jq_Type t = jq_Type.parseType(className);
                loadType(t);
            } catch (NoClassDefFoundError x) {
            	Messages.logAnon("CHA warning: Class " + className + " not found!");
            } catch (ClassFormatError cfe) {
            	Messages.logAnon("CHA warning: Format error loading class " + className + "!");
            } catch (LinkageError le) {
            	Messages.logAnon("CHA warning: Linkage error loading class " + className + "!");
            } catch (RuntimeException e){
            	Messages.logAnon("CHA warning: Runtime error loading class " + className + "!");
            }
		}
		// Save memory!
		((ArrayList) allTypes).trimToSize();
		if (DEBUG) {
			Messages.logAnon("CHA status msg: Done loading types in package " + packageName + ".");
		}
	}

	private void loadType(jq_Type t) {
		if (loadedTypes.add(t)) {
			if (DEBUG) {
            	if (((++numLoadedClasses) % 500) == 0) {
            		Messages.logAnon("CHA status msg: Loaded " + numLoadedClasses + " types so far.");
            	}
            }
//			t.load();
			t.prepare();
			allTypes.add(t);
			if (t instanceof jq_Class) {
				jq_Class c = (jq_Class) t;
				jq_Class d = c.getSuperclass();
				if (d == null)
	        		assert (c == PrimordialClassLoader.JavaLangObject);
				else
					loadType(d);
				for (jq_Class i : c.getDeclaredInterfaces())
					loadType(i);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static Iterator<String> listPackages() {
		PrimordialClassLoader.loader.addToClasspath(Properties.classPathName);
        Collection<String> result = new LinkedList<String>();
        for(Iterator<String> iter = PrimordialClassLoader.loader.listPackages(); iter.hasNext(); ) {
            String packageName = iter.next();
            if (!ignorePackage(packageName)) result.add(packageName);
        }
        return result.iterator();
    }

	private static boolean ignorePackage(String packageName) {
		if (packageName.equals(".") || packageName.equals("")) {
			return true;
		}
		for (String s : excludedPackagePrefixes) {
			if (packageName.startsWith(s)) {
				return true;
			}
		}
//		for (String s : excludedPackageSuffixes) {
//			if (packageName.endsWith(s)) {
//				return true;
//			}
//		}
		return false;
	}
*/
}
