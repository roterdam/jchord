/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * Array related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ArrayUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private ArrayUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determines whether a given array contains a given value.
	 *
	 * @return true iff the given array contains the given value.
	 * @param	<T>	The type of the array elements and the value
	 * to be checked for containment in the array.
	 * @param	array	An array.
	 * @param	s	A value to be checked for containment in the
	 * given array.
	 */
	public static <T> boolean contains(final T[] array, final T s) {
		if (array == null) {
			throw new IllegalArgumentException();
		}
		for (final T t : array) {
			if (t == null) {
				if (s == null) {
					return true;
				}
			} else if (t.equals(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a given array contains duplicate values.
	 *
	 * @return true iff the given array contains duplicate values.
	 * @throws IllegalArgumentException if {@code array} is {@code null}.
	 * @param	<T>	The type of the array elements.
	 * @param	array	An array.
	 */
	public static <T> boolean hasDuplicates(final T[] array) {
		if (array == null) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < array.length - 1; i++) {
			final T x = array[i];
			for (int j = i + 1; j < array.length; j++) {
				final T y = array[j];
				if (x.equals(y)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns string representation of elements in given array. This method never returns {@code null}.
	 *
	 * @param array	 an array of elements.
	 * @param begin	 string prefix.
	 * @param separator elements separator.
	 * @param end	   string suffix.
	 * @param <T>       The type of array elements.
	 * @return string representation of elements in given array.
	 * @throws IllegalArgumentException if {@code array} is {@code null}.
	 */
	public static <T> String toString(final T[] array, final String begin, final String separator, final String end) {
		if (array == null) {
			throw new IllegalArgumentException();
		}
		final StringBuilder result = new StringBuilder(begin);
		for (int i = 0; i < array.length; i++) {
			result.append(array[i]);
			if (i < array.length - 1) {
				result.append(separator);
			}
		}
		return result.append(end).toString();
	}

	/**
	 * Returns string representation of elements in given array. This method never returns {@code null}.
	 *
	 * @param array an array of elements.
	 * @param <T>   The type of the array elements.
	 * @return string representation of elements in given array.
	 */
	public static <T> String toString(final T[] array) {
		return toString(array, "", ",", "");
	}

}
