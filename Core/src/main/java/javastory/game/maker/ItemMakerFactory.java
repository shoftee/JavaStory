package javastory.game.maker;

import java.util.Map;

import javastory.wz.WzData;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public final class ItemMakerFactory {

	private final static ItemMakerFactory instance = new ItemMakerFactory();
	private Map<Integer, MakerItemInfo> itemCache = Maps.newHashMap();
	private Map<Integer, GemInfo> gemCache = Maps.newHashMap();

	public static ItemMakerFactory getInstance() {
		// DO ItemMakerFactory.getInstance() on ChannelServer startup.
		return instance;
	}

	private ItemMakerFactory() {
		System.out.println(":: Loading ItemMakerFactory ::");
		// 0 = Item upgrade crystals
		// 1 / 2/ 4/ 8 = Item creation

		final WzData info = WzDataProviderFactory.getDataProvider("Etc.wz")
				.getData("ItemMake.img");

		for (WzData directory : info.getChildren()) {
			int type = Integer.parseInt(directory.getName());
			switch (type) {
			case 0: // Gem
				cacheGems(directory);
				break;
			case 1: // Warrior
			case 2: // Magician
			case 4: // Bowman
			case 8: // Thief
			case 16: // Pirate
				cacheItems(directory);
				break;
			}
		}
	}

	private void cacheGems(WzData directory) {
		byte reqMakerLevel;
		int reqLevel, cost, quantity;
		for (WzData gemDirectory : directory.getChildren()) {
			reqLevel = WzDataTool.getInt("reqLevel", gemDirectory, 0);
			reqMakerLevel = (byte) WzDataTool.getInt("reqSkillLevel",
														gemDirectory, 0);
			cost = WzDataTool.getInt("meso", gemDirectory, 0);
			quantity = WzDataTool.getInt("itemNum", gemDirectory, 0);

			RandomRewardList rewards = new RandomRewardList();
			ItemRecipeBuilder recipe = new ItemRecipeBuilder();
			for (WzData gemInfo : gemDirectory.getChildren()) {
				for (WzData ind : gemInfo.getChildren()) {
					switch (gemInfo.getName()) {
					case "randomReward":
						rewards.addEntry(WzDataTool.getInt("prob", ind, 0),
											WzDataTool.getInt("item", ind, 0));
						break;
					case "recipe":
						recipe.addEntry(WzDataTool.getInt("item", ind, 0),
										WzDataTool.getInt("count", ind, 0));
						break;
					}
				}
			}
			final GemInfo gemInfo = new GemInfo(rewards.build(),
												recipe.build(), cost,
												reqLevel, reqMakerLevel,
												quantity);
			gemCache.put(Integer.parseInt(gemDirectory.getName()), gemInfo);
		}
	}

	private void cacheItems(WzData directory) {
		for (WzData itemFolder : directory.getChildren()) {
			final int reqLevel =
					WzDataTool.getInt("reqLevel", itemFolder, 0);

			final byte reqMakerLevel = (byte) WzDataTool
					.getInt("reqSkillLevel", itemFolder, 0);

			final int cost =
					WzDataTool.getInt("meso", itemFolder, 0);

			final int quantity =
					WzDataTool.getInt("itemNum", itemFolder, 0);

			final byte totalupgrades =
					(byte) WzDataTool.getInt("tuc", itemFolder, 0);
			final int stimulator =
					WzDataTool.getInt("catalyst", itemFolder, 0);

			ItemRecipeBuilder recipe = new ItemRecipeBuilder();

			for (WzData recipeFolder : itemFolder.getChildren()) {
				for (WzData ind : recipeFolder.getChildren()) {
					if (recipeFolder.getName().equals("recipe")) {
						final int id = WzDataTool.getInt("item", ind, 0);
						final int count = WzDataTool.getInt("count", ind, 0);
						recipe.addEntry(id, count);
					}
				}
			}
			final MakerItemInfo makerItemInfo =
					new MakerItemInfo(recipe.build(), cost, reqLevel,
										reqMakerLevel, quantity,
										totalupgrades, stimulator);
			final int itemId = Integer.parseInt(itemFolder.getName());
			itemCache.put(itemId, makerItemInfo);
		}
	}

	public GemInfo getGemInfo(int itemId) {
		return gemCache.get(itemId);
	}

	public MakerItemInfo getItemInfo(int itemId) {
		return itemCache.get(itemId);
	}
}
