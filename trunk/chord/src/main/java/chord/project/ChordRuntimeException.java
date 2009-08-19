package chord.project;

public class ChordRuntimeException extends RuntimeException {
	public ChordRuntimeException() { }
	public ChordRuntimeException(String msg) {
		super(msg);
	}
	public ChordRuntimeException(Exception ex) {
		super(ex);
	}
}

