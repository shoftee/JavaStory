package javastory.channel.life;

import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javastory.db.Database;
import javastory.game.data.MobInfo;
import javastory.game.data.NpcInfo;
import javastory.tools.StringUtil;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public final class LifeFactory {

	private static final WzDataProvider dataRoot = WzDataProviderFactory.getDataProvider("Mob.wz");
	private static final WzDataProvider stringRoot = WzDataProviderFactory.getDataProvider("String.wz");
	private static final WzDataProvider etcRoot = WzDataProviderFactory.getDataProvider("Etc.wz");
	private static final WzData npcStrings = stringRoot.getData("Npc.img");
	private static final WzData npcLocationData = etcRoot.getData("NpcLocation.img");

	private static final Map<Integer, MobInfo> monsterInfo = new HashMap<>();
	private static final Map<Integer, Integer> npcLocations = new HashMap<>();

	public static AbstractLoadedLife getLife(int id, String type) {
		String typeUpper = type.toUpperCase();
		switch (typeUpper) {
		case "N":
			return getNpc(id);
		case "M":
			return getMonster(id);
		default:
			System.err.println("Unknown Life type: " + type + "");
			return null;
		}
	}

	public static int getNpcLocation(int npcId) {
		if (npcLocations.containsKey(npcId)) {
			return npcLocations.get(npcId);
		}
		final int map = WzDataTool.getIntConvert(Integer.toString(npcId) + "/0", npcLocationData);
		npcLocations.put(npcId, map);
		return map;
	}

	public static Monster getMonster(int mobId) {
		MobInfo info = monsterInfo.get(Integer.valueOf(mobId));

		if (info == null) {
			info = loadMonster(mobId);
		}
		return new Monster(info);
	}

	private static MobInfo loadMonster(int mobId) {
		WzData monsterData = getMonsterData(mobId);
		if (monsterData == null) {
			return null;
		}

		final int linkedMobId = WzDataTool.getIntConvert("info/link", monsterData, 0);

		WzData linkedData = null;
		if (linkedMobId != 0) {
			linkedData = getMonsterData(linkedMobId);
		}

		final MobInfo info = new MobInfo(mobId, monsterData, linkedData);

		monsterInfo.put(Integer.valueOf(mobId), info);
		return info;
	}

	private static WzData getMonsterData(int mobId) {
		final String paddedId = StringUtil.getLeftPaddedStr(Integer.toString(mobId), '0', 7);
		WzData monsterData = dataRoot.getData(paddedId + ".img");
		return monsterData;
	}

	public static Npc getNpc(final int lifeId) {
		if (isPlayerNpc(lifeId)) {
			final NpcInfo info = new NpcInfo("", true);
			final Npc npc = new Npc(lifeId, info);

			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				Connection con = Database.getConnection();
				ps = con.prepareStatement("SELECT * FROM playernpcs WHERE npcid = ?");
				ps.setInt(1, lifeId);

				rs = ps.executeQuery();
				if (rs.next()) {
					info.setCY(rs.getInt("cy"));
					info.setName(rs.getString("name"));
					info.setHair(rs.getInt("hair"));
					info.setFace(rs.getInt("face"));
					info.setSkin(rs.getByte("skin"));
					info.setFH(rs.getInt("Foothold"));
					info.setRX0(rs.getInt("rx0"));
					info.setRX1(rs.getInt("rx1"));
					npc.setPosition(new Point(rs.getInt("x"), info.getCY()));
					ps.close();
					rs.close();

					ps = con.prepareStatement("SELECT * FROM playernpcs_equip WHERE npcid = ?");
					ps.setInt(1, rs.getInt("id"));
					rs = ps.executeQuery();

					Map<Byte, Integer> equips = new HashMap<>();

					while (rs.next()) {
						equips.put(rs.getByte("equippos"), rs.getInt("equipid"));
					}
					info.setEquips(equips);
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
			final String name = WzDataTool.getString(lifeId + "/name", npcStrings, "MISSINGNO");
			return new Npc(lifeId, new NpcInfo(name, false));
		}
	}

	private static boolean isPlayerNpc(final int lifeId) {
		return lifeId >= 9901000 && lifeId <= 9901551;
	}
}
