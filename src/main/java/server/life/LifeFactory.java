package server.life;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import database.DatabaseConnection;

import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataProviderFactory;
import provider.WzDataTool;
import provider.xml.WzDataType;
import tools.Pair;
import tools.StringUtil;

public class LifeFactory {

	private static final WzDataProvider data = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/Mob.wz"));
	private static final WzDataProvider stringDataWZ = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/String.wz"));
	private static final WzDataProvider etcDataWZ = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/Etc.wz"));
	private static final WzData mobStringData = stringDataWZ.getData("Mob.img");
	private static final WzData npcStringData = stringDataWZ.getData("Npc.img");
	private static final WzData npclocData = etcDataWZ.getData("NpcLocation.img");
	private static Map<Integer, MonsterStats> monsterStats = new HashMap<Integer, MonsterStats>();
	private static Map<Integer, Integer> NPCLoc = new HashMap<Integer, Integer>();

	public static AbstractLoadedGameLife getLife(int id, String type) {
	if (type.equalsIgnoreCase("n")) {
		return getNPC(id);
	} else if (type.equalsIgnoreCase("m")) {
		return getMonster(id);
	} else {
		System.err.println("Unknown Life type: " + type + "");
		return null;
	}
	}

	public static int getNPCLocation(int npcid) {
	if (NPCLoc.containsKey(npcid)) {
		return NPCLoc.get(npcid);
	}
	final int map = WzDataTool.getIntConvert(Integer.toString(npcid) + "/0", npclocData);
	NPCLoc.put(npcid, map);
	return map;
	}

