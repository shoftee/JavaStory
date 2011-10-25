package javastory.db;

// TODO: Get rid of this nonsensical class. Jesus.
public class DatabaseException extends RuntimeException {

	private static final long serialVersionUID = -420103154764822555L;

	public DatabaseException(String msg) {
		super(msg);
	}

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}
}