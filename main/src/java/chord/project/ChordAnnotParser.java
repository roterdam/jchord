/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import chord.bddbddb.RelSign;
import chord.project.analyses.ProgramRel;
import chord.util.ClassUtils;

/**
 * Parser of a Chord annotation on a class defining a Java task.
 *
 * The annotation specifies aspects of the task such as its name,
 * its consumed and produced targets, etc.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ChordAnnotParser {
	private final Class type;
	private String name;
	private Map<String, RelSign> nameToSignMap;
	private Map<String, Class> nameToTypeMap;
	private boolean hasNoErrors;
	private Set<String> consumedNames;
	private Set<String> producedNames;
	/**
	 * Constructor.
	 * 
	 * @param	type	A class annotated with a Chord annotation.
	 */
    public ChordAnnotParser(Class type) {
		this.type = type;
	}
    /**
     * Parses this Chord annotation.
     * 
     * @return	true iff the Chord annotation parses successfully.
     */
	public boolean parse() {
		Chord chord = (Chord) type.getAnnotation(Chord.class);
		assert (chord != null);
		hasNoErrors = true;
		this.name = chord.name();

		String[] names;

		names = chord.consumedNames();
		consumedNames = new HashSet<String>(names.length);
		for (String name2 : names) {
			consumedNames.add(name2);
		}
		names = chord.producedNames();
		producedNames = new HashSet<String>(names.length);
		for (String name2 : names) {
			producedNames.add(name2);
		}
		producedNames.add(name);

		nameToTypeMap = new HashMap<String, Class>();
		nameToSignMap = new HashMap<String, RelSign>();

		nameToTypeMap.put(name, type);

		String sign = chord.sign();
		if (ClassUtils.isSubclass(type, ProgramRel.class)) {
			if (sign.equals("")) {
				error("Method sign() cannot return empty string " +
					"for Java analysis '" + type + "'");
			} else {
				RelSign relSign = parseRelSign(sign);
				nameToSignMap.put(this.name, relSign);
				for (String domName : relSign.getDomKinds()) {
					consumedNames.add(domName);
				}
			}
		} else if (!sign.equals("")) {
			error("Method sign() cannot return non-empty string " +
		 		"for Java analysis '" + type + "'");
		}

        String[] namesOfTypes = chord.namesOfTypes();
        Class [] types = chord.types();
        if (namesOfTypes.length != types.length) {
            error("Methods namesOfTypes() and types() " +
                "return arrays of different lengths.");
        } else {
			for (int i = 0; i < namesOfTypes.length; i++) {
				String name2 = namesOfTypes[i];
				if (name2.equals(this.name) || name2.equals(".")) {
					error("Method namesOfTypes() cannot return the same " +
						"name as that returned by name()");
					continue;
				}
				if (nameToTypeMap.containsKey(name2)) {
					error("Method namesOfTypes() cannot return a name ('" +
						name2 + "') multiple times.");
					continue;
				}
				nameToTypeMap.put(name2, types[i]);
			}
		}

        String[] namesOfSigns = chord.namesOfSigns();
        String[] signs = chord.signs();
        if (namesOfSigns.length != signs.length) {
        	error("Methods namesOfSigns() and signs() " +
                "return arrays of different lengths.");
        } else {
			for (int i = 0; i < namesOfSigns.length; i++) {
				String name2 = namesOfSigns[i];
				if (name2.equals(this.name) || name2.equals(".")) {
					error("Method namesOfSigns() cannot return the same " +
						"name as that returned by name(); use sign().");
					continue;
				}
				if (nameToSignMap.containsKey(name2)) {
					error("Method namesOfSigns() cannot return a name ('" +
						name2 + "') multiple times.");
					continue;
				}
				Class type2 = nameToTypeMap.get(name2);
				if (type2 != null) {
					if (!ClassUtils.isSubclass(type2, ProgramRel.class)) {
						error("Method namesOfSigns() implicitly declares " +
							"name '" + name2 + "' as having type '" +
							ProgramRel.class.getName() + "' whereas method " +
							"namesOfTypes() declares it as having " +
							"incompatible type '" + type2.getName() + "'."); 
						continue;
					}
				}
				RelSign relSign2 = parseRelSign(signs[i]);
				if (relSign2 != null)
					nameToSignMap.put(name2, relSign2);
			}
		}

		return hasNoErrors;
	}
	private RelSign parseRelSign(String sign) {
		int i = sign.indexOf(':');
		String domOrder;
		if (i != -1) {
			domOrder = sign.substring(i + 1);
			sign = sign.substring(0, i);
		} else
			domOrder = null;
 		String[] domNamesAry = sign.split(",");
		if (domNamesAry.length == 1)
			domOrder = domNamesAry[0];
		try {
			return new RelSign(domNamesAry, domOrder);
		} catch (RuntimeException ex) {
			error(ex.getMessage());
			return null;
		}
	}
	private void error(String msg) {
		System.err.println("ERROR: @Chord annotation of" +
			" class '" + type.getName() + "': " + msg);
		hasNoErrors = false;
	}
	/**
	 * Provides the names of targets specified by this Chord annotation
	 * as consumed by the associated program analysis.
	 * 
	 * @return	The names of targets consumed by the underlying
	 * 			program analysis as declared by this Chord annotation.
	 */
	public Set<String> getConsumedNames() {
		return consumedNames;
	}
	/**
	 * Provides the names of targets specified by this Chord annotation
	 * as produced by the associated program analysis.
	 * 
	 * @return	The names of targets produced by the underlying
	 * 			program analysis as declared by this Chord annotation.
	 */
	public Set<String> getProducedNames() {
		return producedNames;
	}
	/**
	 * Provides the name specified by this Chord annotation of the
	 * associated program analysis.
	 * 
	 * @return	The name specified by this Chord annotation of the
	 * 			associated program analysis.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Provides a partial map specified by this Chord annotation from
	 * names of program relation targets consumed/produced by the
	 * associated program analysis to their signatures.
	 * 
	 * @return	A partial map specified by this Chord annotation from
	 *			names of program relation targets consumed/produced by
	 *			the associated program analysis to their signatures.
	 */
	public Map<String, RelSign> getNameToSignMap() {
		return nameToSignMap;
	}
	/**
	 * Provides a partial map specified by this Chord annotation from
	 * names of targets consumed/produced by the associated program
	 * analysis to their types.
	 * 
	 * @return	A partial map specified by this Chord annotation from
	 *			names of targets consumed/produced by the associated
	 *			program analysis to their types.
	 */
	public Map<String, Class> getNameToTypeMap() {
		return nameToTypeMap;
	}
};

