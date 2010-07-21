/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scannotation.AnnotationDB;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ClasspathUtils {

	/**
	 * An empty string array.
	 */
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private ClasspathUtils() {
		throw new UnsupportedOperationException();
	}

	public static Set<String> getClassNames(final String classPath) {
		if (classPath == null) {
			throw new IllegalArgumentException();
		}
		final List<URL> list = new ArrayList<URL>();
		for (final String fileName : classPath.split(File.pathSeparator)) {
			final File file = new File(fileName);
			if (!file.exists()) {
				System.out.println("WARNING: Ignoring: " + fileName); // TODO: REMOVE System.out.println()
				continue;
			}
			try {
				list.add(file.toURL());
			} catch (final MalformedURLException ex) {
				throw new RuntimeException(ex);
			}
		}
		final AnnotationDB db = new AnnotationDB();
		db.setIgnoredPackages(EMPTY_STRING_ARRAY);
		try {
			db.scanArchives(list.toArray(new URL[list.size()]));
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
		return db.getClassIndex().keySet();
	}

}
