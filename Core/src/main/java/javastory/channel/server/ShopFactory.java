package javastory.channel.server;

import java.util.Map;

import com.google.common.collect.Maps;

public class ShopFactory {

	private final Map<Integer, Shop> shops = Maps.newHashMap();
	private final Map<Integer, Shop> npcShops = Maps.newHashMap();
	private static ShopFactory instance = new ShopFactory();

	public static ShopFactory getInstance() {
		return instance;
	}

	public void clear() {
		this.shops.clear();
		this.npcShops.clear();
	}

	public Shop getShop(final int shopId) {
		if (this.shops.containsKey(shopId)) {
			return this.shops.get(shopId);
		}
		return this.loadShop(shopId, true);
	}

	public Shop getShopForNPC(final int npcId) {
		if (this.npcShops.containsKey(npcId)) {
			return this.npcShops.get(npcId);
		}
		return this.loadShop(npcId, false);
	}

	private Shop loadShop(final int id, final boolean isShopId) {
		final Shop ret = Shop.createFromDB(id, isShopId);
		if (ret != null) {
			this.shops.put(ret.getId(), ret);
			this.npcShops.put(ret.getNpcId(), ret);
		} else if (isShopId) {
			this.shops.put(id, null);
		} else {
			this.npcShops.put(id, null);
		}
		return ret;
	}
}
