/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.tools;

/**
 * 
 * @author shoftee
 */
public enum FameResponse {

	/* 0: ok, use giveFameResponse
	 * 1: the username is incorrectly entered
	 * 2: users under level 15 are unable to toggle with fame.
	 * 3: can't raise or drop fame anymore today.
	 * 4: can't raise or drop fame for this character for this month anymore.
	 * 5: received fame, use receiveFame()
	 * 6: level of fame neither has been raised nor dropped due to an unexpected error*/
	SUCCESS(0), INCORRECT_NAME(1), LOW_LEVEL(2), ONCE_A_DAY(3), ONCE_A_MONTH(4), RECEIVED_FAME(5), UNEXPECTED_ERROR(6);

	private int type;

	private FameResponse(final int type) {
		this.type = type;
	}

	public byte asNumber() {
		return (byte) this.type;
	}
}
