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
import java.io.File;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataInput;
import java.io.IOException;

import joeq.Class.jq_ClassFileConstants;
import joeq.UTF.Utf8;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_ConstantPool;
import joeq.Class.Classpath;
import joeq.Class.ClasspathElement;
import chord.project.Messages;
import chord.project.Properties;
import chord.util.ArraySet;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ClassHierarchyBuilder {
	/**
	 * The entire classpath: the bootclasspath followed by the library
	 * extensions path followed by the user-defined classpath.
	 */
	private final Classpath classpath = new Classpath();
	/**
	 * List of prefixes of names of classes/packages to be excluded
	 * from the class hierarchy.
	 */
	private final String[] scopeExcludeAry;
	/**
	 * Set consisting of:
	 * 1. each class not in scope but declared as the superclass of
	 *    some class in scope, and
	 * 2. each interface not in scope but declared as an
	 *    implemented/extended interface of some class/interface
	 *    respectively in scope.
     * Scope is defined as the set of classes in <code>classpath</code>
	 * minus those that are excluded by <code>scopeExcludeAry</code>.
	 */
	private final Set<String> missingTypes = new HashSet<String>();
	/**
	 * Set of all concrete classes in scope.
	 */
	private final Set<String> allConcreteClasses = new HashSet<String>();
	/**
	 * Set of all interfaces in scope.
	 */
	private final Set<String> allInterfaces = new HashSet<String>();
	/**
	 * Map from each (concrete or abstract) class c in scope to the
	 * class/interface d (not necessarily in scope) such that:
	 * 1. if c == java.lang.Object then d == null, and
	 * 2. if c is a class other than java.lang.Object then d is the
	 *    declared superclass of c.
	 * Note that c cannot be an interface.
	 */
	private final Map<String, String> classToDeclaredSuperclass =
		new HashMap<String, String>();
	/**
	 * Map from each class/interface c in scope to the set of interfaces
	 * S (not necessarily in scope) such that:
	 * 1. if c is an interface then S is the set of interfaces that c
	 *    declares it extends, and
	 * 2. if c is a class then S is the set of interfaces that c
	 *    declares it implements.
	 */
	private final Map<String, Set<String>> typeToDeclaredInterfaces =
		new HashMap<String, Set<String>>();
	/**
	 * Map from each concrete class in scope to the set of all its
	 * (direct and transitive) superclasses (not necessarily in scope).
	 */
	private final Map<String, Set<String>> concreteClassToAllSuperclasses =
		new HashMap<String, Set<String>>();
	/**
	 * Map from each concrete class in scope to the set of all
	 * (direct and transitive) interfaces implemented by it.
	 */
	private final Map<String, Set<String>> concreteClassToAllInterfaces =
		new HashMap<String, Set<String>>();
	/**
	 * Map from each (concrete or abstract) class in scope to the set of
	 * all its concrete subclasses in scope (including itself if it is
	 * concrete).
	 */
	private final Map<String, Set<String>> classToConcreteSubclasses =
		new HashMap<String, Set<String>>();
	/**
	 * Map from each interface in scope to the set of all concrete classes
	 * in scope that implement it.
	 */
	private final Map<String, Set<String>> interfaceToConcreteImplementors =
		new HashMap<String, Set<String>>();

	public ClassHierarchyBuilder() {
		this.scopeExcludeAry = Properties.scopeExcludeAry;
	}

	/**
	 * Provides the set of all concrete classes that subclass a given class.
	 *
	 * @param	cName	The name of a (concrete or abstract) class.
	 *
	 * @return	The set of all concrete classes that subclass (directly or transitively)
	 *			the class named <code>cName</code>, if it exists in the class hierarchy,
	 *			and null otherwise.
	 */
	public Set<String> getConcreteSubclasses(String cName) {
		return classToConcreteSubclasses.get(cName);
	}

	/**
	 * Provides the set of all concrete classes that implement a given interface.
	 *
	 * @param	iName	The name of an interface.
	 *
	 * @return	The set of all concrete classes that implement the interface named
	 *			<code>iName</code>, if it exists in the class hierarchy, and null otherwise.
	 */
	public Set<String> getConcreteImplementors(String iName) {
		return interfaceToConcreteImplementors.get(iName);
	}

	public void run() {
		// maintain set of all classes/interfaces read so far to check for duplicates
	 	final Set<String> allTypes = new HashSet<String>();
		final Classpath cp = new Classpath();
		cp.addStandardClasspath();
		final List<ClasspathElement> cpeList = cp.getClasspathElements();
		// build maps classToDeclaredSuperclass and typeToDeclaredInterfaces and
		// sets allConcreteClasses and allInterfaces
		for (ClasspathElement cpe : cpeList) {
			for (String fileName : cpe.getEntries()) {
				if (!fileName.endsWith(".class"))
					continue;
				String baseName = fileName.substring(0, fileName.length() - 6);
				String className = baseName.replace(File.separatorChar, '.');
				// ignore duplicate classes in classpath
				if (!allTypes.add(className))
					continue;
				boolean exclude = false;
				for (String prefix : scopeExcludeAry) {
					if (className.startsWith(prefix)) {
						exclude = true;
						break;
					}
				}
				if (exclude)
					continue;
				InputStream is = cpe.getResourceAsStream(fileName);
				assert (is != null);
				DataInputStream in = new DataInputStream(is);
				// description of superclass and interfaces sections in class file of class c:
				// 1. superclass d:
				//   if c is an interface then d is java.lang.Object
				//   if c == java.lang.Object then d is null (has index 0 in constant pool)
				//   if c is a class other than java.lang.Object then d is the declared
				//   superclass of c
				// 2. interfaces S:
				//   if c is an interface then S is the set of interfaces c declares it extends
				//   if c is a class then S is the set of interfaces c declares it implements
				try {
					int magicNum = in.readInt(); // 0xCAFEBABE
					if (magicNum != 0xCAFEBABE) {
						throw new ClassFormatError("bad magic number: " +
							Integer.toHexString(magicNum));
					}
					in.readUnsignedShort(); // read minor_version
					in.readUnsignedShort(); // read major_version
					int constant_pool_count = in.readUnsignedShort();
					Object[] constant_pool =
						processConstantPool(in, constant_pool_count);
					char access_flags = (char) in.readUnsignedShort();  // read access_flags
					int self_index = in.readUnsignedShort();
					int super_index = in.readUnsignedShort();
					if (super_index == 0) {
						assert (className.equals("java.lang.Object"));
						classToDeclaredSuperclass.put(className, null);
						allConcreteClasses.add(className);
					} else {
						int c = (Integer) constant_pool[super_index];
						Utf8 utf8 = (Utf8) constant_pool[c];
						String superclassName = utf8.toString().replace(File.separatorChar, '.');
						if (isInterface(access_flags)) {
							assert (superclassName.equals("java.lang.Object"));
							allInterfaces.add(className);
						} else {
							classToDeclaredSuperclass.put(className, superclassName);
							if (!isAbstract(access_flags))
								allConcreteClasses.add(className);
						}
					}
					int n_interfaces = (int) in.readUnsignedShort();
					Set<String> interfaces = new ArraySet<String>(n_interfaces);
					typeToDeclaredInterfaces.put(className, interfaces);
					for (int i = 0; i < n_interfaces; ++i) {
						int interface_index = in.readUnsignedShort();
						int c = (Integer) constant_pool[interface_index];
						Utf8 utf8 = (Utf8) constant_pool[c];
						String interfaceName = utf8.toString().replace(File.separatorChar, '.');
						interfaces.add(interfaceName);
					}
					in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
		}

		// build maps concreteClassToAllSuperclasses and concreteClassToAllInterfaces
		// and also populate set missingTypes
		// the reason we only care about concrete classes in the domain of these two
		// maps is because we will subsequently "invert" these maps when we build maps
		// classToConcreteSubclasses and interfaceToConcreteImplementors, and these
		// two maps only have concrete classes in their range.
		for (String c : allConcreteClasses) {
			Set<String> superclasses = new ArraySet<String>(1);
			boolean success1 = true;
			String d = c;
			while (true) {
				String e = classToDeclaredSuperclass.get(d);
				if (e == null) {
					if (!d.equals("java.lang.Object")) {
						missingTypes.add(d);
						success1 = false;
					}
					break;
				}
				boolean added = superclasses.add(e);
				assert (added);
				d = e;
			}
			Set<String> interfaces = new ArraySet<String>(2);
			boolean success2 = populateInterfaces(c, interfaces);
			if (success1 && success2) {
				concreteClassToAllSuperclasses.put(c, superclasses);
				concreteClassToAllInterfaces.put(c, interfaces);
				continue;
			}
			if (!success1) {
				Messages.logAnon("WARN: Ignoring class " + c +
					" as some (direct or transitive) superclass of it is missing in scope");
			}
			if (!success2) {
				Messages.logAnon("WARN: Ignoring class/interface " + c +
					" as some (direct or transitive) interface implemented/extended by it is missing in scope");
			}
		}

		if (!missingTypes.isEmpty()) {
			Messages.log("SCOPE.CLASSES_NOT_FOUND");
			for (String c : missingTypes)
				Messages.logAnon("\t" + c);
		}

		// build map classToConcreteSubclasses
		for (String c : classToDeclaredSuperclass.keySet()) {
			Set<String> subs = new ArraySet<String>(2);
			if (allConcreteClasses.contains(c))
				subs.add(c);
			classToConcreteSubclasses.put(c, subs);
		}
		for (String c : concreteClassToAllSuperclasses.keySet()) {
			Set<String> supers = concreteClassToAllSuperclasses.get(c);
			for (String d : supers) {
				Set<String> subs = classToConcreteSubclasses.get(d);
				if (subs == null) {
					assert (missingTypes.contains(d));
					continue;
				}
				subs.add(c);
			}
		}
/*
		System.out.println("CLASS TO CONCRETE SUBS:");
		for (String c : classToConcreteSubclasses.keySet()) {
			System.out.println(c);
			for (String d : classToConcreteSubclasses.get(c))
				System.out.println("\t" + d);
		}
*/

		// build map interfaceToConcreteImplementors
		for (String c : allInterfaces) {
			Set<String> impls = new ArraySet<String>(2);
			interfaceToConcreteImplementors.put(c, impls);
		}
		for (String c : concreteClassToAllInterfaces.keySet()) {
			Set<String> interfaces = concreteClassToAllInterfaces.get(c);
			for (String d : interfaces) {
				Set<String> impls = interfaceToConcreteImplementors.get(d);
				if (impls == null) {
					assert (missingTypes.contains(d));
					continue;
				}
				impls.add(c);
			}
		}
/*
		System.out.println("INTERFACE TO CONCRETE IMPLS:");
		for (String c : interfaceToConcreteImplementors.keySet()) {
			System.out.println(c);
			for (String d : interfaceToConcreteImplementors.get(c))
				System.out.println("\t" + d);
		}
*/
		allTypes.clear();
		allConcreteClasses.clear();
		allInterfaces.clear();
		classToDeclaredSuperclass.clear();
		typeToDeclaredInterfaces.clear();
		concreteClassToAllSuperclasses.clear();
		concreteClassToAllInterfaces.clear();
	}

	private static boolean isInterface(char access_flags) {
		return (access_flags & jq_ClassFileConstants.ACC_INTERFACE) != 0;
	}

	private static boolean isAbstract(char access_flags) {
		return (access_flags & jq_ClassFileConstants.ACC_ABSTRACT) != 0;
	}

	private boolean populateInterfaces(String c, Set<String> result) {
		Set<String> interfaces = typeToDeclaredInterfaces.get(c);
		if (interfaces == null) {
			missingTypes.add(c);
			return false;
		}
		for (String d : interfaces) {
			if (result.add(d)) {
				if (!populateInterfaces(d, result))
					return false;
			}
		}
		return true;
	}

	private Object[] processConstantPool(DataInput in, int size) throws IOException {
		Object[] constant_pool = new Object[size];
		for (int i = 1; i < size; ++i) { // CP slot 0 is unused
			byte b = in.readByte();
			switch (b) {
			case jq_ClassFileConstants.CONSTANT_Integer:
				in.readInt();
				break;
			case jq_ClassFileConstants.CONSTANT_Float:
				in.readFloat();
				break;
			case jq_ClassFileConstants.CONSTANT_Long:
				++i;
				in.readLong();
				break;
			case jq_ClassFileConstants.CONSTANT_Double:
				++i;
				in.readDouble();
				break;
			case jq_ClassFileConstants.CONSTANT_Utf8:
			{
				byte utf[] = new byte[in.readUnsignedShort()];
				in.readFully(utf);
				constant_pool[i] = Utf8.get(utf);
				break;
			}
			case jq_ClassFileConstants.CONSTANT_Class:
				constant_pool[i] = new Integer(in.readUnsignedShort());
				break;
			case jq_ClassFileConstants.CONSTANT_String:
				in.readUnsignedShort();
				break;
			case jq_ClassFileConstants.CONSTANT_NameAndType:
			case jq_ClassFileConstants.CONSTANT_FieldRef:
			case jq_ClassFileConstants.CONSTANT_MethodRef:
			case jq_ClassFileConstants.CONSTANT_InterfaceMethodRef:
				in.readUnsignedShort();
				in.readUnsignedShort();
				break;
			default:
				throw new ClassFormatError(
					"bad constant pool entry tag: entry="+i+", tag="+b);
			}
		}
		return constant_pool;
	}
}
