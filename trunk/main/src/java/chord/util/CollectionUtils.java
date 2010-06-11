/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Collection related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class CollectionUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private CollectionUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determines whether a given collection contains duplicate
	 * values.
	 *
	 * @return true iff the given collection contains duplicate
	 *         values.
	 * @param	<T>	The type of the collection elements.
	 * @param	elements	A collection.
	 */
	public static <T> boolean hasDuplicates(final List<T> elements) {
		for (int i = 0; i < elements.size() - 1; i++) {
			final T element = elements.get(i);
			for (int j = i + 1; j < elements.size(); j++) {
				if (elements.get(j).equals(element))
					return true;
			}
		}
		return false;
	}

	public static <T> String toString(final Collection<T> c,
			final String prefix, final String separator, final String suffix) {
		if (c == null) {
			throw new IllegalArgumentException();
		}
		final StringBuilder result = new StringBuilder(prefix);
		if (c.size() > 0) {
			final Iterator<T> it = c.iterator();
			result.append(it.next());
			while (it.hasNext()) {
				result.append(separator).append(it.next());
			}
		}
		return result.append(suffix).toString();
	}


	public static <T> String toString(final Collection<T> a) {
		return toString(a, "", ",", "");
	}

}
