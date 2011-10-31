package javastory.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javastory.game.Item;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public class CashItemFactory {

	private static CashItemFactory instance = new CashItemFactory();
	private final Map<Integer, CashItemInfo> itemStats = new HashMap<>();
	private final WzDataProvider data = WzDataProviderFactory.getDataProvider("Etc.wz");

	public static CashItemFactory getInstance() {
		return instance;
	}

	protected CashItemFactory() {
		System.out.println(":: Loading CashItemFactory ::");
		for (final WzData field : this.data.getData("Commodity.img").getChildren()) {
			final boolean onSale = WzDataTool.getIntConvert("OnSale", field, 0) > 0;
			if (onSale) {
				final CashItemInfo stats = new CashItemInfo(WzDataTool.getIntConvert("ItemId", field), WzDataTool.getIntConvert("Count", field, 1), WzDataTool
					.getIntConvert("Price", field, 0), WzDataTool.getIntConvert("Period", field, 0));
				this.itemStats.put(WzDataTool.getIntConvert("SN", field, 0), stats);
			}
		}
	}

	public CashItemInfo getItem(final int sn) {
		final CashItemInfo stats = this.itemStats.get(sn);
		if (stats == null) {
			return null;
		}
		return stats;
	}

	public List<Integer> getPackageItems(final int itemId) {
		final List<Integer> packageItems = new ArrayList<>();
		for (final WzData b : this.data.getData("CashPackage.img").getChildren()) {
			if (itemId == Integer.parseInt(b.getName())) {
				for (final WzData c : b.getChildren()) {
					for (final WzData d : c.getChildren()) {
						packageItems.add(this.getItem(WzDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c)).getId());
					}
				}
				break;
			}
		}
		return packageItems;
	}

	public void addToInventory(final Item item) {
		//inventory.add(item);
	}

	public void removeFromInventory(final Item item) {
		//inventory.remove(item);
	}
}