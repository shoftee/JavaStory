package javastory.channel.maps;

public enum ItemDropType {
	DEFAULT((byte) 0), PARTY((byte) 1), FREE_FOR_ALL((byte) 2), EXPLOSIVE((byte) 3);

	private final byte type;

	private ItemDropType(byte type) {
		this.type = type;
	}

	public byte asByte() {
		return (byte) type;
	}
}
