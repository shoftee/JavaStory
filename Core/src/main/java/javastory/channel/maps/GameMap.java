package javastory.channel.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.client.BuffStat;
import javastory.channel.client.MonsterStatus;
import javastory.channel.client.MonsterStatusEffect;
import javastory.channel.client.Pet;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.life.Npc;
import javastory.channel.life.SpawnPointAreaBoss;
import javastory.channel.life.Spawns;
import javastory.channel.packet.MobPacket;
import javastory.channel.packet.PetPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Portal;
import javastory.channel.server.StatEffect;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.IItem;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.ItemInfoProvider;
import javastory.game.Jobs;
import javastory.game.SpawnPoint;
import javastory.game.data.MobDropInfo;
import javastory.game.data.MobDropInfoProvider;
import javastory.game.data.MobGlobalDropInfo;
import javastory.io.GamePacket;
import javastory.scripting.EventInstanceManager;
import javastory.tools.LogUtil;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.PartyOperation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class GameMap {

	private final Map<Integer, GameMapObject> mapObjects;
	private final Collection<Spawns> monsterSpawn;
	private final List<ChannelCharacter> characters;
	private final Map<Integer, Portal> portals;
	private final List<Rectangle> areas;

	private FootholdTree footholds = null;
	private float monsterRate, recoveryRate;
	private GameMapEffect mapEffect;
	private short decHP = 0, createMobInterval = 9000;
	private int protectItem = 0, mapId, returnMapId, timeLimit, fieldLimit, maxRegularSpawn = 0;
	private int runningOid = 100000, forcedReturnMap = 999999999;
	private boolean town, clock, personalShop, isEverlast = false, dropsDisabled = false;
	private String onUserEnter, onFirstUserEnter;

	private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
	private final Lock mutex = new ReentrantLock();

	public GameMap(final int mapId, final int returnMapId, final float monsterRate) {
		this.mapId = mapId;
		this.returnMapId = returnMapId;
		this.monsterRate = monsterRate;

		this.mapObjects = Maps.newHashMap();
		this.monsterSpawn = Lists.newLinkedList();
		this.characters = Lists.newArrayList();
		this.portals = Maps.newHashMap();
		this.areas = Lists.newArrayList();
	}

	public final void toggleDrops() {
		this.dropsDisabled = !dropsDisabled;
	}

	public final int getId() {
		return mapId;
	}

	public final GameMap getReturnMap() {
		return ChannelServer.getMapFactory().getMap(returnMapId);
	}

	public final int getReturnMapId() {
		return returnMapId;
	}

	public final int getForcedReturnId() {
		return forcedReturnMap;
	}

	public final GameMap getForcedReturnMap() {
		return ChannelServer.getMapFactory().getMap(forcedReturnMap);
	}

	public final void setForcedReturnMap(final int map) {
		this.forcedReturnMap = map;
	}

	public final float getRecoveryRate() {
		return recoveryRate;
	}

	public final void setRecoveryRate(final float recoveryRate) {
		this.recoveryRate = recoveryRate;
	}

	public final int getFieldLimit() {
		return fieldLimit;
	}

	public final void setFieldLimit(final int fieldLimit) {
		this.fieldLimit = fieldLimit;
	}

	public final void setCreateMobInterval(final short createMobInterval) {
		this.createMobInterval = createMobInterval;
	}

	public final void setTimeLimit(final int timeLimit) {
		this.timeLimit = timeLimit;
	}

	public final void setFirstUserEnter(final String onFirstUserEnter) {
		this.onFirstUserEnter = onFirstUserEnter;
	}

	public final void setUserEnter(final String onUserEnter) {
		this.onUserEnter = onUserEnter;
	}

	public final boolean hasClock() {
		return clock;
	}

	public final void setClock(final boolean hasClock) {
		this.clock = hasClock;
	}

	public final boolean isTown() {
		return town;
	}

	public final void setTown(final boolean town) {
		this.town = town;
	}

	public final boolean allowPersonalShop() {
		return personalShop;
	}

	public final void setPersonalShop(final boolean personalShop) {
		this.personalShop = personalShop;
	}

	public final void setEverlast(final boolean everlast) {
		this.isEverlast = everlast;
	}

	public final boolean isEverlast() {
		return isEverlast;
	}

	public final int getHPDec() {
		return decHP;
	}

	public final void setHPDec(final int delta) {
		decHP = (short) delta;
	}

	public final int getHPDecProtect() {
		return protectItem;
	}

	public final void setHPDecProtect(final int delta) {
		this.protectItem = delta;
	}

	public final int getCurrentPartyId() {
		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				final PartyMember member = chr.getPartyMembership();
				if (member != null) {
					return member.getPartyId();
				}
			}
		} finally {
			mutex.unlock();
		}
		return -1;
	}

	public final void addMapObject(final GameMapObject mapobject) {
		mutex.lock();

		try {
			runningOid++;
			mapobject.setObjectId(runningOid);
			mapObjects.put(runningOid, mapobject);
		} finally {
			mutex.unlock();
		}
	}

	private void spawnAndAddRangedMapObject(final GameMapObject mapobject, final DelayedPacketCreation packetbakery, final SpawnCondition condition) {
		mutex.lock();

		try {
			runningOid++;
			mapobject.setObjectId(runningOid);
			mapObjects.put(runningOid, mapobject);

			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				if (condition == null || condition.canSpawn(chr)) {
					if (chr.getPosition().distanceSq(mapobject.getPosition()) <= GameConstants.maxViewRangeSq()) {
						packetbakery.sendPackets(chr.getClient());
						chr.addVisibleMapObject(mapobject);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
	}

	public final void removeMapObject(final int num) {
		mutex.lock();
		try {
			mapObjects.remove(Integer.valueOf(num));
		} finally {
			mutex.unlock();
		}
	}

	public final void removeMapObject(final GameMapObject obj) {
		mutex.lock();
		try {
			mapObjects.remove(Integer.valueOf(obj.getObjectId()));
		} finally {
			mutex.unlock();
		}
	}

	public final Point calcPointBelow(final Point initial) {
		final Foothold fh = footholds.findBelow(initial);
		if (fh == null) {
			return null;
		}
		int dropY = fh.getY1();
		if (!fh.isWall() && fh.getY1() != fh.getY2()) {
			final double s1 = Math.abs(fh.getY2() - fh.getY1());
			final double s2 = Math.abs(fh.getX2() - fh.getX1());
			if (fh.getY2() < fh.getY1()) {
				dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
			} else {
				dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
			}
		}
		return new Point(initial.x, dropY);
	}

	private Point calcDropPos(final Point initial, final Point fallback) {
		final Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
		if (ret == null) {
			return fallback;
		}
		return ret;
	}

	private void dropFromMonster(final ChannelCharacter chr, final Monster mob) {
		if (dropsDisabled || mob.dropsDisabled()) {
			return;
		}

		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		final byte droptype;
		if (mob.getStats().isExplosiveReward()) {
			droptype = 3;
		} else if (mob.getStats().isFreeForAllLoot()) {
			droptype = 2;
		} else if (chr.hasParty()) {
			droptype = 1;
		} else {
			droptype = 0;
		}

		final int mobpos = mob.getPosition().x;
		final float globalItemRate = ChannelServer.getInstance().getItemRate();
		IItem idrop;
		byte d = 1;
		Point pos = new Point(0, mob.getPosition().y);
		final MobDropInfoProvider mi = MobDropInfoProvider.getInstance();
		final List<MobDropInfo> dropEntry = new ArrayList<>(mi.retrieveDrop(mob.getId()));
		Collections.shuffle(dropEntry);
		for (final MobDropInfo de : dropEntry) {
			if (Randomizer.nextInt(999999) < de.Chance * globalItemRate) {
				if (droptype == 3) {
					pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
				} else {
					pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
				}

				if (de.ItemId == 0) {
					// meso
					int mesos = Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum;
					if (mesos > 0) {
						if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
							mesos = (int) (mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0);
						}
						final float mesoRate = ChannelServer.getInstance().getMesoRate();
						spawnMobMesoDrop((int) (mesos * mesoRate), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
					}
				} else {
					if (GameConstants.getInventoryType(de.ItemId) == InventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii.getEquipById(de.ItemId));
					} else {
						idrop = new Item(de.ItemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1),
							(byte) 0);
					}
					spawnMobDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
				}
				d++;
			}
		}
		final List<MobGlobalDropInfo> globalEntry = mi.getGlobalDrop();
		// Global Drops
		for (final MobGlobalDropInfo de : globalEntry) {
			if (Randomizer.nextInt(999999) < de.Chance) {
				if (droptype == 3) {
					pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
				} else {
					pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
				}
				if (de.ItemId == 0) { // Random Cash xD
					int cashGain;
					cashGain = (int) (Math.random() * 100);
					if (cashGain < 20) {
						cashGain = 20;
						chr.modifyCSPoints(1, cashGain, true);
					} else {
						chr.modifyCSPoints(1, cashGain, true);
					}
				} else {
					if (GameConstants.getInventoryType(de.ItemId) == InventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii.getEquipById(de.ItemId));
					} else {
						idrop = new Item(de.ItemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1),
							(byte) 0);
					}
					spawnMobDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
					d++;
				}
			}
		}
	}

	private void killMonster(final Monster monster) { // For mobs with
														// removeAfter
		spawnedMonstersOnMap.decrementAndGet();
		monster.setHp(0);
		monster.spawnRevives(this);
		broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 1));
		removeMapObject(monster);
	}

	public final void killMonster(final Monster monster, final ChannelCharacter chr, final boolean withDrops, final boolean second, final byte animation) {
		if (monster.getId() == 8810018 && !second) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					killMonster(monster, chr, true, true, (byte) 1);
					killAllMonsters(true);
				}
			}, 3000);
			return;
		}
		spawnedMonstersOnMap.decrementAndGet();
		removeMapObject(monster);
		ChannelCharacter dropOwner = monster.killBy(chr);
		broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
		if (monster.getBuffToGive() > -1) {
			final int buffid = monster.getBuffToGive();
			final StatEffect buff = ItemInfoProvider.getInstance().getItemEffect(buffid);
			for (final GameMapObject mmo : getAllPlayer()) {
				final ChannelCharacter c = (ChannelCharacter) mmo;
				if (c.isAlive()) {
					buff.applyTo(c);
					switch (monster.getId()) {
					case 8810018:
					case 8820001:
						// HT nine spirit
						c.getClient().write(ChannelPackets.showOwnBuffEffect(buffid, 11));
						broadcastMessage(c, ChannelPackets.showBuffeffect(c.getId(), buffid, 11), false);
						break;
					}
				}
			}
		}
		final int mobid = monster.getId();
		if (mobid == 8810018) {
			try {
				ChannelServer.getWorldInterface().broadcastMessage(
					ChannelPackets.serverNotice(6,
						"To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!"));
			} catch (RemoteException e) {
				ChannelServer.pingWorld();
			}
			LogUtil.log(LogUtil.Horntail_Log, MapDebug_Log());
		} else if (mobid == 8820001) {
			try {
				ChannelServer.getWorldInterface().broadcastMessage(
					ChannelPackets.serverNotice(6, "Expedition who defeated Pink Bean with invicible passion! You are the true timeless hero!"));
			} catch (RemoteException e) {
				ChannelServer.pingWorld();
			}
			LogUtil.log(LogUtil.Pinkbean_Log, MapDebug_Log());
		} else if (mobid >= 8800003 && mobid <= 8800010) {
			boolean makeZakReal = true;
			final Collection<GameMapObject> objects = getAllMonster();
			for (final GameMapObject object : objects) {
				final Monster mons = ((Monster) object);
				if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
					makeZakReal = false;
					break;
				}
			}
			if (makeZakReal) {
				for (final GameMapObject object : objects) {
					final Monster mons = ((Monster) object);
					if (mons.getId() == 8800000) {
						final Point pos = mons.getPosition();
						this.killAllMonsters(true);
						spawnMonsterOnGroundBelow(LifeFactory.getMonster(8800000), pos);
						break;
					}
				}
			}
		}
		if (withDrops) {
			if (dropOwner == null) {
				dropOwner = chr;
			}
			dropFromMonster(dropOwner, monster);
		}
	}

	public final void killAllMonsters(final boolean animate) {
		for (final GameMapObject monstermo : getAllMonster()) {
			final Monster monster = (Monster) monstermo;
			spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
			removeMapObject(monster);
		}
	}

	public final void killMonster(final int monsId) {
		for (final GameMapObject mmo : getAllMonster()) {
			if (((Monster) mmo).getId() == monsId) {
				spawnedMonstersOnMap.decrementAndGet();
				removeMapObject(mmo);
				broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
				break;
			}
		}
	}

	private String MapDebug_Log() {
		final StringBuilder sb = new StringBuilder("Defeat time : ");
		sb.append(LogUtil.CurrentReadable_Time());

		sb.append(" | Mapid : ").append(this.mapId);

		final List<GameMapObject> players = getAllPlayer();
		sb.append(" Users [").append(players.size()).append("] | ");
		final Iterator<GameMapObject> itr = players.iterator();
		while (itr.hasNext()) {
			sb.append(((ChannelCharacter) itr.next()).getName()).append(", ");
		}
		return sb.toString();
	}

	public final void destroyReactor(final int oid) {
		final Reactor reactor = getReactorByOid(oid);
		broadcastMessage(ChannelPackets.destroyReactor(reactor));
		reactor.setAlive(false);
		removeMapObject(reactor);
		reactor.setTimerActive(false);

		if (reactor.getDelay() > 0) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public final void run() {
					respawnReactor(reactor);
				}
			}, reactor.getDelay());
		}
	}

	/*
	 * command to reset all item-reactors in a map to state 0 for GM/NPC use -
	 * not tested (broken reactors get removed from mapobjects when destroyed)
	 * Should create instances for multiple copies of non-respawning reactors...
	 */
	public final void resetReactors() {
		for (final GameMapObject o : getAllReactor()) {
			((Reactor) o).setState((byte) 0);
			((Reactor) o).setTimerActive(false);
			broadcastMessage(ChannelPackets.triggerReactor((Reactor) o, 0));
		}
	}

	public final void setReactorState() {
		for (final GameMapObject o : getAllReactor()) {
			((Reactor) o).setState((byte) 1);
			((Reactor) o).setTimerActive(false);
			broadcastMessage(ChannelPackets.triggerReactor((Reactor) o, 1));
		}
	}

	/*
	 * command to shuffle the positions of all reactors in a map for PQ purposes
	 * (such as ZPQ/LMPQ)
	 */
	public final void shuffleReactors() {
		List<Point> points = new ArrayList<>();

		for (final GameMapObject o : getAllReactor()) {
			points.add(((Reactor) o).getPosition());
		}
		Collections.shuffle(points);
		for (final GameMapObject o : getAllReactor()) {
			((Reactor) o).setPosition(points.remove(points.size() - 1));
		}
	}

	/**
	 * Automagically finds a new controller for the given monster from the chars
	 * on the map...
	 * 
	 * @param monster
	 */
	public final void updateMonsterController(final Monster monster) {
		if (!monster.isAlive()) {
			return;
		}
		if (monster.getController() != null) {
			if (monster.getController().getMap() != this) {
				monster.getController().stopControllingMonster(monster);
			} else { // Everything is fine :)
				return;
			}
		}
		int mincontrolled = -1;
		ChannelCharacter newController = null;

		mutex.lock();

		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
					mincontrolled = chr.getControlledMonsters().size();
					newController = chr;
				}
			}
		} finally {
			mutex.unlock();
		}
		if (newController != null) {
			if (monster.isFirstAttack()) {
				newController.controlMonster(monster, true);
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(true);
			} else {
				newController.controlMonster(monster, false);
			}
		}
	}

	/*
	 * public Collection<MapleMapObject> getMapObjects() { return
	 * Collections.unmodifiableCollection(mapobjects.values()); }
	 */
	public final GameMapObject getMapObject(final int oid) {
		return mapObjects.get(oid);
	}

	public final int containsNPC(final int npcid) {
		for (GameMapObject obj : getAllNPC()) {
			if (((Npc) obj).getId() == npcid) {
				return obj.getObjectId();
			}
		}
		return -1;
	}

	/**
	 * returns a monster with the given oid, if no such monster exists returns
	 * null
	 * 
	 * @param oid
	 * @return
	 */
	public final Monster getMonsterByOid(final int oid) {
		final GameMapObject mmo = getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.MONSTER) {
			return (Monster) mmo;
		}
		return null;
	}

	public final Npc getNPCByOid(final int oid) {
		final GameMapObject mmo = getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.NPC) {
			return (Npc) mmo;
		}
		return null;
	}

	public final Reactor getReactorByOid(final int oid) {
		final GameMapObject mmo = getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.REACTOR) {
			return (Reactor) mmo;
		}
		return null;
	}

	public final Reactor getReactorByName(final String name) {
		for (final GameMapObject obj : getAllReactor()) {
			if (((Reactor) obj).getName().equals(name)) {
				return (Reactor) obj;
			}
		}
		return null;
	}

	public final void spawnNpc(final int id, final Point pos) {
		final Npc npc = LifeFactory.getNpc(id);
		npc.setPosition(pos);
		npc.setCy(pos.y);
		npc.setRx0(pos.x + 50);
		npc.setRx1(pos.x - 50);
		npc.setFoothold(getFootholds().findBelow(pos).getId());
		npc.setCustom(true);
		addMapObject(npc);
		broadcastMessage(ChannelPackets.spawnNpc(npc, true));
	}

	public final void removeNpc(final int id) {
		final List<GameMapObject> npcs = getAllNPC();
		for (final GameMapObject npcmo : npcs) {
			final Npc npc = (Npc) npcmo;
			if (npc.isCustom() && npc.getId() == id) {
				broadcastMessage(ChannelPackets.removeNpc(npc.getObjectId()));
				removeMapObject(npc.getObjectId());
			}
		}
	}

	public final void spawnMonster_sSack(final Monster mob, final Point pos, final int spawnType) {
		final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
		mob.setPosition(spos);
		spawnMonster(mob, spawnType);
	}

	public final void spawnMonsterOnGroundBelow(final Monster mob, final Point pos) {
		final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
		mob.setPosition(spos);
		spawnMonster(mob, -2);
	}

	public final void spawnZakum(final Point pos) {
		final Monster mainb = LifeFactory.getMonster(8800000);
		final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
		mainb.setPosition(spos);
		mainb.setFake(true);

		// Might be possible to use the map object for reference in future.
		spawnFakeMonster(mainb);

		final int[] zakpart = { 8800003, 8800004, 8800005, 8800006, 8800007, 8800008, 8800009, 8800010 };

		for (final int i : zakpart) {
			final Monster part = LifeFactory.getMonster(i);
			part.setPosition(spos);

			spawnMonster(part, -2);
		}
	}

	public final void spawnFakeMonsterOnGroundBelow(final Monster mob, final Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = calcPointBelow(spos);
		spos.y -= 1;
		mob.setPosition(spos);
		spawnFakeMonster(mob);
	}

	private void checkRemoveAfter(final Monster monster) {
		final int ra = monster.getStats().getRemoveAfter();

		if (ra > 0) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public final void run() {
					if (monster != null) {
						killMonster(monster);
					}
				}
			}, ra * 1000);
		}
	}

	public final void spawnRevives(final Monster monster, final int oid) {
		monster.setMap(this);
		checkRemoveAfter(monster);

		spawnAndAddRangedMapObject(monster, new MonsterSpawnTask(oid, monster), null);
		updateMonsterController(monster);

		spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnMonster(final Monster monster, final int spawnType) {
		monster.setMap(this);
		checkRemoveAfter(monster);

		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(ChannelClient c) {
				c.write(MobPacket.spawnMonster(monster, spawnType, 0, 0));
			}
		}, null);
		updateMonsterController(monster);

		spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnMonsterWithEffect(final Monster monster, final int effect, Point pos) {
		try {
			monster.setMap(this);
			monster.setPosition(pos);

			spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

				@Override
				public final void sendPackets(ChannelClient c) {
					c.write(MobPacket.spawnMonster(monster, -2, effect, 0));
				}
			}, null);
			updateMonsterController(monster);

			spawnedMonstersOnMap.incrementAndGet();
		} catch (Exception e) {
		}
	}

	public final void spawnFakeMonster(final Monster monster) {
		monster.setMap(this);
		monster.setFake(true);

		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(ChannelClient c) {
				c.write(MobPacket.spawnMonster(monster, -2, 0xfc, 0));
				// c.write(MobPacket.spawnFakeMonster(monster, 0));
			}
		}, null);
		updateMonsterController(monster);

		spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnReactor(final Reactor reactor) {
		reactor.setMap(this);

		spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.spawnReactor(reactor));
			}
		}, null);
	}

	private void respawnReactor(final Reactor reactor) {
		reactor.setState((byte) 0);
		reactor.setAlive(true);
		spawnReactor(reactor);
	}

	public final void spawnDoor(final Door door) {
		spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(ChannelClient c) {
				final ChannelCharacter doorOwner = door.getOwner();
				c.write(ChannelPackets.spawnDoor(doorOwner.getId(), door.getTargetPosition(), false));

				final ChannelCharacter clientPlayer = c.getPlayer();
				final boolean isOwner = doorOwner == clientPlayer;

				final PartyMember ownerMember = doorOwner.getPartyMembership();
				final PartyMember clientMember = clientPlayer.getPartyMembership();
				final boolean isSameParty = ownerMember != null && clientMember != null && ownerMember.getPartyId() == clientMember.getPartyId();

				if (isOwner || isSameParty) {
					c.write(ChannelPackets.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
				}
				c.write(ChannelPackets.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
				c.write(ChannelPackets.enableActions());
			}
		}, new SpawnCondition() {

			@Override
			public final boolean canSpawn(final ChannelCharacter chr) {
				return chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.hasParty();
			}
		});
	}

	public final void spawnDragon(final Dragon summon) {
		spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.spawnDragon(summon));
			}
		}, null);
	}

	public final void spawnSummon(final Summon summon) {
		spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.spawnSummon(summon, summon.getSkillLevel(), true));
			}
		}, null);
	}

	public final void spawnMist(final Mist mist, final int duration, boolean poison, boolean fake) {
		spawnAndAddRangedMapObject(mist, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.spawnMist(mist));
			}
		}, null);

		final MapTimer tMan = MapTimer.getInstance();
		final ScheduledFuture<?> poisonSchedule;

		if (poison) {
			poisonSchedule = tMan.register(new Runnable() {

				@Override
				public void run() {
					for (final GameMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(GameMapObjectType.MONSTER))) {
						if (mist.makeChanceResult()) {
							((Monster) mo).applyStatus(mist.getOwner(), new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist
								.getSourceSkill(), null, false), true, duration, false);
						}
					}
				}
			}, 2000, 2500);
		} else {
			poisonSchedule = null;
		}
		tMan.schedule(new Runnable() {

			@Override
			public void run() {
				broadcastMessage(ChannelPackets.removeMist(mist.getObjectId()));
				removeMapObject(mist);
				if (poisonSchedule != null) {
					poisonSchedule.cancel(false);
				}
			}
		}, duration);
	}

	public final void disappearingItemDrop(final GameMapObject dropper, final ChannelCharacter owner, final IItem item, final Point pos) {
		final Point droppos = calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) 1, false);
		broadcastMessage(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
	}

	public final void spawnMesoDrop(final int meso, final Point position, final GameMapObject dropper, final ChannelCharacter owner, final boolean playerDrop,
		final byte droptype) {
		final Point droppos = calcDropPos(position, position);
		final GameMapItem mdrop = new GameMapItem(meso, droppos, dropper, owner, droptype, playerDrop);
		spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		if (!isEverlast) {
			MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
		}
	}

	public final void spawnMobMesoDrop(final int meso, final Point position, final GameMapObject dropper, final ChannelCharacter owner,
		final boolean playerDrop, final byte droptype) {
		final GameMapItem mdrop = new GameMapItem(meso, position, dropper, owner, droptype, playerDrop);
		spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(mdrop, dropper.getPosition(), position, (byte) 1));
			}
		}, null);
		MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
	}

	private void spawnMobDrop(final IItem idrop, final Point dropPos, final Monster mob, final ChannelCharacter chr, final byte droptype, final short questid) {
		final GameMapItem mdrop = new GameMapItem(idrop, dropPos, mob, chr, droptype, false, questid);
		spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				if (questid <= 0 || c.getPlayer().getQuestCompletionStatus(questid) == 1) {
					c.write(ChannelPackets.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1));
				}
			}
		}, null);
		MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
		activateItemReactors(mdrop, chr.getClient());
	}

	public final void spawnItemDrop(final GameMapObject dropper, final ChannelCharacter owner, final IItem item, Point pos, final boolean ffaDrop,
		final boolean playerDrop) {
		final Point droppos = calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) 0, playerDrop);
		spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		broadcastMessage(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));
		if (!isEverlast) {
			MapTimer.getInstance().schedule(new ExpireMapItemJob(drop), 180000);
			activateItemReactors(drop, owner.getClient());
		}
	}

	private void activateItemReactors(final GameMapItem drop, final ChannelClient c) {
		final IItem item = drop.getItem();

		for (final GameMapObject o : getAllReactor()) {
			final Reactor react = (Reactor) o;

			if (react.getReactorType() == 100) {
				if (react.getReactItem().getLeft() == item.getItemId() && react.getReactItem().getRight() == item.getQuantity()) {

					if (react.getArea().contains(drop.getPosition())) {
						if (!react.isTimerActive()) {
							MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
							react.setTimerActive(true);
							break;
						}
					}
				}
			}
		}
	}

	public final void AriantPQStart() {
		int i = 1;

		mutex.lock();

		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter chars;
			while (ltr.hasNext()) {
				chars = ltr.next();
				broadcastMessage(ChannelPackets.updateAriantPQRanking(chars.getName(), 0, false));
				broadcastMessage(ChannelPackets.serverNotice(0, ChannelPackets.updateAriantPQRanking(chars.getName(), 0, false).toString()));
				if (this.getCharactersSize() > i) {
					broadcastMessage(ChannelPackets.updateAriantPQRanking(null, 0, true));
					broadcastMessage(ChannelPackets.serverNotice(0, ChannelPackets.updateAriantPQRanking(chars.getName(), 0, true).toString()));
				}
				i++;
			}
		} finally {
			mutex.unlock();
		}
	}

	public final void returnEverLastItem(final ChannelCharacter chr) {
		for (final GameMapObject o : getAllItems()) {
			final GameMapItem item = ((GameMapItem) o);
			if (item.getOwner() == chr.getId()) {
				item.setPickedUp(true);
				broadcastMessage(ChannelPackets.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getPosition());
				if (item.getMeso() > 0) {
					chr.gainMeso(item.getMeso(), false);
				} else {
					InventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
				}
				removeMapObject(item);
			}
		}
	}

	public final void startMapEffect(final String msg, final int itemId) {
		if (mapEffect != null) {
			return;
		}
		mapEffect = new GameMapEffect(msg, itemId);
		broadcastMessage(mapEffect.makeStartData());
		MapTimer.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				broadcastMessage(mapEffect.makeDestroyData());
				mapEffect = null;
			}
		}, 30000);
	}

	public final void addPlayer(final ChannelCharacter character) {
		mutex.lock();
		try {
			characters.add(character);
			mapObjects.put(character.getObjectId(), character);
		} finally {
			mutex.unlock();
		}
		if (!character.isHidden()) {
			broadcastMessage(ChannelPackets.spawnPlayerMapObject(character));
		}
		sendObjectPlacement(character);

		final ChannelClient client = character.getClient();

		client.write(ChannelPackets.spawnPlayerMapObject(character));

		if (!onFirstUserEnter.equals("")) {
			if (getCharactersSize() == 1) {
				MapScriptMethods.startScript_FirstUser(client, onFirstUserEnter);
			}
		}
		if (!onUserEnter.equals("")) {
			MapScriptMethods.startScript_User(client, onUserEnter);
		}
		for (final Pet pet : character.getPets()) {
			if (pet.isSummoned()) {
				final GamePacket packet = PetPacket.showPet(character, pet);
				broadcastMessage(character, packet, false);
			}
		}
		switch (mapId) {
		case 809000101:
		case 809000201:
			client.write(ChannelPackets.showEquipEffect());
			break;
		}
		if (getHPDec() > 0) {
			character.startHurtHp();
		}
		final PartyMember member = character.getPartyMembership();
		if (member != null) {
			try {
				Party party = ChannelServer.getWorldInterface().getParty(member.getPartyId());
				character.silentPartyUpdate();
				client.write(ChannelPackets.updateParty(client.getChannelId(), party, PartyOperation.SILENT_UPDATE, null));
				character.updatePartyMemberHP();
				character.receivePartyMemberHP();
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
		final StatEffect stat = character.getStatForBuff(BuffStat.SUMMON);
		if (stat != null) {
			final Summon summon = character.getSummons().get(stat.getSourceId());
			summon.setPosition(character.getPosition());
			character.addVisibleMapObject(summon);
			this.spawnSummon(summon);
		}
		if (mapEffect != null) {
			mapEffect.sendStartData(client);
		}
		if (timeLimit > 0 && getForcedReturnMap() != null) {
			character.startMapTimeLimitTask(timeLimit, getForcedReturnMap());
		}

		if (character.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			if (FieldLimitType.Mount.check(fieldLimit)) {
				character.cancelBuffStats(BuffStat.MONSTER_RIDING);
			}
		}

		final EventInstanceManager eventInstance = character.getEventInstance();
		if (eventInstance != null && eventInstance.isTimerStarted()) {
			client.write(ChannelPackets.getClock((int) (eventInstance.getTimeLeft() / 1000)));
		}

		if (hasClock()) {
			final Calendar calendar = Calendar.getInstance();
			client.write(ChannelPackets.getClockTime(calendar));
		}

		if (character.getCarnivalParty() != null && eventInstance != null) {
			eventInstance.onMapLoad(character);
		}

		final int jobId = character.getJobId();
		if (Jobs.isEvan(jobId) && jobId >= 2200 && character.getBuffedValue(BuffStat.MONSTER_RIDING) == null) {
			if (character.getDragon() == null) {
				character.makeDragon();
			}
			spawnDragon(character.getDragon());
			updateMapObjectVisibility(character, character.getDragon());
		}
	}

	public final void removePlayer(final ChannelCharacter character) {
		// log.warn("[dc] [level2] Player {} leaves map {}", new Object[] {
		// chr.getName(), mapid });

		if (isEverlast) {
			returnEverLastItem(character);
		}
		mutex.lock();
		try {
			characters.remove(character);
		} finally {
			mutex.unlock();
		}
		removeMapObject(Integer.valueOf(character.getObjectId()));
		broadcastMessage(ChannelPackets.removePlayerFromMap(character.getId()));

		for (final Monster monster : character.getControlledMonsters()) {
			monster.setController(null);
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
			updateMonsterController(monster);
		}
		character.leaveMap();
		character.cancelMapTimeLimitTask();

		for (final Summon summon : character.getSummons().values()) {
			if (summon.isPuppet()) {
				character.cancelBuffStats(BuffStat.PUPPET);
				character.cancelBuffStats(BuffStat.MIRROR_TARGET);
			} else {
				removeMapObject(summon);
			}
		}
		if (character.getDragon() != null) {
			removeMapObject(character.getDragon());
		}
	}

	public final void broadcastMessage(final GamePacket packet) {
		broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	public final void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final boolean repeatToSource) {
		broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
	}

	/*
	 * public void broadcastMessage(GameCharacter source, GamePacket packet,
	 * boolean repeatToSource, boolean ranged) { broadcastMessage(repeatToSource
	 * ? null : source, packet, ranged ? GameCharacter.MAX_VIEW_RANGE_SQ :
	 * Double.POSITIVE_INFINITY, source.getPosition()); }
	 */
	public final void broadcastMessage(final GamePacket packet, final Point rangedFrom) {
		broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
	}

	public final void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final Point rangedFrom) {
		broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
	}

	private void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final double rangeSq, final Point rangedFrom) {
		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				if (chr != source) {
					if (rangeSq < Double.POSITIVE_INFINITY) {
						if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
							chr.getClient().write(packet);
						}
					} else {
						chr.getClient().write(packet);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
	}

	private void sendObjectPlacement(final ChannelCharacter c) {
		if (c == null) {
			return;
		}
		for (final GameMapObject o : getAllMonster()) {
			updateMonsterController((Monster) o);
		}
		for (final GameMapObject o : getMapObjectsInRange(c.getPosition(), GameConstants.maxViewRangeSq(), GameConstants.rangedMapObjectTypes)) {
			if (o.getType() == GameMapObjectType.REACTOR) {
				if (!((Reactor) o).isAlive()) {
					continue;
				}
			}
			o.sendSpawnData(c.getClient());
			c.addVisibleMapObject(o);
		}
	}

	public final List<GameMapObject> getMapObjectsInRange(final Point from, final double rangeSq) {
		final List<GameMapObject> ret = new LinkedList<>();

		mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = mapObjects.values().iterator();
			GameMapObject obj;
			while (ltr.hasNext()) {
				obj = ltr.next();
				if (from.distanceSq(obj.getPosition()) <= rangeSq) {
					ret.add(obj);
				}
			}
		} finally {
			mutex.unlock();
		}
		return ret;
	}

	public final List<GameMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<GameMapObjectType> MapObject_types) {
		final List<GameMapObject> ret = new LinkedList<>();

		mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = mapObjects.values().iterator();
			GameMapObject obj;
			while (ltr.hasNext()) {
				obj = ltr.next();
				if (MapObject_types.contains(obj.getType())) {
					if (from.distanceSq(obj.getPosition()) <= rangeSq) {
						ret.add(obj);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
		return ret;
	}

	public final List<GameMapObject> getMapObjectsInRect(final Rectangle box, final List<GameMapObjectType> MapObject_types) {
		final List<GameMapObject> ret = new LinkedList<>();

		mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = mapObjects.values().iterator();
			GameMapObject obj;
			while (ltr.hasNext()) {
				obj = ltr.next();
				if (MapObject_types.contains(obj.getType())) {
					if (box.contains(obj.getPosition())) {
						ret.add(obj);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
		return ret;
	}

	public final List<ChannelCharacter> getPlayersInRect(final Rectangle box, final List<ChannelCharacter> CharacterList) {
		final List<ChannelCharacter> character = new LinkedList<>();

		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter a;
			while (ltr.hasNext()) {
				a = ltr.next();
				if (CharacterList.contains(a.getClient().getPlayer())) {
					if (box.contains(a.getPosition())) {
						character.add(a);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
		return character;
	}

	public final List<GameMapObject> getAllItems() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
	}

	public final List<GameMapObject> getAllNPC() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.NPC));
	}

	public final List<GameMapObject> getAllReactor() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
	}

	public final List<GameMapObject> getAllPlayer() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.PLAYER));
	}

	public final List<GameMapObject> getAllMonster() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER));
	}

	public final List<GameMapObject> getAllDoor() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.DOOR));
	}

	public final void addPortal(final Portal myPortal) {
		portals.put(myPortal.getId(), myPortal);
	}

	public final Portal getPortal(final String portalname) {
		for (final Portal port : portals.values()) {
			if (port.getName().equals(portalname)) {
				return port;
			}
		}
		return null;
	}

	public final Portal getPortal(final int portalid) {
		return portals.get(portalid);
	}

	public final void addMapleArea(final Rectangle rec) {
		areas.add(rec);
	}

	public final List<Rectangle> getAreas() {
		return new ArrayList<>(areas);
	}

	public final Rectangle getArea(final int index) {
		return areas.get(index);
	}

	public final void setFootholds(final FootholdTree footholds) {
		this.footholds = footholds;
	}

	public final FootholdTree getFootholds() {
		return footholds;
	}

	public final void loadMonsterRate(final boolean first) {
		final int spawnSize = monsterSpawn.size();
		/*
		 * if (spawnSize >= 25 || monsterRate > 1.5) { maxRegularSpawn =
		 * Math.round(spawnSize / monsterRate); } else { maxRegularSpawn =
		 * Math.round(spawnSize * monsterRate); }
		 */
		maxRegularSpawn = Math.round(spawnSize * monsterRate);
		if (maxRegularSpawn < 2) {
			maxRegularSpawn = 2;
		} else if (maxRegularSpawn > spawnSize) {
			maxRegularSpawn = spawnSize - (spawnSize / 15);
		}
		Collection<Spawns> newSpawn = new LinkedList<>();
		Collection<Spawns> newBossSpawn = new LinkedList<>();
		for (final Spawns s : monsterSpawn) {
			if (s.getCarnivalTeam() >= 2) {
				continue; // Remove carnival spawned mobs
			}
			if (s.getMonster().getStats().isBoss()) {
				newBossSpawn.add(s);
			} else {
				newSpawn.add(s);
			}
		}
		monsterSpawn.clear();
		monsterSpawn.addAll(newBossSpawn);
		monsterSpawn.addAll(newSpawn);

		if (first && spawnSize > 0) {
			MapTimer.getInstance().register(new Runnable() {

				@Override
				public void run() {
					respawn(false);
				}
			}, createMobInterval);
		}
	}

	public final void addMonsterSpawn(final Monster monster, final int mobTime, final byte carnivalTeam, final String msg) {
		final Point newpos = calcPointBelow(monster.getPosition());
		newpos.y -= 1;
		monsterSpawn.add(new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg));
	}

	public final void addAreaMonsterSpawn(final Monster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg) {
		pos1 = calcPointBelow(pos1);
		pos2 = calcPointBelow(pos2);
		pos3 = calcPointBelow(pos3);
		pos1.y -= 1;
		pos2.y -= 1;
		pos3.y -= 1;

		monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg));
	}

	public final Collection<ChannelCharacter> getCharacters() {
		final List<ChannelCharacter> chars = new ArrayList<>();

		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			while (ltr.hasNext()) {
				chars.add(ltr.next());
			}
		} finally {
			mutex.unlock();
		}
		return chars;
	}

	public final ChannelCharacter getCharacterById_InMap(final int id) {
		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			ChannelCharacter c;
			while (ltr.hasNext()) {
				c = ltr.next();
				if (c.getId() == id) {
					return c;
				}
			}
		} finally {
			mutex.unlock();
		}
		return null;
	}

	private void updateMapObjectVisibility(final ChannelCharacter chr, final GameMapObject mo) {
		if (!chr.isMapObjectVisible(mo)) { // monster entered view range
			if (mo.getType() == GameMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= GameConstants.maxViewRangeSq()) {
				chr.addVisibleMapObject(mo);
				mo.sendSpawnData(chr.getClient());
			}
		} else { // monster left view range
			if (mo.getType() != GameMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > GameConstants.maxViewRangeSq()) {
				chr.removeVisibleMapObject(mo);
				mo.sendDestroyData(chr.getClient());
			}
		}
	}

	public void moveMonster(Monster monster, Point reportedPos) {
		monster.setPosition(reportedPos);

		mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = characters.iterator();
			while (ltr.hasNext()) {
				updateMapObjectVisibility(ltr.next(), monster);
			}
		} finally {
			mutex.unlock();
		}
	}

	public void movePlayer(final ChannelCharacter player, final Point newPosition) {
		player.setPosition(newPosition);

		final Collection<GameMapObject> visibleObjects = player.getVisibleMapObjects();
		final GameMapObject[] visibleObjectsNow = visibleObjects.toArray(new GameMapObject[visibleObjects.size()]);

		for (GameMapObject mo : visibleObjectsNow) {
			if (getMapObject(mo.getObjectId()) == mo) {
				updateMapObjectVisibility(player, mo);
			} else {
				player.removeVisibleMapObject(mo);
			}
		}
		for (GameMapObject mo : getMapObjectsInRange(player.getPosition(), GameConstants.maxViewRangeSq())) {
			if (!player.isMapObjectVisible(mo)) {
				mo.sendSpawnData(player.getClient());
				player.addVisibleMapObject(mo);
			}
		}
	}

	public Portal findClosestSpawnpoint(Point from) {
		Portal closest = null;
		double distance, shortestDistance = Double.POSITIVE_INFINITY;
		for (Portal portal : portals.values()) {
			distance = portal.getPosition().distanceSq(from);
			if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public String spawnDebug() {
		StringBuilder sb = new StringBuilder("Mapobjects in map : ");
		sb.append(this.getMapObjectSize());
		sb.append(" spawnedMonstersOnMap: ");
		sb.append(spawnedMonstersOnMap);
		sb.append(" spawnpoints: ");
		sb.append(monsterSpawn.size());
		sb.append(" maxRegularSpawn: ");
		sb.append(maxRegularSpawn);
		sb.append(" actual monsters: ");
		sb.append(getAllMonster().size());

		return sb.toString();
	}

	public final int getMapObjectSize() {
		return mapObjects.size();
	}

	public final int getCharactersSize() {
		return characters.size();
	}

	public Collection<Portal> getPortals() {
		return Collections.unmodifiableCollection(portals.values());
	}

	public int getSpawnedMonstersOnMap() {
		return spawnedMonstersOnMap.get();
	}

	private static final class MonsterSpawnTask implements DelayedPacketCreation {
		private final int oid;
		private final Monster monster;

		private MonsterSpawnTask(int oid, Monster monster) {
			this.oid = oid;
			this.monster = monster;
		}

		@Override
		public final void sendPackets(ChannelClient c) {
			// TODO: effect
			c.write(MobPacket.spawnMonster(monster, -2, 0, oid));
		}
	}

	private class ExpireMapItemJob implements Runnable {

		private GameMapItem mapitem;

		public ExpireMapItemJob(GameMapItem mapitem) {
			this.mapitem = mapitem;
		}

		@Override
		public void run() {
			if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
				if (mapitem.isPickedUp()) {
					return;
				}
				mapitem.setPickedUp(true);
				broadcastMessage(ChannelPackets.removeItemFromMap(mapitem.getObjectId(), 0, 0));
				removeMapObject(mapitem);
			}
		}
	}

	private class ActivateItemReactor implements Runnable {

		private GameMapItem mapitem;
		private Reactor reactor;
		private ChannelClient c;

		public ActivateItemReactor(GameMapItem mapitem, Reactor reactor, ChannelClient c) {
			this.mapitem = mapitem;
			this.reactor = reactor;
			this.c = c;
		}

		@Override
		public void run() {
			if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
				if (mapitem.isPickedUp()) {
					reactor.setTimerActive(false);
					return;
				}
				mapitem.setPickedUp(true);
				broadcastMessage(ChannelPackets.removeItemFromMap(mapitem.getObjectId(), 0, 0));
				removeMapObject(mapitem);
				reactor.hitReactor(c);
				reactor.setTimerActive(false);
				if (reactor.getDelay() > 0) {
					MapTimer.getInstance().schedule(new Runnable() {

						@Override
						public void run() {
							reactor.setState((byte) 0);
							broadcastMessage(ChannelPackets.triggerReactor(reactor, 0));
						}
					}, reactor.getDelay());
				}
			}
		}
	}

	public void respawn(final boolean force) {
		if (force) {
			final int numShouldSpawn = monsterSpawn.size() - spawnedMonstersOnMap.get();
			if (numShouldSpawn > 0) {
				int spawned = 0;
				for (Spawns spawnPoint : monsterSpawn) {
					spawnPoint.spawnMonster(this);
					spawned++;
					if (spawned >= numShouldSpawn) {
						break;
					}
				}
			}
		} else {
			if (getCharactersSize() <= 0) {
				return;
			}
			final int numShouldSpawn = maxRegularSpawn - spawnedMonstersOnMap.get();
			if (numShouldSpawn > 0) {
				int spawned = 0;
				final List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
				Collections.shuffle(randomSpawn);
				for (Spawns spawnPoint : randomSpawn) {
					if (spawnPoint.shouldSpawn()) {
						spawnPoint.spawnMonster(this);
						spawned++;
					}
					if (spawned >= numShouldSpawn) {
						break;
					}
				}
			}
		}
	}

	private static interface DelayedPacketCreation {

		void sendPackets(ChannelClient c);
	}

	private static interface SpawnCondition {

		boolean canSpawn(ChannelCharacter chr);
	}
}