	public static Monster getMonster(int mid) {
	MonsterStats stats = monsterStats.get(Integer.valueOf(mid));

	if (stats == null) {
		WzData monsterData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
		if (monsterData == null) {
		return null;
		}
		WzData monsterInfoData = monsterData.getChildByPath("info");
		stats = new MonsterStats();

		stats.setHp(WzDataTool.getIntConvert("maxHP", monsterInfoData));
		stats.setMp(WzDataTool.getIntConvert("maxMP", monsterInfoData, 0));
		stats.setExp(WzDataTool.getIntConvert("exp", monsterInfoData, 0));
		stats.setLevel((short) WzDataTool.getIntConvert("level", monsterInfoData));
		stats.setRemoveAfter(WzDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
		stats.setrareItemDropLevel((byte) WzDataTool.getIntConvert("rareItemDropLevel", monsterInfoData, 0));
		stats.setFixedDamage(WzDataTool.getIntConvert("fixedDamage", monsterInfoData, -1));
		stats.setOnlyNormalAttack(WzDataTool.getIntConvert("onlyNormalAttack", monsterInfoData, 0) > 0);
		stats.setBoss(WzDataTool.getIntConvert("boss", monsterInfoData, 0) > 0 || mid == 8810018 || mid == 9410066);
		stats.setExplosiveReward(WzDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
		stats.setFfaLoot(WzDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
		stats.setUndead(WzDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
		stats.setName(WzDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
		stats.setBuffToGive(WzDataTool.getIntConvert("buff", monsterInfoData, -1));
		stats.setFriendly(WzDataTool.getIntConvert("damagedByMob", monsterInfoData, 0) > 0);
		stats.setCP((byte) WzDataTool.getIntConvert("getCP", monsterInfoData, 0));
		stats.setPhysicalDefense((short) WzDataTool.getIntConvert("PDDamage", monsterInfoData, 0));
		stats.setMagicDefense((short) WzDataTool.getIntConvert("MDDamage", monsterInfoData, 0));
		stats.setEva((short) WzDataTool.getIntConvert("eva", monsterInfoData, 0));

		final WzData selfd = monsterInfoData.getChildByPath("selfDestruction");
		if (selfd != null) {
		stats.setSelfDHP(WzDataTool.getIntConvert("hp", selfd, 0));
		stats.setSelfD((byte) WzDataTool.getIntConvert("action", selfd, -1));
		} else {
		stats.setSelfD((byte) -1);
		}

		final WzData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
		if (firstAttackData != null) {
		if (firstAttackData.getType() == WzDataType.FLOAT) {
			stats.setFirstAttack(Math.round(WzDataTool.getFloat(firstAttackData)) > 0);
		} else {
			stats.setFirstAttack(WzDataTool.getInt(firstAttackData) > 0);
		}
		}
		if (stats.isBoss() || isDmgSponge(mid)) {
		if (monsterInfoData.getChildByPath("hpTagColor") == null || monsterInfoData.getChildByPath("hpTagBgcolor") == null) {
			stats.setTagColor(0);
			stats.setTagBgColor(0);
		} else {
			stats.setTagColor(WzDataTool.getIntConvert("hpTagColor", monsterInfoData));
			stats.setTagBgColor(WzDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
		}
		}

		final WzData banishData = monsterInfoData.getChildByPath("ban");
		if (banishData != null) {
		stats.setBanishInfo(new BanishInfo(
			WzDataTool.getString("banMsg", banishData),
			WzDataTool.getInt("banMap/0/field", banishData, -1),
			WzDataTool.getString("banMap/0/portal", banishData, "sp")));
		}

		final WzData reviveInfo = monsterInfoData.getChildByPath("revive");
		if (reviveInfo != null) {
		List<Integer> revives = new LinkedList<Integer>();
		for (WzData bdata : reviveInfo) {
			revives.add(WzDataTool.getInt(bdata));
		}
		stats.setRevives(revives);
		}

		final WzData monsterSkillData = monsterInfoData.getChildByPath("skill");
		if (monsterSkillData != null) {
		int i = 0;
		List<Pair<Integer, Integer>> skills = new ArrayList<Pair<Integer, Integer>>();
		while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
			skills.add(new Pair<Integer, Integer>(Integer.valueOf(WzDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(WzDataTool.getInt(i + "/level", monsterSkillData, 0))));
			i++;
		}
		stats.setSkills(skills);
		}

		decodeElementalString(stats, WzDataTool.getString("elemAttr", monsterInfoData, ""));

		// Other data which isn;t in the mob, but might in the linked data

		final int link = WzDataTool.getIntConvert("link", monsterInfoData, 0);
		if (link != 0) { // Store another copy, for faster processing.
		monsterData = data.getData(StringUtil.getLeftPaddedStr(link + ".img", '0', 11));
		monsterInfoData = monsterData.getChildByPath("info");
		}

		for (WzData idata : monsterData) {
		if (idata.getName().equals("fly")) {
			stats.setFly(true);
			stats.setMobile(true);
			break;
		} else if (idata.getName().equals("move")) {
			stats.setMobile(true);
		}
		}
		
		byte hpdisplaytype = -1;
		if (stats.getTagColor() > 0) {
		hpdisplaytype = 0;
		} else if (stats.isFriendly()) {
		hpdisplaytype = 1;
		} else if (mid >= 9300184 && mid <= 9300215) { // Mulung TC mobs
		hpdisplaytype = 2;
		} else if (!stats.isBoss() || mid == 9410066) { // Not boss and dong dong chiang
		hpdisplaytype = 3;
		}
		stats.setHPDisplayType(hpdisplaytype);

		monsterStats.put(Integer.valueOf(mid), stats);
	}
	return new Monster(mid, stats);
	}

	public static final void decodeElementalString(MonsterStats stats, String elemAttr) {
	for (int i = 0; i < elemAttr.length(); i += 2) {
		stats.setEffectiveness(
			Element.getFromChar(elemAttr.charAt(i)),
			ElementalEffectiveness.getByNumber(Integer.valueOf(String.valueOf(elemAttr.charAt(i + 1)))));
	}
	}

	private static final boolean isDmgSponge(final int mid) {
	switch (mid) {
		case 8810018:
		case 8820010:
		case 8820011:
		case 8820012:
		case 8820013:
		case 8820014:
		return true;
	}
	return false;
	}

	public static Npc getNPC(final int nid) {
	if (nid >= 9901000 && nid <= 9901551) {
		final NpcStats stats = new NpcStats("", true);
		final Npc npc = new Npc(nid, stats);

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
		Connection con = DatabaseConnection.getConnection();
		ps = con.prepareStatement("SELECT * FROM playernpcs WHERE npcid = ?");
		ps.setInt(1, nid);

		rs = ps.executeQuery();
		if (rs.next()) {
			stats.setCY(rs.getInt("cy"));
			stats.setName(rs.getString("name"));
			stats.setHair(rs.getInt("hair"));
			stats.setFace(rs.getInt("face"));
			stats.setSkin(rs.getByte("skin"));
			stats.setFH(rs.getInt("Foothold"));
			stats.setRX0(rs.getInt("rx0"));
			stats.setRX1(rs.getInt("rx1"));
			npc.setPosition(new Point(rs.getInt("x"), stats.getCY()));
			ps.close();
			rs.close();

			ps = con.prepareStatement("SELECT * FROM playernpcs_equip WHERE npcid = ?");
			ps.setInt(1, rs.getInt("id"));
			rs = ps.executeQuery();

			Map<Byte, Integer> equips = new HashMap<Byte, Integer>();

			while (rs.next()) {
			equips.put(rs.getByte("equippos"), rs.getInt("equipid"));
			}
			stats.setEquips(equips);
			rs.close();
			ps.close();
		}
		} catch (SQLException ex) {
		} finally {
		try {
			if (ps != null) {
			ps.close();
			}
			if (rs != null) {
			rs.close();
			}
		} catch (SQLException ignore) {
		}
		}
		return npc;
	} else {
		final String name = WzDataTool.getString(nid + "/name", npcStringData, "MISSINGNO");
		return new Npc(nid, new NpcStats(name, false));
	}
	}
}