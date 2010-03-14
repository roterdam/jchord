package chord.project;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
	public static void log(String key, Object... args) {
		try {
			String format = RESOURCE_BUNDLE.getString(key);
			String msg = String.format(format, args);
			System.out.println(msg);
		} catch (MissingResourceException e) {
			throw new RuntimeException("Message not found: " + key);
		}
	}
}

