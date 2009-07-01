/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * System property related utilities.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class PropertyUtils {
	/**
	 * Returns the string value of a given system property, if it is
	 * defined, and null otherwise.
	 * 
	 * @param	key	The name of a string-valued system property.
	 * 
	 * @return	The string value of the given system property, if it
	 * 			is defined, and null otherwise.
	 */
	public static String getStrProperty(String key) {
		return System.getProperty(key);
	}
	/**
	 * Returns the string value of a given system property, if it is
	 * defined, and a given default value otherwise.
	 * 
	 * @param	key	The name of a string-valued system property.
	 * @param	def	The default value of the property.
	 * 
	 * @return	The string value of the given system property, if it
	 * 			is defined, and the given default value otherwise.
	 */
	public static String getStrProperty(String key, String def) {
		return System.getProperty(key, def);
	}
	/**
	 * Returns the integer value of a given system property, if it is
	 * defined, and 0 otherwise.
	 * 
	 * @param	key	The name of a integer-valued system property.
	 * 
	 * @return	The integer value of the given system property, if it
	 * 			is defined, and 0 otherwise.
	 */
	public static int getIntProperty(String key) {
		return getIntProperty(key, 0);
	}
	/**
	 * Returns the integer value of a given system property, if it is
	 * defined, and a given default value otherwise.
	 * 
	 * @param	key	The name of a integer-valued system property.
	 * @param	def	The default value of the property.
	 * 
	 * @return	The integer value of the given system property, if it
	 * 			is defined, and the given default value otherwise.
	 */
	public static int getIntProperty(String key, int def) {
		String strVal = System.getProperty(key);
		if (strVal == null)
			return def;
		int intVal;
		try {
			intVal = Integer.parseInt(strVal);
		} catch (NumberFormatException ex) {
			throw new RuntimeException("Integer value " +
				"expected for system property '" + key +
				"; got '" + strVal + "'");
		}
		return intVal;
	}
	/**
	 * Returns the boolean value of a given system property, if it is
	 * defined, and false otherwise.
	 * 
	 * @param	key	The name of a boolean-valued system property.
	 * 
	 * @return	The boolean value of the given system property, if it
	 * 			is defined, and false otherwise.
	 */
	public static boolean getBoolProperty(String key) {
		return getBoolProperty(key, false);
	}
	/**
	 * Returns the boolean value of a given system property, if it is
	 * defined, and a given default value otherwise.
	 * 
	 * @param	key	The name of a boolean-valued system property.
	 * 
	 * @return	The boolean value of the given system property, if it
	 * 			is defined, and the given default value otherwise.
	 */
	public static boolean getBoolProperty(String key, boolean def) {
		String strVal = System.getProperty(key);
		if (strVal == null)
			return def;
		if (strVal.equals("true"))
			return true;
		if (strVal.equals("false"))
			return false;
		throw new RuntimeException("Boolean value ('true' or 'false') " +
			"expected for system property '" + key +
			"; got '" + strVal + "'");
	}
}
