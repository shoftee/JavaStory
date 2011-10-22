package javastory.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javastory.game.IItem;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public class CashItemFactory {

    private static CashItemFactory instance = new CashItemFactory();
    private Map<Integer, CashItemInfo> itemStats = new HashMap<>();
    private WzDataProvider data = WzDataProviderFactory.getDataProvider("Etc.wz");

    public static CashItemFactory getInstance() {
        return instance;
    }

    protected CashItemFactory() {
        System.out.println(":: Loading CashItemFactory ::");
        for (WzData field : data.getData("Commodity.img").getChildren()) {
            boolean onSale = WzDataTool.getIntConvert("OnSale", field, 0) > 0;
            if (onSale) {
                final CashItemInfo stats = new CashItemInfo(
                        WzDataTool.getIntConvert("ItemId", field),
                        WzDataTool.getIntConvert("Count", field, 1),
                        WzDataTool.getIntConvert("Price", field, 0),
                        WzDataTool.getIntConvert("Period", field, 0));
                itemStats.put(WzDataTool.getIntConvert("SN", field, 0), stats);
            }
        }
    }

    public CashItemInfo getItem(int sn) {
        CashItemInfo stats = itemStats.get(sn);
        if (stats == null) {
            return null;
        }
        return stats;
    }

    public List<Integer> getPackageItems(int itemId) {
        List<Integer> packageItems = new ArrayList<>();
        for (WzData b : data.getData("CashPackage.img").getChildren()) {
            if (itemId == Integer.parseInt(b.getName())) {
                for (WzData c : b.getChildren()) {
                    for (WzData d : c.getChildren()) {
                        packageItems.add(getItem(WzDataTool.getIntConvert("" +
                                Integer.parseInt(d.getName()), c)).getId());
                    }
                }
                break;
            }
        }
        return packageItems;
    }

    public void addToInventory(IItem item) {
        //inventory.add(item);
    }

    public void removeFromInventory(IItem item) {
        //inventory.remove(item);
    }
}