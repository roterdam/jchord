/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Class related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ClassUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private ClassUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determines whether a given class is a subclass of another.
	 *
	 * @param	subclass	An intended subclass.
	 * @param	superclass	An intended superclass.
	 * @return	{@code true} iff class {@code subclass} is a subclass of class <tt>superclass</tt>.
	 */
	public static boolean isSubclass(final Class subclass, final Class superclass) {
		try {
			subclass.asSubclass(superclass);
		} catch (final ClassCastException ex) {
			return false;
		}
		return true;
	}

	public static InputStream getResourceAsStream(String resName) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return cl.getResourceAsStream(resName);
	}

	public static BufferedReader getResourceAsReader(String resName) {
		InputStream is = getResourceAsStream(resName);
		return (is == null) ? null : new BufferedReader(new InputStreamReader(is));
	}
}
