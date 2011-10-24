package javastory.channel.server;

import java.util.HashMap;
import java.util.Map;

public class ShopFactory {

	private Map<Integer, Shop> shops = new HashMap<>();
	private Map<Integer, Shop> npcShops = new HashMap<>();
	private static ShopFactory instance = new ShopFactory();

	public static ShopFactory getInstance() {
		return instance;
	}

	public void clear() {
		shops.clear();
		npcShops.clear();
	}

	public Shop getShop(int shopId) {
		if (shops.containsKey(shopId)) {
			return shops.get(shopId);
		}
		return loadShop(shopId, true);
	}

	public Shop getShopForNPC(int npcId) {
		if (npcShops.containsKey(npcId)) {
			return npcShops.get(npcId);
		}
		return loadShop(npcId, false);
	}

	private Shop loadShop(int id, boolean isShopId) {
		Shop ret = Shop.createFromDB(id, isShopId);
		if (ret != null) {
			shops.put(ret.getId(), ret);
			npcShops.put(ret.getNpcId(), ret);
		} else if (isShopId) {
			shops.put(id, null);
		} else {
			npcShops.put(id, null);
		}
		return ret;
	}
}
