package javastory.channel.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.life.AbstractLoadedLife;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.server.Portal;
import javastory.channel.server.PortalFactory;
import javastory.tools.StringUtil;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class GameMapFactory {

	private static final WzDataProvider source = WzDataProviderFactory.getDataProvider("Map.wz");

	private final Map<Integer, GameMap> maps;
	private final WeakHashMap<Integer, GameMap> instanceMap;

	private final ReentrantLock lock;

	public GameMapFactory() {
		this.maps = Maps.newHashMap();
		this.instanceMap = new WeakHashMap<>();

		this.lock = new ReentrantLock();
	}

	public final GameMap getMap(final int mapId) {
		return getMap(mapId, true, true, true);
	}

	// backwards-compatible
	public final GameMap getMap(final int mapId, final boolean respawns, final boolean npcs) {
		return getMap(mapId, respawns, npcs, true);
	}

	public final GameMap getMap(final int mapId, final boolean mobsRespawn, final boolean loadNpcs, final boolean loadReactors) {
		Integer mapIdInt = Integer.valueOf(mapId);
		GameMap map = maps.get(mapIdInt);
		if (map == null) {
			lock.lock();
			try {
				// check if someone else who was also synchronized has loaded
				// the map already
				map = maps.get(mapIdInt);
				if (map != null) {
					return map;
				}

				map = loadMap(mapId, mobsRespawn, loadNpcs, loadReactors);

				maps.put(mapIdInt, map);
			} finally {
				lock.unlock();
			}
		}
		return map;
	}

	private GameMap loadMap(final int mapId, final boolean mobsRespawn, final boolean loadNpcs, final boolean loadReactors) {

		WzData mapData = source.getData(getMapName(mapId));
		WzData link = mapData.getChildByPath("info/link");
		if (link != null) {
			mapData = source.getData(getMapName(WzDataTool.getIntConvert("info/link", mapData)));
		}

		float monsterRate = 0;
		if (mobsRespawn) {
			WzData mobRate = mapData.getChildByPath("info/mobRate");
			if (mobRate != null) {
				monsterRate = ((Float) mobRate.getData()).floatValue();
			}
		}

		final int returnMapId = WzDataTool.getInt("info/returnMap", mapData);
		final GameMap map = new GameMap(mapId, returnMapId, monsterRate);

		PortalFactory portalFactory = new PortalFactory();
		for (WzData portalData : mapData.getChildByPath("portal")) {
			final int portalType = WzDataTool.getInt("pt", portalData);
			final Portal portal = portalFactory.makePortal(portalType, portalData);

			map.addPortal(portal);
		}

		List<Foothold> footholds = Lists.newLinkedList();
		Point lBound = new Point();
		Point uBound = new Point();
		for (WzData fhRoot : mapData.getChildByPath("foothold")) {
			for (WzData fhCategory : fhRoot) {
				for (WzData fhData : fhCategory) {
					final Foothold fh = new Foothold(fhData);

					if (fh.getX1() < lBound.x) {
						lBound.x = fh.getX1();
					}
					if (fh.getX2() > uBound.x) {
						uBound.x = fh.getX2();
					}
					if (fh.getY1() < lBound.y) {
						lBound.y = fh.getY1();
					}
					if (fh.getY2() > uBound.y) {
						uBound.y = fh.getY2();
					}

					footholds.add(fh);
				}
			}
		}

		FootholdTree fTree = new FootholdTree(lBound, uBound);
		for (Foothold item : footholds) {
			fTree.insert(item);
		}
		map.setFootholds(fTree);

		// load areas (EG PQ platforms)
		if (mapData.getChildByPath("area") != null) {
			for (WzData area : mapData.getChildByPath("area")) {
				final int x1 = WzDataTool.getInt(area.getChildByPath("x1"));
				final int y1 = WzDataTool.getInt(area.getChildByPath("y1"));
				final int x2 = WzDataTool.getInt(area.getChildByPath("x2"));
				final int y2 = WzDataTool.getInt(area.getChildByPath("y2"));
				final Rectangle mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
				map.addMapleArea(mapArea);
			}
		}

		String message = null;
		final WzData timeMob = mapData.getChildByPath("info/timeMob");
		if (timeMob != null) {
			message = WzDataTool.getString("message", timeMob, null);
		}

		// load life data (npc, monsters)
		for (WzData life : mapData.getChildByPath("life")) {
			final String type = WzDataTool.getString("type", life);
			if (loadNpcs || !type.equals("n")) {
				final String id = WzDataTool.getString("id", life);
				final AbstractLoadedLife myLife = loadLife(life, id, type);
				if (myLife instanceof Monster) {
					final Monster mob = (Monster) myLife;
					final int mobTime = WzDataTool.getInt("mobTime", life, 0);
					final byte team = (byte) WzDataTool.getInt("team", life, -1);
					map.addMonsterSpawn(mob, mobTime, team, message);
				} else {
					boolean bAdd = true;
					if (bAdd) {
						map.addMapObject(myLife);
					}
				}
			}
		}
		addAreaBossSpawn(map);
		map.loadMonsterRate(true);

		// load reactor data
		if (loadReactors && mapData.getChildByPath("reactor") != null) {
			for (WzData reactor : mapData.getChildByPath("reactor")) {
				final String id = WzDataTool.getString("id", reactor);
				if (id == null)
					continue;

				final byte facingDirection = (byte) WzDataTool.getInt("f", reactor, 0);
				map.spawnReactor(loadReactor(reactor, id, facingDirection));
			}
		}

		map.setClock(mapData.getChildByPath("clock") != null);
		map.setEverlast(mapData.getChildByPath("info/everlast") != null);
		map.setTown(mapData.getChildByPath("info/town") != null);

		map.setForcedReturnMap(WzDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));

		map.setHPDec(WzDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
		map.setHPDecProtect(WzDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
		map.setTimeLimit(WzDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
		map.setFieldLimit(WzDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
		map.setRecoveryRate(WzDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));

		map.setPersonalShop(mapData.getChildByPath("info/personalShop") != null);
		map.setCreateMobInterval((short) WzDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));

		map.setFirstUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
		map.setUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
		return map;
	}

	public GameMap getInstanceMap(final int instanceid) {
		return instanceMap.get(instanceid);
	}

	public void removeInstanceMap(final int instanceid) {
		instanceMap.remove(instanceid);
	}

	public GameMap createInstanceMap(int mapId, boolean mobsRespawn, boolean loadNpcs, boolean loadReactors, int instanceId) {
		WzData mapData = source.getData(getMapName(mapId));
		WzData link = mapData.getChildByPath("info/link");
		if (link != null) {
			mapData = source.getData(getMapName(WzDataTool.getIntConvert("info/link", mapData)));
		}

		float monsterRate = 0;
		if (mobsRespawn) {
			WzData mobRate = mapData.getChildByPath("info/mobRate");
			if (mobRate != null) {
				monsterRate = ((Float) mobRate.getData()).floatValue();
			}
		}

		GameMap map = new GameMap(mapId, WzDataTool.getInt("info/returnMap", mapData), monsterRate);

		PortalFactory portalFactory = new PortalFactory();
		for (WzData portalData : mapData.getChildByPath("portal")) {
			final int portalType = WzDataTool.getInt("pt", portalData);
			final Portal portal = portalFactory.makePortal(portalType, portalData);

			map.addPortal(portal);
		}

		List<Foothold> footholds = Lists.newLinkedList();
		Point lBound = new Point();
		Point uBound = new Point();
		for (WzData fhRoot : mapData.getChildByPath("foothold")) {
			for (WzData fhCategory : fhRoot) {
				for (WzData fhData : fhCategory) {
					Foothold fh = new Foothold(fhData);

					if (fh.getX1() < lBound.x) {
						lBound.x = fh.getX1();
					}
					if (fh.getX2() > uBound.x) {
						uBound.x = fh.getX2();
					}
					if (fh.getY1() < lBound.y) {
						lBound.y = fh.getY1();
					}
					if (fh.getY2() > uBound.y) {
						uBound.y = fh.getY2();
					}
					footholds.add(fh);
				}
			}
		}

		FootholdTree fTree = new FootholdTree(lBound, uBound);
		for (Foothold fh : footholds) {
			fTree.insert(fh);
		}
		map.setFootholds(fTree);

		// load areas (EG PQ platforms)
		if (mapData.getChildByPath("area") != null) {
			for (WzData area : mapData.getChildByPath("area")) {
				final int x1 = WzDataTool.getInt(area.getChildByPath("x1"));
				final int y1 = WzDataTool.getInt(area.getChildByPath("y1"));
				final int x2 = WzDataTool.getInt(area.getChildByPath("x2"));
				final int y2 = WzDataTool.getInt(area.getChildByPath("y2"));
				final Rectangle mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
				map.addMapleArea(mapArea);
			}
		}

		String message = null;
		final WzData timeMob = mapData.getChildByPath("info/timeMob");
		if (timeMob != null) {
			message = WzDataTool.getString("message", timeMob, null);
		}

		// load life data (npc, monsters)
		for (WzData life : mapData.getChildByPath("life")) {
			final String type = WzDataTool.getString("type", life);
			if (loadNpcs || !type.equals("n")) {
				final String id = WzDataTool.getString("id", life);
				final AbstractLoadedLife myLife = loadLife(life, id, type);
				if (myLife instanceof Monster) {
					final Monster mob = (Monster) myLife;
					final int mobTime = WzDataTool.getInt("mobTime", life, 0);
					final byte team = (byte) WzDataTool.getInt("team", life, -1);
					map.addMonsterSpawn(mob, mobTime, team, message);
				} else {
					boolean bAdd = true;
					if (bAdd) {
						map.addMapObject(myLife);
					}
				}
			}
		}
		addAreaBossSpawn(map);
		map.loadMonsterRate(true);

		// load reactor data
		if (loadReactors && mapData.getChildByPath("reactor") != null) {
			for (WzData reactor : mapData.getChildByPath("reactor")) {
				final String id = WzDataTool.getString("id", reactor);
				if (id == null)
					continue;

				final byte facingDirection = (byte) WzDataTool.getInt("f", reactor, 0);
				map.spawnReactor(loadReactor(reactor, id, facingDirection));
			}
		}

		map.setClock(mapData.getChildByPath("clock") != null);
		map.setEverlast(mapData.getChildByPath("info/everlast") != null);
		map.setTown(mapData.getChildByPath("info/town") != null);

		map.setForcedReturnMap(WzDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));

		map.setRecoveryRate(WzDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
		map.setHPDec(WzDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
		map.setHPDecProtect(WzDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
		map.setFieldLimit(WzDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
		map.setTimeLimit(WzDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));

		map.setCreateMobInterval((short) WzDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));

		map.setFirstUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
		map.setUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));

		instanceMap.put(instanceId, map);
		return map;
	}

	public int getLoadedMaps() {
		return maps.size();
	}

	public boolean isMapLoaded(int mapId) {
		return maps.containsKey(mapId);
	}

	public boolean isInstanceMapLoaded(int instanceid) {
		return instanceMap.containsKey(instanceid);
	}

	public void clearLoadedMap() {
		maps.clear();
	}

	public Map<Integer, GameMap> getMaps() {
		return maps;
	}

	private AbstractLoadedLife loadLife(WzData life, String id, String type) {
		AbstractLoadedLife myLife = LifeFactory.getLife(Integer.parseInt(id), type);
		myLife.setCy(WzDataTool.getInt("cy", life));
		myLife.setFacingDirection(WzDataTool.getInt("f", life, 0));
		myLife.setFoothold(WzDataTool.getInt("fh", life));

		myLife.setRx0(WzDataTool.getInt("rx0", life));
		myLife.setRx1(WzDataTool.getInt("rx1", life));

		final int x = WzDataTool.getInt("x", life);
		final int y = WzDataTool.getInt("y", life);
		final Point position = new Point(x, y);
		myLife.setPosition(position);

		if (WzDataTool.getInt("hide", life, 0) == 1) {
			myLife.setHidden(true);
		}
		return myLife;
	}

	private Reactor loadReactor(final WzData reactor, final String id, final byte facingDirection) {
		final ReactorInfo stats = ReactorFactory.getReactor(Integer.parseInt(id));
		final Reactor myReactor = new Reactor(stats, Integer.parseInt(id));

		stats.setFacingDirection(facingDirection);

		final int x = WzDataTool.getInt("x", reactor);
		final int y = WzDataTool.getInt("y", reactor);
		final Point position = new Point(x, y);
		myReactor.setPosition(position);

		final int delay = WzDataTool.getInt("reactorTime", reactor);
		myReactor.setDelay(delay * 1000);
		myReactor.setState((byte) 0);
		final String name = WzDataTool.getString("name", reactor, "");
		myReactor.setName(name);

		return myReactor;
	}

	private String getMapName(int mapId) {
		String paddedId = StringUtil.getLeftPaddedStr(Integer.toString(mapId), '0', 9);
		StringBuilder builder = new StringBuilder("Map/Map");
		builder.append(mapId / 100_000_000);
		builder.append("/");
		builder.append(paddedId);
		builder.append(".img");

		paddedId = builder.toString();
		return paddedId;
	}

	private void addAreaBossSpawn(final GameMap map) {
		int monsterid = -1;
		int mobtime = -1;
		String msg = null;
		Point pos1 = null, pos2 = null, pos3 = null;

		switch (map.getId()) {
		case 104_000_400: // Mano
			mobtime = 2700;
			monsterid = 2220000;
			msg = "A cool breeze was felt when Mano appeared.";
			pos1 = new Point(439, 185);
			pos2 = new Point(301, -85);
			pos3 = new Point(107, -355);
			break;
		case 101_030_404: // Stumpy
			mobtime = 2700;
			monsterid = 3220000;
			msg = "Stumpy has appeared with a stumping sound that rings the Stone Mountain.";
			pos1 = new Point(867, 1282);
			pos2 = new Point(810, 1570);
			pos3 = new Point(838, 2197);
			break;
		case 110_040_000: // King Clang
			mobtime = 1200;
			monsterid = 5220001;
			msg = "A strange turban shell has appeared on the beach.";
			pos1 = new Point(-355, 179);
			pos2 = new Point(-1283, -113);
			pos3 = new Point(-571, -593);
			break;
		case 250_010_304: // Tae Roon
			mobtime = 2100;
			monsterid = 7220000;
			msg = "Tae Roon appeared with a loud grow.";
			pos1 = new Point(-210, 33);
			pos2 = new Point(-234, 393);
			pos3 = new Point(-654, 33);
			break;
		case 200_010_300: // Eliza
			mobtime = 1200;
			monsterid = 8220000;
			msg = "Eliza has appeared with a black whirlwind.";
			pos1 = new Point(665, 83);
			pos2 = new Point(672, -217);
			pos3 = new Point(-123, -217);
			break;
		case 250_010_503: // Ghost Priest
			mobtime = 1800;
			monsterid = 7220002;
			msg = "The area fills with an unpleasant force of evil.. even the occasional ones of the cats sound disturbing";
			pos1 = new Point(-303, 543);
			pos2 = new Point(227, 543);
			pos3 = new Point(719, 543);
			break;
		case 222_010_310: // Old Fox
			mobtime = 2700;
			monsterid = 7220001;
			msg = "As the moon light dims,a long fox cry can be heard and the presence of the old fox can be felt.";
			pos1 = new Point(-169, -147);
			pos2 = new Point(-517, 93);
			pos3 = new Point(247, 93);
			break;
		case 107_000_300: // Dale
			mobtime = 1800;
			monsterid = 6220000;
			msg = "The huge crocodile Dale has come out from the swamp.";
			pos1 = new Point(710, 118);
			pos2 = new Point(95, 119);
			pos3 = new Point(-535, 120);
			break;
		case 100_040_105: // Faust
			mobtime = 1800;
			monsterid = 5220002;
			msg = "The blue fog became darker when Faust appeared.";
			pos1 = new Point(1000, 278);
			pos2 = new Point(557, 278);
			pos3 = new Point(95, 278);
			break;
		case 100_040_106: // Faust
			mobtime = 1800;
			monsterid = 5220002;
			msg = "The blue fog became darker when Faust appeared.";
			pos1 = new Point(1000, 278);
			pos2 = new Point(557, 278);
			pos3 = new Point(95, 278);
			break;
		case 220_050_100: // Timer
			mobtime = 1500;
			monsterid = 5220003;
			msg = "Click clock! Timer has appeared with an irregular clock sound.";
			pos1 = new Point(-467, 1032);
			pos2 = new Point(532, 1032);
			pos3 = new Point(-47, 1032);
			break;
		case 221_040_301: // Jeno
			mobtime = 2400;
			monsterid = 6220001;
			msg = "Jeno has appeared with a heavy sound of machinery.";
			pos1 = new Point(-4134, 416);
			pos2 = new Point(-4283, 776);
			pos3 = new Point(-3292, 776);
			break;
		case 240_040_401: // Lev
			mobtime = 7200;
			monsterid = 8220003;
			msg = "Leviathan has appeared with a cold wind from over the gorge.";
			pos1 = new Point(-15, 2481);
			pos2 = new Point(127, 1634);
			pos3 = new Point(159, 1142);
			break;
		case 260_010_201: // Dewu
			mobtime = 3600;
			monsterid = 3220001;
			msg = "Dewu slowly appeared out of the sand dust.";
			pos1 = new Point(-215, 275);
			pos2 = new Point(298, 275);
			pos3 = new Point(592, 275);
			break;
		case 261_030_000: // Chimera
			mobtime = 2700;
			monsterid = 8220002;
			msg = "Chimera has appeared out of the darkness of the underground with a glitter in her eyes.";
			pos1 = new Point(-1094, -405);
			pos2 = new Point(-772, -116);
			pos3 = new Point(-108, 181);
			break;
		case 230_020_100: // Sherp
			mobtime = 2700;
			monsterid = 4220000;
			msg = "A strange shell has appeared from a grove of seaweed.";
			pos1 = new Point(-291, -20);
			pos2 = new Point(-272, -500);
			pos3 = new Point(-462, 640);
			break;
		/*
		 * case 910000000: // FM mobtime = 300; monsterid = 9420015; msg =
		 * "NooNoo has appeared out of anger, it seems that NooNoo is stuffed with Christmas gifts!"
		 * ; pos1 = new Point(498, 4); pos2 = new Point(498, 4); pos3 = new
		 * Point(498, 4); break;
		 */
		case 209_000_000: // Happyvile
			mobtime = 300;
			monsterid = 9500317;
			msg = "Little Snowman has appeared!";
			pos1 = new Point(-115, 154);
			pos2 = new Point(-115, 154);
			pos3 = new Point(-115, 154);
			break;
		default:
			return;
		}
		map.addAreaMonsterSpawn(LifeFactory.getMonster(monsterid), pos1, pos2, pos3, mobtime, msg);
	}
}