package javastory.game;

public enum ItemConsumeType {
	DEFAULT(0),
	ON_PICKUP(1),
	ON_PICKUP_PARTY(2);

	private int value;

	private ItemConsumeType(final int value) {
		this.value = value;
	}

	public byte asNumber() {
		return (byte) this.value;
	}

	public static ItemConsumeType fromNumber(final int value) {
		switch (value) {
		case 0:
			return DEFAULT;
		case 1:
			return ON_PICKUP;
		case 2:
			return ON_PICKUP_PARTY;
		default:
			return null;
		}
	}
}
