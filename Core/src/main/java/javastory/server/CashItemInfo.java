package javastory.server;

public class CashItemInfo {
	private final int itemId, count, price, period;

	public CashItemInfo(final int itemId, final int count, final int price, final int period) {
		this.itemId = itemId;
		this.count = count;
		this.price = price;
		this.period = period;
	}

	public int getId() {
		return this.itemId;
	}

	public int getCount() {
		return this.count;
	}

	public int getPeriod() {
		return this.period;
	}

	public int getPrice() {
		return this.price;
	}
}