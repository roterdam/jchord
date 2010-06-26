/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility for logging messages during Chord's execution.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Messages {
	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle("chord.project.messages");

	private Messages() { }

	public static String get(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			throw new RuntimeException("Message not found: " + key);
		}
	}
	public static String get(String key, Object... args) {
		try {
			String format = RESOURCE_BUNDLE.getString(key);
			return String.format(format, args);
		} catch (MissingResourceException e) {
			throw new RuntimeException("Message not found: " + key);
		}
	}
	public static void logAnon(String format, Object... args) {
		String msg = String.format(format, args);
        System.out.println(msg);
    }

	public static void log(String key, Object... args) {
		try {
			String format = RESOURCE_BUNDLE.getString(key);
			String msg = String.format(format, args);
			System.out.println(msg);
		} catch (MissingResourceException e) {
			throw new RuntimeException("Message not found: " + key);
		}
	}
	public static void fatal(String key, Object... args) {
		log(key, args);
		System.exit(1);
	}
	public static void fatalAnon(String format, Object... args) {
		logAnon(format, args);
		System.exit(1);
	}
}

