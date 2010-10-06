package chord.program.reflect;

import java.io.IOException;
import chord.runtime.CoreEventHandler;

public class ReflectEventHandler extends CoreEventHandler {
	public static void reflectEvent(int eventKind, String q, String c) {
		synchronized (CoreEventHandler.class) {
			if (trace) {
				trace = false;
				try {
					buffer.putByte((byte) eventKind);
					buffer.putString(q);
					buffer.putString(c);
				} catch (IOException ex) { throw new RuntimeException(ex); }
				trace = true;
			}
		}
	}
}
