/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

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

}
