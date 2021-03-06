package javastory.login;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javastory.game.Equip;
import javastory.game.Item;
import javastory.wz.WzData;
import javastory.wz.WzDataDirectoryEntry;
import javastory.wz.WzDataFileEntry;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public final class LoginInfoProvider {

	private final static LoginInfoProvider instance = new LoginInfoProvider();
	private final Map<Integer, Map<String, Integer>> equipStatsCache = Maps.newHashMap();
	private final Map<Integer, Equip> equipCache = Maps.newHashMap();
	private final List<String> nameFilter = Lists.newArrayList();

	public static LoginInfoProvider getInstance() {
		return instance;
	}

	private LoginInfoProvider() {
		System.out.println(":: Loading LoginInformationProvider ::");

		final int[] LoadEquipment = { 1040002, 1040006, 1040010, // top
		1060006, 1060002, 1060138, // Bottom
			1041002, 1041006, 1041010, 1041011, 1042167, 1042180, // Top
			1061002, 1061008, 1062115, 1061160, // Bottom
			1302000, 1322005, 1312004, 1442079, 1302132, // Weapon
			1072001, 1072005, 1072037, 1072038, 1072383, 1072418 // Shoes
			};
		final WzDataProvider equipData = WzDataProviderFactory.getDataProvider("Character.wz");
		for (final int element : LoadEquipment) {
			this.loadEquipStats(element, equipData);
		}

		final WzData nameData = WzDataProviderFactory.getDataProvider("Etc.wz").getData("ForbiddenName.img");
		for (final WzData data : nameData.getChildren()) {
			this.nameFilter.add(WzDataTool.getString(data));
		}
	}

	private void loadEquipStats(final int itemId, final WzDataProvider equipData) {
		final WzData item = this.getItemData(itemId, equipData);
		if (item == null) {
			return;
		}

		final WzData info = item.getChildByPath("info");
		if (info == null) {
			return;
		}

		final Map<String, Integer> ret = Maps.newLinkedHashMap();
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
		ret.put("cash", WzDataTool.getInt("cash", info, 0));
		ret.put("cursed", WzDataTool.getInt("cursed", info, 0));
		ret.put("success", WzDataTool.getInt("success", info, 0));
		this.equipStatsCache.put(itemId, ret);
	}

	private WzData getItemData(final int itemId, final WzDataProvider equipData) {
		final WzData ret = null;
		final String idStr = "0" + String.valueOf(itemId);
		final WzDataDirectoryEntry root = equipData.getRoot();
		for (final WzDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final WzDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					return equipData.getData(topDir.getName() + "/" + iFile.getName());
				}
			}
		}
		return ret;
	}

	public final Item getEquipById(final int equipId) {
		final Equip equip = new Equip(equipId, (byte) 0, -1, (byte) 0);
		equip.setQuantity((short) 1);
		final Map<String, Integer> stats = this.equipStatsCache.get(equipId);
		if (stats != null) {
			for (final Entry<String, Integer> stat : stats.entrySet()) {
				final String key = stat.getKey();
				switch (key) {
				case "STR":
					equip.setStr(stat.getValue().shortValue());
					break;
				case "DEX":
					equip.setDex(stat.getValue().shortValue());
					break;
				case "INT":
					equip.setInt(stat.getValue().shortValue());
					break;
				case "LUK":
					equip.setLuk(stat.getValue().shortValue());
					break;
				case "PAD":
					equip.setWatk(stat.getValue().shortValue());
					break;
				case "PDD":
					equip.setWdef(stat.getValue().shortValue());
					break;
				case "MAD":
					equip.setMatk(stat.getValue().shortValue());
					break;
				case "MDD":
					equip.setMdef(stat.getValue().shortValue());
					break;
				case "ACC":
					equip.setAcc(stat.getValue().shortValue());
					break;
				case "EVA":
					equip.setAvoid(stat.getValue().shortValue());
					break;
				case "Speed":
					equip.setSpeed(stat.getValue().shortValue());
					break;
				case "Jump":
					equip.setJump(stat.getValue().shortValue());
					break;
				case "MHP":
					equip.setHp(stat.getValue().shortValue());
					break;
				case "MMP":
					equip.setMp(stat.getValue().shortValue());
					break;
				case "tuc":
					equip.setUpgradeSlots(stat.getValue().byteValue());
					break;
				case "afterImage":
					break;
				}
			}
		}
		this.equipCache.put(equipId, equip);
		return equip.copy();
	}

	public final boolean isAllowedName(final String in) {
		for (final String name : this.nameFilter) {
			if (in.contains(name)) {
				return false;
			}
		}
		return true;
	}
}