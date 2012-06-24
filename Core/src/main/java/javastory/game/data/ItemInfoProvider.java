package javastory.game.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.server.StatEffect;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.IdNameEntry;
import javastory.game.IdProbabilityEntry;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.ItemConsumeType;
import javastory.game.ItemFlag;
import javastory.game.ItemType;
import javastory.server.EquipLevelInfo;
import javastory.tools.Randomizer;
import javastory.wz.WzData;
import javastory.wz.WzDataDirectoryEntry;
import javastory.wz.WzDataFileEntry;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;
import javastory.wz.WzDataType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class ItemInfoProvider {

	//
	private final static ItemInfoProvider instance = new ItemInfoProvider();
	private final WzDataProvider itemData = WzDataProviderFactory.getDataProvider("Item.wz");
	private final WzDataProvider equipData = WzDataProviderFactory.getDataProvider("Character.wz");
	private final WzDataProvider stringData = WzDataProviderFactory.getDataProvider("String.wz");
	private final WzData cashStringData = this.stringData.getData("Cash.img");
	private final WzData consumeStringData = this.stringData.getData("Consume.img");
	private final WzData eqpStringData = this.stringData.getData("Eqp.img");
	private final WzData etcStringData = this.stringData.getData("Etc.img");
	private final WzData insStringData = this.stringData.getData("Ins.img");
	private final WzData petStringData = this.stringData.getData("Pet.img");
	//
	private final Map<Integer, Short> slotMaxCache = Maps.newHashMap();
	private final Map<Integer, StatEffect> itemEffects = Maps.newHashMap();
	private final Map<Integer, Map<String, Integer>> equipStatsCache = Maps.newHashMap();
	private final Map<Integer, Map<String, Byte>> itemMakeStatsCache = Maps.newHashMap();
	private final Map<Integer, List<EquipLevelInfo>> equipLevelCache = Maps.newHashMap();
	private final Map<Integer, Short> itemMakeLevel = Maps.newHashMap();
	private final Map<Integer, Equip> equipCache = Maps.newHashMap();
	private final Map<Integer, Double> priceCache = Maps.newHashMap();
	private final Map<Integer, Integer> wholePriceCache = Maps.newHashMap();
	private final Map<Integer, Integer> projectileWatkCache = Maps.newHashMap();
	private final Map<Integer, Integer> monsterBookID = Maps.newHashMap();
	private final Map<Integer, String> nameCache = Maps.newHashMap();
	private final Map<Integer, String> descCache = Maps.newHashMap();
	private final Map<Integer, String> msgCache = Maps.newHashMap();
	private final Map<Integer, Map<String, Integer>> SkillStatsCache = Maps.newHashMap();
	private final Map<Integer, ItemConsumeType> consumeOnPickupCache = Maps.newHashMap();
	private final Map<Integer, Boolean> dropRestrictionCache = Maps.newHashMap();
	private final Map<Integer, Boolean> pickupRestrictionCache = Maps.newHashMap();
	private final Map<Integer, Integer> stateChangeCache = Maps.newHashMapWithExpectedSize(40);
	private final Map<Integer, Integer> karmaEnabledCache = Maps.newHashMap();
	private final Map<Integer, Boolean> isQuestItemCache = Maps.newHashMap();
	private final Map<Integer, List<IdProbabilityEntry>> summonMobCache = Maps.newHashMap();
	private final List<IdNameEntry> itemNameCache = Lists.newArrayList();
	private final Map<Integer, RewardSetInfo> RewardItem = Maps.newHashMap();

	private ItemInfoProvider() {
		System.out.println(":: Loading MapleItemInformationProvider ::");
	}

	public static ItemInfoProvider getInstance() {
		return instance;
	}

	public final List<IdNameEntry> getAllItems() {
		if (!this.itemNameCache.isEmpty()) {
			return this.itemNameCache;
		}
		final List<IdNameEntry> itemPairs = Lists.newArrayList();
		WzData itemsData;

		itemsData = this.stringData.getData("Cash.img");
		for (final WzData itemFolder : itemsData.getChildren()) {
			final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));

			itemPairs.add(entry);
		}

		itemsData = this.stringData.getData("Consume.img");
		for (final WzData itemFolder : itemsData.getChildren()) {
			final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));

			itemPairs.add(entry);
		}

		itemsData = this.stringData.getData("Eqp.img").getChildByPath("Eqp");
		for (final WzData eqpType : itemsData.getChildren()) {
			for (final WzData itemFolder : eqpType.getChildren()) {
				final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));

				itemPairs.add(entry);
			}
		}

		itemsData = this.stringData.getData("Etc.img").getChildByPath("Etc");
		for (final WzData itemFolder : itemsData.getChildren()) {
			final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));
			itemPairs.add(entry);
		}

		itemsData = this.stringData.getData("Ins.img");
		for (final WzData itemFolder : itemsData.getChildren()) {
			final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));
			itemPairs.add(entry);
		}

		itemsData = this.stringData.getData("Pet.img");
		for (final WzData itemFolder : itemsData.getChildren()) {
			final IdNameEntry entry = new IdNameEntry(Integer.parseInt(itemFolder.getName()), WzDataTool.getString("name", itemFolder, "NO-NAME"));
			itemPairs.add(entry);
		}
		return itemPairs;
	}

	private WzData getStringData(final int itemId) {
		String cat = null;
		WzData data;

		if (itemId >= 5010000) {
			data = this.cashStringData;
		} else if (itemId >= 2000000 && itemId < 3000000) {
			data = this.consumeStringData;
		} else if (itemId >= 1142000 && itemId < 1143000 || itemId >= 1010000 && itemId < 1040000 || itemId >= 1122000 && itemId < 1123000) {
			data = this.eqpStringData;
			cat = "Accessory";
		} else if (itemId >= 1000000 && itemId < 1010000) {
			data = this.eqpStringData;
			cat = "Cap";
		} else if (itemId >= 1102000 && itemId < 1103000) {
			data = this.eqpStringData;
			cat = "Cape";
		} else if (itemId >= 1040000 && itemId < 1050000) {
			data = this.eqpStringData;
			cat = "Coat";
		} else if (itemId >= 20000 && itemId < 22000) {
			data = this.eqpStringData;
			cat = "Face";
		} else if (itemId >= 1080000 && itemId < 1090000) {
			data = this.eqpStringData;
			cat = "Glove";
		} else if (itemId >= 30000 && itemId < 32000) {
			data = this.eqpStringData;
			cat = "Hair";
		} else if (itemId >= 1050000 && itemId < 1060000) {
			data = this.eqpStringData;
			cat = "Longcoat";
		} else if (itemId >= 1060000 && itemId < 1070000) {
			data = this.eqpStringData;
			cat = "Pants";
		} else if (itemId >= 1802000 && itemId < 1810000) {
			data = this.eqpStringData;
			cat = "PetEquip";
		} else if (itemId >= 1112000 && itemId < 1120000) {
			data = this.eqpStringData;
			cat = "Ring";
		} else if (itemId >= 1092000 && itemId < 1100000) {
			data = this.eqpStringData;
			cat = "Shield";
		} else if (itemId >= 1070000 && itemId < 1080000) {
			data = this.eqpStringData;
			cat = "Shoes";
		} else if (itemId >= 1900000 && itemId < 2000000) {
			data = this.eqpStringData;
			cat = "Taming";
		} else if (itemId >= 1300000 && itemId < 1800000) {
			data = this.eqpStringData;
			cat = "Weapon";
		} else if (itemId >= 4000000 && itemId < 5000000) {
			data = this.etcStringData;
		} else if (itemId >= 3000000 && itemId < 4000000) {
			data = this.insStringData;
		} else if (itemId >= 5000000 && itemId < 5010000) {
			data = this.petStringData;
		} else {
			return null;
		}
		if (cat == null) {
			return data.getChildByPath(String.valueOf(itemId));
		} else {
			return data.getChildByPath("Eqp/" + cat + "/" + itemId);
		}
	}

	private WzData getItemData(final int itemId) {
		WzData ret = null;
		final String idStr = "0" + String.valueOf(itemId);
		WzDataDirectoryEntry root = this.itemData.getRoot();
		for (final WzDataDirectoryEntry topDir : root.getSubdirectories()) {
			// we should have .img files here beginning with the first 4 IID
			for (final WzDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = this.itemData.getData(topDir.getName() + "/" + iFile.getName());
					if (ret == null) {
						return null;
					}
					ret = ret.getChildByPath(idStr);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					return this.itemData.getData(topDir.getName() + "/" + iFile.getName());
				}
			}
		}
		root = this.equipData.getRoot();
		for (final WzDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final WzDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					return this.equipData.getData(topDir.getName() + "/" + iFile.getName());
				}
			}
		}
		return ret;
	}

	/** returns the maximum of items in one slot */
	public final short getSlotMax(final int itemId) {
		if (this.slotMaxCache.containsKey(itemId)) {
			return this.slotMaxCache.get(itemId);
		}
		short ret = 0;
		final WzData item = this.getItemData(itemId);
		if (item != null) {
			final WzData smEntry = item.getChildByPath("info/slotMax");
			if (smEntry == null) {
				if (GameConstants.getInventoryType(itemId) == InventoryType.EQUIP) {
					ret = 1;
				} else {
					ret = 100;
				}
			} else {
				ret = (short) WzDataTool.getInt(smEntry);
			}
		}
		this.slotMaxCache.put(itemId, ret);
		return ret;
	}

	public final int getWholePrice(final int itemId) {
		if (this.wholePriceCache.containsKey(itemId)) {
			return this.wholePriceCache.get(itemId);
		}
		final WzData item = this.getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		final WzData pData = item.getChildByPath("info/price");
		if (pData == null) {
			return -1;
		}
		pEntry = WzDataTool.getInt(pData);

		this.wholePriceCache.put(itemId, pEntry);
		return pEntry;
	}
	
	public final double getPrice(final int itemId) {
		if (this.priceCache.containsKey(itemId)) {
			return this.priceCache.get(itemId);
		}
		final WzData item = this.getItemData(itemId);
		if (item == null) {
			return -1;
		}
		double pEntry = 0.0;
		WzData pData = item.getChildByPath("info/unitPrice");
		if (pData != null) {
			if (pData.getType() == WzDataType.DOUBLE) {
				pEntry = WzDataTool.getDouble(pData);
			} else {
				pEntry = WzDataTool.getInt(pData);
			}
		} else {
			pData = item.getChildByPath("info/price");
			if (pData == null) {
				return -1;
			}
			pEntry = WzDataTool.getInt(pData);
		}
		if (itemId == 2070019 || itemId == 2330007) {
			pEntry = 1.0;
		}
		this.priceCache.put(itemId, pEntry);
		return pEntry;
	}

	public final List<EquipLevelInfo> getEquipLevelStat(final int itemid, final byte level) {
		if (this.equipLevelCache.containsKey(itemid)) {
			return this.equipLevelCache.get(itemid);
		}
		final WzData item = this.getItemData(itemid);
		if (item == null) {
			return null;
		}
		final WzData info = item.getChildByPath("info/level");
		if (info == null) {
			return null;
		}
		final List<EquipLevelInfo> el = Lists.newArrayList();
		EquipLevelInfo sel;

		for (final WzData data : info.getChildByPath("info")) {
			sel = new EquipLevelInfo();
			sel.incSTRMax = (byte) WzDataTool.getInt("incSTRMax", data, 0);
			sel.incSTRMin = (byte) WzDataTool.getInt("incSTRMin", data, 0);

			sel.incDEXMax = (byte) WzDataTool.getInt("incDEXMax", data, 0);
			sel.incDEXMin = (byte) WzDataTool.getInt("incDEXMin", data, 0);

			sel.incLUKMax = (byte) WzDataTool.getInt("incLUKMax", data, 0);
			sel.incLUKMin = (byte) WzDataTool.getInt("incLUKMin", data, 0);

			sel.incINTMax = (byte) WzDataTool.getInt("incINTMax", data, 0);
			sel.incINTMin = (byte) WzDataTool.getInt("incINTMin", data, 0);

			sel.incPADMax = (byte) WzDataTool.getInt("incPADMax", data, 0);
			sel.incPADMin = (byte) WzDataTool.getInt("incPADMin", data, 0);

			sel.incMADMax = (byte) WzDataTool.getInt("incMADMax", data, 0);
			sel.incMADMin = (byte) WzDataTool.getInt("incMADMin", data, 0);

			el.add(sel);
		}
		this.equipLevelCache.put(itemid, el);
		return el;
	}

	public final Map<String, Byte> getItemMakeStats(final int itemId) {
		if (this.itemMakeStatsCache.containsKey(itemId)) {
			return this.itemMakeStatsCache.get(itemId);
		}
		if (itemId / 10000 != 425) {
			return null;
		}
		final Map<String, Byte> ret = Maps.newLinkedHashMap();
		final WzData item = this.getItemData(itemId);
		if (item == null) {
			return null;
		}
		final WzData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		ret.put("incPAD", (byte) WzDataTool.getInt("incPAD", info, 0)); // WATK
		ret.put("incMAD", (byte) WzDataTool.getInt("incMAD", info, 0)); // MATK
		ret.put("incACC", (byte) WzDataTool.getInt("incACC", info, 0)); // ACC
		ret.put("incEVA", (byte) WzDataTool.getInt("incEVA", info, 0)); // AVOID
		ret.put("incSpeed", (byte) WzDataTool.getInt("incSpeed", info, 0)); // SPEED
		ret.put("incJump", (byte) WzDataTool.getInt("incJump", info, 0)); // JUMP
		ret.put("incMaxHP", (byte) WzDataTool.getInt("incMaxHP", info, 0)); // HP
		ret.put("incMaxMP", (byte) WzDataTool.getInt("incMaxMP", info, 0)); // MP
		ret.put("incSTR", (byte) WzDataTool.getInt("incSTR", info, 0)); // STR
		ret.put("incINT", (byte) WzDataTool.getInt("incINT", info, 0)); // INT
		ret.put("incLUK", (byte) WzDataTool.getInt("incLUK", info, 0)); // LUK
		ret.put("incDEX", (byte) WzDataTool.getInt("incDEX", info, 0)); // DEX
		//	ret.put("incReqLevel", MapleDataTool.getInt("incReqLevel", info, 0)); // IDK!
		ret.put("randOption", (byte) WzDataTool.getInt("randOption", info, 0)); // Black Crystal Wa/MA
		ret.put("randStat", (byte) WzDataTool.getInt("randStat", info, 0)); // Dark Crystal - Str/Dex/int/Luk

		this.itemMakeStatsCache.put(itemId, ret);
		return ret;
	}

	public final Map<String, Integer> getEquipStats(final int itemId) {
		if (this.equipStatsCache.containsKey(itemId)) {
			return this.equipStatsCache.get(itemId);
		}
		final Map<String, Integer> ret = Maps.newLinkedHashMap();
		final WzData item = this.getItemData(itemId);
		if (item == null) {
			return null;
		}
		final WzData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (final WzData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3), WzDataTool.getIntConvert(data));
			}
		}
		ret.put("tuc", WzDataTool.getInt("tuc", info, 0));
		ret.put("reqLevel", WzDataTool.getInt("reqLevel", info, 0));
		ret.put("reqJob", WzDataTool.getInt("reqJob", info, 0));
		ret.put("reqSTR", WzDataTool.getInt("reqSTR", info, 0));
		ret.put("reqDEX", WzDataTool.getInt("reqDEX", info, 0));
		ret.put("reqINT", WzDataTool.getInt("reqINT", info, 0));
		ret.put("reqLUK", WzDataTool.getInt("reqLUK", info, 0));
		ret.put("reqPOP", WzDataTool.getInt("reqPOP", info, 0));
		ret.put("cash", WzDataTool.getInt("cash", info, 0));
		ret.put("canLevel", info.getChildByPath("level") == null ? 0 : 1);
		ret.put("cursed", WzDataTool.getInt("cursed", info, 0));
		ret.put("success", WzDataTool.getInt("success", info, 0));
		ret.put("equipTradeBlock", WzDataTool.getInt("equipTradeBlock", info, 0));

		if (GameConstants.isMagicWeapon(itemId)) {
			ret.put("elemDefault", WzDataTool.getInt("elemDefault", info, 100));
			ret.put("incRMAS", WzDataTool.getInt("incRMAS", info, 100)); // Poison
			ret.put("incRMAF", WzDataTool.getInt("incRMAF", info, 100)); // Fire
			ret.put("incRMAL", WzDataTool.getInt("incRMAL", info, 100)); // Lightning
			ret.put("incRMAI", WzDataTool.getInt("incRMAI", info, 100)); // Ice
		}

		this.equipStatsCache.put(itemId, ret);
		return ret;
	}

	public final boolean canEquip(final Map<String, Integer> stats, final int itemid, final int level, final int job, final int fame, final int str,
		final int dex, final int luk, final int int_) {
		if (level >= stats.get("reqLevel") && str >= stats.get("reqSTR") && dex >= stats.get("reqDEX") && luk >= stats.get("reqLUK")
			&& int_ >= stats.get("reqINT")) {
			final int fameReq = stats.get("reqPOP");
			if (fameReq != 0 && fame < fameReq) {
				return false;
			}
			return true;
		}
		return false;
	}

	public final int getReqLevel(final int itemId) {
		return this.getEquipStats(itemId).get("reqLevel");
	}

	public final List<Integer> getScrollReqs(final int itemId) {
		final List<Integer> ret = Lists.newArrayList();
		final WzData data = this.getItemData(itemId).getChildByPath("req");

		if (data == null) {
			return ret;
		}
		for (final WzData req : data.getChildren()) {
			ret.add(WzDataTool.getInt(req));
		}
		return ret;
	}

	public final Item scrollEquipWithId(final Item equipItem, final int scrollId, final boolean ws) {
		if (equipItem.getType() == ItemType.EQUIP) {
			final Equip equip = (Equip) equipItem;
			final Map<String, Integer> stats = this.getEquipStats(scrollId);
			final Map<String, Integer> eqstats = this.getEquipStats(equip.getItemId());

			if (Randomizer.nextInt(100) <= stats.get("success")) {
				switch (scrollId) {
				case 2049000:
				case 2049001:
				case 2049002:
				case 2049003: {
					if (equip.getLevel() + equip.getUpgradeSlots() < eqstats.get("tuc")) {
						equip.setUpgradeSlots((byte) (equip.getUpgradeSlots() + 1));
					}
					break;
				}
				case 2040727: // Spikes on shoe, prevents slip
				{
					byte flag = equip.getFlag();
					flag |= ItemFlag.SPIKES.getValue();
					equip.setFlag(flag);
					break;
				}
				case 2041058: // Cape for Cold protection
				{
					byte flag = equip.getFlag();
					flag |= ItemFlag.COLD.getValue();
					equip.setFlag(flag);
					break;
				}
				case 2049100: // Chaos Scroll
				case 2049101: // Liar's Wood Liquid
				case 2049102: // Maple Syrup
				case 2049104: // Angent Equipmenet scroll
				case 2049103:
					this.setRandomStats(equip);
					break;
				default:
					this.setDefaultStats(stats, equip);
					break;
				}
				if (!GameConstants.isCleanSlate(scrollId) && !GameConstants.isSpecialScroll(scrollId)) {
					equip.setUpgradeSlots((byte) (equip.getUpgradeSlots() - 1));
					equip.setLevel((byte) (equip.getLevel() + 1));
				}
			} else {
				if (!ws && !GameConstants.isCleanSlate(scrollId) && !GameConstants.isSpecialScroll(scrollId)) {
					equip.setUpgradeSlots((byte) (equip.getUpgradeSlots() - 1));
				}
				if (Randomizer.nextInt(99) < stats.get("cursed")) {
					return null;
				}
			}
		}
		return equipItem;
	}

	private void setDefaultStats(final Map<String, Integer> stats, final Equip equip) {
		for (final Entry<String, Integer> stat : stats.entrySet()) {
			final String key = stat.getKey();
			switch (key) {
			case "STR":
				equip.setStr((short) (equip.getStr() + stat.getValue().intValue()));
				break;
			case "DEX":
				equip.setDex((short) (equip.getDex() + stat.getValue().intValue()));
				break;
			case "INT":
				equip.setInt((short) (equip.getInt() + stat.getValue().intValue()));
				break;
			case "LUK":
				equip.setLuk((short) (equip.getLuk() + stat.getValue().intValue()));
				break;
			case "PAD":
				equip.setWatk((short) (equip.getWatk() + stat.getValue().intValue()));
				break;
			case "PDD":
				equip.setWdef((short) (equip.getWdef() + stat.getValue().intValue()));
				break;
			case "MAD":
				equip.setMatk((short) (equip.getMatk() + stat.getValue().intValue()));
				break;
			case "MDD":
				equip.setMdef((short) (equip.getMdef() + stat.getValue().intValue()));
				break;
			case "ACC":
				equip.setAcc((short) (equip.getAcc() + stat.getValue().intValue()));
				break;
			case "EVA":
				equip.setAvoid((short) (equip.getAvoid() + stat.getValue().intValue()));
				break;
			case "Speed":
				equip.setSpeed((short) (equip.getSpeed() + stat.getValue().intValue()));
				break;
			case "Jump":
				equip.setJump((short) (equip.getJump() + stat.getValue().intValue()));
				break;
			case "MHP":
				equip.setHp((short) (equip.getHp() + stat.getValue().intValue()));
				break;
			case "MMP":
				equip.setMp((short) (equip.getMp() + stat.getValue().intValue()));
				break;
			}
		}
		return;
	}

	private void setRandomStats(final Equip equip) {
		// Beach Sandals Scroll
		final int increase = Randomizer.nextBoolean() ? 1 : -1;

		if (equip.getStr() > 0) {
			equip.setStr((short) (equip.getStr() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getDex() > 0) {
			equip.setDex((short) (equip.getDex() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getInt() > 0) {
			equip.setInt((short) (equip.getInt() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getLuk() > 0) {
			equip.setLuk((short) (equip.getLuk() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getWatk() > 0) {
			equip.setWatk((short) (equip.getWatk() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getWdef() > 0) {
			equip.setWdef((short) (equip.getWdef() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getMatk() > 0) {
			equip.setMatk((short) (equip.getMatk() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getMdef() > 0) {
			equip.setMdef((short) (equip.getMdef() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getAcc() > 0) {
			equip.setAcc((short) (equip.getAcc() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getAvoid() > 0) {
			equip.setAvoid((short) (equip.getAvoid() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getSpeed() > 0) {
			equip.setSpeed((short) (equip.getSpeed() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getJump() > 0) {
			equip.setJump((short) (equip.getJump() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getHp() > 0) {
			equip.setHp((short) (equip.getHp() + Randomizer.nextInt(5) * increase));
		}
		if (equip.getMp() > 0) {
			equip.setMp((short) (equip.getMp() + Randomizer.nextInt(5) * increase));
		}
	}

	public final Item getEquipById(final int equipId) {
		return this.getEquipById(equipId, -1);
	}

	public final Item getEquipById(final int equipId, final int ringId) {
		final Equip nEquip = new Equip(equipId, (byte) 0, ringId, (byte) 0);
		nEquip.setQuantity((short) 1);
		final Map<String, Integer> stats = this.getEquipStats(equipId);
		if (stats != null) {
			for (final Entry<String, Integer> stat : stats.entrySet()) {
				final String key = stat.getKey();
				final int statValue = stat.getValue().intValue();
				switch (key) {
				case "STR":
					nEquip.setStr((short) statValue);
					break;
				case "DEX":
					nEquip.setDex((short) statValue);
					break;
				case "INT":
					nEquip.setInt((short) statValue);
					break;
				case "LUK":
					nEquip.setLuk((short) statValue);
					break;
				case "PAD":
					nEquip.setWatk((short) statValue);
					break;
				case "PDD":
					nEquip.setWdef((short) statValue);
					break;
				case "MAD":
					nEquip.setMatk((short) statValue);
					break;
				case "MDD":
					nEquip.setMdef((short) statValue);
					break;
				case "ACC":
					nEquip.setAcc((short) statValue);
					break;
				case "EVA":
					nEquip.setAvoid((short) statValue);
					break;
				case "Speed":
					nEquip.setSpeed((short) statValue);
					break;
				case "Jump":
					nEquip.setJump((short) statValue);
					break;
				case "MHP":
					nEquip.setHp((short) statValue);
					break;
				case "MMP":
					nEquip.setMp((short) statValue);
					break;
				case "tuc":
					nEquip.setUpgradeSlots((byte) statValue);
					break;
				case "Craft":
					nEquip.setHands((short) statValue);
					break;
				}
			}
		}
		this.equipCache.put(equipId, nEquip);
		return nEquip.copy();
	}

	private short getRandStat(final short defaultValue, final int maxRange) {
		if (defaultValue == 0) {
			return 0;
		}
		// vary no more than ceil of 10% of stat
		final int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);

		return (short) (defaultValue - lMaxRange + Math.floor(Math.random() * (lMaxRange * 2 + 1)));
	}

	public final Equip randomizeStats(final Equip equip) {
		equip.setStr(this.getRandStat(equip.getStr(), 5));
		equip.setDex(this.getRandStat(equip.getDex(), 5));
		equip.setInt(this.getRandStat(equip.getInt(), 5));
		equip.setLuk(this.getRandStat(equip.getLuk(), 5));
		equip.setMatk(this.getRandStat(equip.getMatk(), 5));
		equip.setWatk(this.getRandStat(equip.getWatk(), 5));
		equip.setAcc(this.getRandStat(equip.getAcc(), 5));
		equip.setAvoid(this.getRandStat(equip.getAvoid(), 5));
		equip.setJump(this.getRandStat(equip.getJump(), 5));
		equip.setHands(this.getRandStat(equip.getHands(), 5));
		equip.setSpeed(this.getRandStat(equip.getSpeed(), 5));
		equip.setWdef(this.getRandStat(equip.getWdef(), 10));
		equip.setMdef(this.getRandStat(equip.getMdef(), 10));
		equip.setHp(this.getRandStat(equip.getHp(), 10));
		equip.setMp(this.getRandStat(equip.getMp(), 10));
		return equip;
	}

	public final StatEffect getItemEffect(final int itemId) {
		StatEffect ret = this.itemEffects.get(Integer.valueOf(itemId));
		if (ret == null) {
			final WzData item = this.getItemData(itemId);
			if (item == null) {
				return null;
			}
			ret = StatEffect.loadItemEffectFromData(item.getChildByPath("spec"), itemId);
			this.itemEffects.put(Integer.valueOf(itemId), ret);
		}
		return ret;
	}

	public final List<IdProbabilityEntry> getSummonMobs(final int itemId) {
		if (this.summonMobCache.containsKey(Integer.valueOf(itemId))) {
			return this.summonMobCache.get(itemId);
		}
		if (!GameConstants.isSummonSack(itemId)) {
			return null;
		}
		final WzData data = this.getItemData(itemId).getChildByPath("mob");
		if (data == null) {
			return null;
		}
		final List<IdProbabilityEntry> mobPairs = Lists.newArrayList();

		for (final WzData child : data.getChildren()) {
			mobPairs.add(new IdProbabilityEntry(WzDataTool.getIntConvert("id", child), WzDataTool.getIntConvert("prob", child)));
		}
		this.summonMobCache.put(itemId, mobPairs);
		return mobPairs;
	}

	public final int getCardMobId(final int id) {
		if (id == 0) {
			return 0;
		}
		if (this.monsterBookID.containsKey(id)) {
			return this.monsterBookID.get(id);
		}
		final WzData data = this.getItemData(id);
		final int monsterid = WzDataTool.getIntConvert("info/mob", data, 0);

		if (monsterid == 0) { // Hack.
			return 0;
		}
		this.monsterBookID.put(id, monsterid);
		return this.monsterBookID.get(id);
	}

	public final int getWatkForProjectile(final int itemId) {
		Integer atk = this.projectileWatkCache.get(itemId);
		if (atk != null) {
			return atk.intValue();
		}
		final WzData data = this.getItemData(itemId);
		atk = Integer.valueOf(WzDataTool.getInt("info/incPAD", data, 0));
		this.projectileWatkCache.put(itemId, atk);
		return atk.intValue();
	}

	public final boolean canScroll(final int scrollId, final int itemId) {
		return scrollId / 100 % 100 == itemId / 10000 % 100;
	}

	public final String getName(final int itemId) {
		if (this.nameCache.containsKey(itemId)) {
			return this.nameCache.get(itemId);
		}
		final WzData strings = this.getStringData(itemId);
		if (strings == null) {
			return null;
		}
		final String ret = WzDataTool.getString("name", strings, null);
		this.nameCache.put(itemId, ret);
		return ret;
	}

	public final String getDesc(final int itemId) {
		if (this.descCache.containsKey(itemId)) {
			return this.descCache.get(itemId);
		}
		final WzData strings = this.getStringData(itemId);
		if (strings == null) {
			return null;
		}
		final String ret = WzDataTool.getString("desc", strings, null);
		this.descCache.put(itemId, ret);
		return ret;
	}

	public final String getMsg(final int itemId) {
		if (this.msgCache.containsKey(itemId)) {
			return this.msgCache.get(itemId);
		}
		final WzData strings = this.getStringData(itemId);
		if (strings == null) {
			return null;
		}
		final String ret = WzDataTool.getString("msg", strings, null);
		this.msgCache.put(itemId, ret);
		return ret;
	}

	public final short getItemMakeLevel(final int itemId) {
		if (this.itemMakeLevel.containsKey(itemId)) {
			return this.itemMakeLevel.get(itemId);
		}
		if (itemId / 10000 != 400) {
			return 0;
		}
		final short lvl = (short) WzDataTool.getIntConvert("info/lv", this.getItemData(itemId), 0);
		this.itemMakeLevel.put(itemId, lvl);
		return lvl;
	}

	public final ItemConsumeType isConsumeOnPickup(final int itemId) {
		// 0 = not, 1 = consume on pickup, 2 = consume + party
		if (this.consumeOnPickupCache.containsKey(itemId)) {
			return this.consumeOnPickupCache.get(itemId);
		}
		final WzData data = this.getItemData(itemId);
		byte consume = (byte) WzDataTool.getIntConvert("spec/consumeOnPickup", data, 0);
		if (consume == 0) {
			consume = (byte) WzDataTool.getIntConvert("specEx/consumeOnPickup", data, 0);
		}
		if (consume == 1) {
			if (WzDataTool.getIntConvert("spec/party", this.getItemData(itemId), 0) > 0) {
				consume = 2;
			}
		}
		final ItemConsumeType type = ItemConsumeType.fromNumber(consume);
		this.consumeOnPickupCache.put(itemId, type);
		return type;
	}

	public final boolean isDropRestricted(final int itemId) {
		if (this.dropRestrictionCache.containsKey(itemId)) {
			return this.dropRestrictionCache.get(itemId);
		}
		final WzData data = this.getItemData(itemId);

		boolean trade = false;
		if (WzDataTool.getIntConvert("info/tradeBlock", data, 0) == 1 || WzDataTool.getIntConvert("info/quest", data, 0) == 1) {
			trade = true;
		}
		this.dropRestrictionCache.put(itemId, trade);
		return trade;
	}

	public final boolean isPickupRestricted(final int itemId) {
		if (this.pickupRestrictionCache.containsKey(itemId)) {
			return this.pickupRestrictionCache.get(itemId);
		}
		final boolean isRestricted = WzDataTool.getIntConvert("info/only", this.getItemData(itemId), 0) == 1;

		this.pickupRestrictionCache.put(itemId, isRestricted);
		return isRestricted;
	}

	public final int getStateChangeItem(final int itemId) {
		if (this.stateChangeCache.containsKey(itemId)) {
			return this.stateChangeCache.get(itemId);
		}
		final int triggerItem = WzDataTool.getIntConvert("info/stateChangeItem", this.getItemData(itemId), 0);
		this.stateChangeCache.put(itemId, triggerItem);
		return triggerItem;
	}

	public final boolean isKarmaEnabled(final int itemId, final int karmaID) {
		if (this.karmaEnabledCache.containsKey(itemId)) {
			return this.karmaEnabledCache.get(itemId) == karmaID % 10 + 1;
		}
		final int bRestricted = WzDataTool.getIntConvert("info/tradeAvailable", this.getItemData(itemId), 0);

		this.karmaEnabledCache.put(itemId, bRestricted);
		return bRestricted == karmaID % 10 + 1;
	}

	public final RewardSetInfo getRewardSet(final int itemId) {
		if (this.RewardItem.containsKey(itemId)) {
			return this.RewardItem.get(itemId);
		}
		final WzData data = this.getItemData(itemId);
		if (data == null) {
			return null;
		}
		final WzData rewards = data.getChildByPath("reward");
		if (rewards == null) {
			return null;
		}
		final List<RewardItemInfo> all = new ArrayList<RewardItemInfo>();

		for (final WzData reward : rewards) {
			final RewardItemInfo info = new RewardItemInfo(reward);

			all.add(info);
		}
		final RewardSetInfo toreturn = new RewardSetInfo(all);
		this.RewardItem.put(itemId, toreturn);
		return toreturn;
	}

	public final Map<String, Integer> getSkillStats(final int itemId) {
		if (this.SkillStatsCache.containsKey(itemId)) {
			return this.SkillStatsCache.get(itemId);
		}
		if (!(itemId / 10000 == 228 || itemId / 10000 == 229)) { // Skillbook and mastery book
			return null;
		}
		final WzData item = this.getItemData(itemId);
		if (item == null) {
			return null;
		}
		final WzData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		final Map<String, Integer> ret = Maps.newLinkedHashMap();
		for (final WzData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3), WzDataTool.getIntConvert(data));
			}
		}
		ret.put("masterLevel", WzDataTool.getInt("masterLevel", info, 0));
		ret.put("reqSkillLevel", WzDataTool.getInt("reqSkillLevel", info, 0));
		ret.put("success", WzDataTool.getInt("success", info, 0));

		final WzData skill = info.getChildByPath("skill");

		for (int i = 0; i < skill.getChildren().size(); i++) { // List of allowed skillIds
			ret.put("skillid" + i, WzDataTool.getInt(Integer.toString(i), skill, 0));
		}
		this.SkillStatsCache.put(itemId, ret);
		return ret;
	}

	public final List<Integer> petsCanConsume(final int itemId) {
		final List<Integer> ret = Lists.newArrayList();
		final WzData data = this.getItemData(itemId);
		int curPetId = 0;
		final int size = data.getChildren().size();
		for (int i = 0; i < size; i++) {
			curPetId = WzDataTool.getInt("spec/" + Integer.toString(i), data, 0);
			if (curPetId == 0) {
				break;
			}
			ret.add(Integer.valueOf(curPetId));
		}
		return ret;
	}

	public final boolean isQuestItem(final int itemId) {
		if (this.isQuestItemCache.containsKey(itemId)) {
			return this.isQuestItemCache.get(itemId);
		}
		final boolean questItem = WzDataTool.getIntConvert("info/quest", this.getItemData(itemId), 0) == 1;
		this.isQuestItemCache.put(itemId, questItem);
		return questItem;
	}

	public final boolean isCash(final int itemid) {
		if (this.getEquipStats(itemid) == null) {
			return GameConstants.getInventoryType(itemid) == InventoryType.CASH;
		}
		return GameConstants.getInventoryType(itemid) == InventoryType.CASH || this.getEquipStats(itemid).get("cash") > 0;
	}

	public Equip hardcoreItem(final Equip equip, final short stat) {
		equip.setStr(stat);
		equip.setDex(stat);
		equip.setInt(stat);
		equip.setLuk(stat);
		equip.setMatk(stat);
		equip.setWatk(stat);
		equip.setAcc(stat);
		equip.setAvoid(stat);
		equip.setJump(stat);
		equip.setSpeed(stat);
		equip.setWdef(stat);
		equip.setMdef(stat);
		equip.setHp(stat);
		equip.setMp(stat);
		return equip;
	}
}
