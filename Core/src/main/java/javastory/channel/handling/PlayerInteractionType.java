package javastory.channel.handling;

public enum PlayerInteractionType {
	CREATE(0x00),
	TRADE_INVITE(0x02),
	TRADE_DECLINE(0x03),
	VISIT(0x04),
	CHAT(0x06),
	EXIT(0x0A),
	OPEN(0x0B),
	TRADE_SET_ITEMS(0x0E),
	TRADE_SET_MESO(0x0F),
	TRADE_CONFIRM(0x10),
	MERCHANT_EXIT(0x1C),
	ADD_ITEM(0x1F),
	BUY_ITEM_STORE(0x20),
	BUY_ITEM_HIREDMERCHANT(0x22),
	REMOVE_ITEM(0x24),
	MAINTENANCE_OFF(0x25),
	MAINTENANCE_ORGANISE(0x26),
	CLOSE_MERCHANT(0x27),
	ADMIN_STORE_NAMECHANGE(0x2B),
	VIEW_MERCHANT_VISITOR(0x2C),
	VIEW_MERCHANT_BLACKLIST(0x2D);

	final byte value;

	private PlayerInteractionType(int value) {
		this.value = (byte) value;
	}

	public byte toNumber() {
		return this.value;
	}

	public static PlayerInteractionType fromNumber(final byte number) {
		for (final PlayerInteractionType type : PlayerInteractionType.values()) {
			if (type.value == number) {
				return type;
			}
		}
		return null;
	}
}