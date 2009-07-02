/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.scannotation.AnnotationDB;

/**
 * 
 * @author Mayur Naik (mayur.naik@intel.com)
 */
public class ClasspathUtils {
	public static Set<String> getClassNames(String classPath) {
        ArrayList<URL> list = new ArrayList<URL>();
        String[] fileNames = classPath.split(File.pathSeparator);
        for (String fileName : fileNames) {
            File file = new File(fileName);
            if (!file.exists()) {
				System.out.println("WARNING: Ignoring: " + fileName);
				continue;
            }
            try {
            	list.add(file.toURL());
            } catch (MalformedURLException ex) {
            	throw new RuntimeException(ex);
            }
        }
        URL[] urls = new URL[list.size()];
        list.toArray(urls);
        AnnotationDB db = new AnnotationDB();
		db.setIgnoredPackages(new String[0]);
        try {
            db.scanArchives(urls);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        Map<String, Set<String>> index = db.getClassIndex();
        return index.keySet();
	}
}
