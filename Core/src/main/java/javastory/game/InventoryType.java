package javastory.game;

public enum InventoryType {

	UNDEFINED(0), EQUIP(1), USE(2), SETUP(3), ETC(4), CASH(5), EQUIPPED(-1);
	final byte type;

	private InventoryType(final int type) {
		this.type = (byte) type;
	}

	public byte asNumber() {
		return this.type;
	}

	public short getBitfieldEncoding() {
		return (short) (2 << this.type);
	}

	public static InventoryType fromNumber(final byte type) {
		for (final InventoryType item : InventoryType.values()) {
			if (item.asNumber() == type) {
				return item;
			}
		}
		return null;
	}

	public static InventoryType getByWZName(final String name) {
		if (name.equals("Install")) {
			return SETUP;
		} else if (name.equals("Consume")) {
			return USE;
		} else if (name.equals("Etc")) {
			return ETC;
		} else if (name.equals("Cash")) {
			return CASH;
		} else if (name.equals("Pet")) {
			return CASH;
		}
		return UNDEFINED;
	}
}