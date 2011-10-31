/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.game.quest;

/**
 * 
 * @author shoftee
 */
public enum QuestCompletionStatus {
	INACTIVE(0), IN_PROGRESS(1), COMPLETED(2);
	//
	private int status;

	private QuestCompletionStatus(final int status) {
		this.status = status;
	}

	public byte asNumber() {
		return (byte) this.status;
	}

	public static QuestCompletionStatus fromNumber(final int status) {
		switch (status) {
		case 0:
			return INACTIVE;
		case 1:
			return IN_PROGRESS;
		case 2:
			return COMPLETED;
		default:
			return null;
		}
	}
}
