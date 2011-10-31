package javastory.game;

/**
 * Specifies Gender across the server operation.
 * 
 * This enum is used for gender indication in packets, as well as gender
 * restriction in item data.
 * 
 * @author shoftee
 */
public enum Gender {
	/**
	 * Indicates the male gender.
	 */
	MALE(0),
	/**
	 * Indicates the female gender.
	 */
	FEMALE(1),
	/**
	 * Indicates an unspecified gender. Note: Used only for gender restriction
	 * in item data.
	 */
	UNSPECIFIED(2);

	private byte type;

	private Gender(final int type) {
		this.type = (byte) type;
	}

	/**
	 * Returns the underlying number for this Gender.
	 * 
	 * @return the value for this Gender.
	 */
	public byte asNumber() {
		return this.type;
	}

	/**
	 * Returns the Gender for the specified type value, or <code>null</code> if
	 * there is no corresponding one.
	 * 
	 * @param type
	 *            the value to query
	 * @return the Gender corresponding to the specified value, or
	 *         <code>null</code> if there is no such Gender.
	 */
	public static Gender fromNumber(final int type) {
		switch (type) {
		case 0:
			return MALE;
		case 1:
			return FEMALE;
		case 2:
			return UNSPECIFIED;
		default:
			return null;
		}
	}
}
