package javastory.server;

public class ShopItem {

	private final short buyable;
	private final int itemId;
	private final int price;

	public ShopItem(final short buyable, final int itemId, final int price) {
		this.buyable = buyable;
		this.itemId = itemId;
		this.price = price;
	}

	public short getBuyable() {
		return this.buyable;
	}

	public int getItemId() {
		return this.itemId;
	}

	public int getPrice() {
		return this.price;
	}
}
