package javastory.channel.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.Jobs;
import javastory.game.SpawnPoint;
import javastory.game.data.ItemInfoProvider;
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
	private final float monsterRate;
	private float recoveryRate;
	private GameMapEffect mapEffect;
	private short decHP = 0, createMobInterval = 9000;
	private int protectItem = 0;
	private final int mapId;
	private final int returnMapId;
	private int timeLimit;
	private int fieldLimit;
	private int maxRegularSpawn = 0;
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
		this.dropsDisabled = !this.dropsDisabled;
	}

	public final int getId() {
		return this.mapId;
	}

	public final GameMap getReturnMap() {
		return ChannelServer.getMapFactory().getMap(this.returnMapId);
	}

	public final int getReturnMapId() {
		return this.returnMapId;
	}

	public final int getForcedReturnId() {
		return this.forcedReturnMap;
	}

	public final GameMap getForcedReturnMap() {
		return ChannelServer.getMapFactory().getMap(this.forcedReturnMap);
	}

	public final void setForcedReturnMap(final int map) {
		this.forcedReturnMap = map;
	}

	public final float getRecoveryRate() {
		return this.recoveryRate;
	}

	public final void setRecoveryRate(final float recoveryRate) {
		this.recoveryRate = recoveryRate;
	}

	public final int getFieldLimit() {
		return this.fieldLimit;
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
		return this.clock;
	}

	public final void setClock(final boolean hasClock) {
		this.clock = hasClock;
	}

	public final boolean isTown() {
		return this.town;
	}

	public final void setTown(final boolean town) {
		this.town = town;
	}

	public final boolean allowPersonalShop() {
		return this.personalShop;
	}

	public final void setPersonalShop(final boolean personalShop) {
		this.personalShop = personalShop;
	}

	public final void setEverlast(final boolean everlast) {
		this.isEverlast = everlast;
	}

	public final boolean isEverlast() {
		return this.isEverlast;
	}

	public final int getHPDec() {
		return this.decHP;
	}

	public final void setHPDec(final int delta) {
		this.decHP = (short) delta;
	}

	public final int getHPDecProtect() {
		return this.protectItem;
	}

	public final void setHPDecProtect(final int delta) {
		this.protectItem = delta;
	}

	public final int getCurrentPartyId() {
		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				final PartyMember member = chr.getPartyMembership();
				if (member != null) {
					return member.getPartyId();
				}
			}
		} finally {
			this.mutex.unlock();
		}
		return -1;
	}

	public final void addMapObject(final GameMapObject mapobject) {
		this.mutex.lock();

		try {
			this.runningOid++;
			mapobject.setObjectId(this.runningOid);
			this.mapObjects.put(this.runningOid, mapobject);
		} finally {
			this.mutex.unlock();
		}
	}

	private void spawnAndAddRangedMapObject(final GameMapObject mapobject, final DelayedPacketCreation packetbakery, final SpawnCondition condition) {
		this.mutex.lock();

		try {
			this.runningOid++;
			mapobject.setObjectId(this.runningOid);
			this.mapObjects.put(this.runningOid, mapobject);

			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
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
			this.mutex.unlock();
		}
	}

	public final void removeMapObject(final int num) {
		this.mutex.lock();
		try {
			this.mapObjects.remove(Integer.valueOf(num));
		} finally {
			this.mutex.unlock();
		}
	}

	public final void removeMapObject(final GameMapObject obj) {
		this.mutex.lock();
		try {
			this.mapObjects.remove(Integer.valueOf(obj.getObjectId()));
		} finally {
			this.mutex.unlock();
		}
	}

	public final Point calcPointBelow(final Point initial) {
		final Foothold fh = this.footholds.findBelow(initial);
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
		final Point ret = this.calcPointBelow(new Point(initial.x, initial.y - 50));
		if (ret == null) {
			return fallback;
		}
		return ret;
	}

	private void dropFromMonster(final ChannelCharacter chr, final Monster mob) {
		if (this.dropsDisabled || mob.dropsDisabled()) {
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
		Item idrop;
		byte d = 1;
		final Point pos = new Point(0, mob.getPosition().y);
		final MobDropInfoProvider mi = MobDropInfoProvider.getInstance();
		final List<MobDropInfo> dropEntry = Lists.newArrayList(mi.retrieveDrop(mob.getId()));
		Collections.shuffle(dropEntry);
		for (final MobDropInfo de : dropEntry) {
			if (Randomizer.nextInt(999999) < de.Chance * globalItemRate) {
				if (droptype == 3) {
					pos.x = mobpos + (d % 2 == 0 ? 40 * (d + 1) / 2 : -(40 * (d / 2)));
				} else {
					pos.x = mobpos + (d % 2 == 0 ? 25 * (d + 1) / 2 : -(25 * (d / 2)));
				}

				if (de.ItemId == 0) {
					// meso
					int mesos = Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum;
					if (mesos > 0) {
						if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
							mesos = (int) (mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0);
						}
						final float mesoRate = ChannelServer.getInstance().getMesoRate();
						this.spawnMobMesoDrop((int) (mesos * mesoRate), this.calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
					}
				} else {
					if (GameConstants.getInventoryType(de.ItemId) == InventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii.getEquipById(de.ItemId));
					} else {
						idrop = new Item(de.ItemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1),
							(byte) 0);
					}
					this.spawnMobDrop(idrop, this.calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
				}
				d++;
			}
		}
		final List<MobGlobalDropInfo> globalEntry = mi.getGlobalDrop();
		// Global Drops
		for (final MobGlobalDropInfo de : globalEntry) {
			if (Randomizer.nextInt(999999) < de.Chance) {
				if (droptype == 3) {
					pos.x = mobpos + (d % 2 == 0 ? 40 * (d + 1) / 2 : -(40 * (d / 2)));
				} else {
					pos.x = mobpos + (d % 2 == 0 ? 25 * (d + 1) / 2 : -(25 * (d / 2)));
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
					this.spawnMobDrop(idrop, this.calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
					d++;
				}
			}
		}
	}

	private void killMonster(final Monster monster) { // For mobs with
														// removeAfter
		this.spawnedMonstersOnMap.decrementAndGet();
		monster.setHp(0);
		monster.spawnRevives(this);
		this.broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 1));
		this.removeMapObject(monster);
	}

	public final void killMonster(final Monster monster, final ChannelCharacter chr, final boolean withDrops, final boolean second, final byte animation) {
		if (monster.getId() == 8810018 && !second) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					GameMap.this.killMonster(monster, chr, true, true, (byte) 1);
					GameMap.this.killAllMonsters(true);
				}
			}, 3000);
			return;
		}
		this.spawnedMonstersOnMap.decrementAndGet();
		this.removeMapObject(monster);
		ChannelCharacter dropOwner = monster.killBy(chr);
		this.broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
		if (monster.getBuffToGive() > -1) {
			final int buffid = monster.getBuffToGive();
			final StatEffect buff = ItemInfoProvider.getInstance().getItemEffect(buffid);
			for (final GameMapObject mmo : this.getAllPlayer()) {
				final ChannelCharacter c = (ChannelCharacter) mmo;
				if (c.isAlive()) {
					buff.applyTo(c);
					switch (monster.getId()) {
					case 8810018:
					case 8820001:
						// HT nine spirit
						c.getClient().write(ChannelPackets.showOwnBuffEffect(buffid, 11));
						this.broadcastMessage(c, ChannelPackets.showBuffeffect(c.getId(), buffid, 11), false);
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
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
			LogUtil.log(LogUtil.Horntail_Log, this.MapDebug_Log());
		} else if (mobid == 8820001) {
			try {
				ChannelServer.getWorldInterface().broadcastMessage(
					ChannelPackets.serverNotice(6, "Expedition who defeated Pink Bean with invicible passion! You are the true timeless hero!"));
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
			LogUtil.log(LogUtil.Pinkbean_Log, this.MapDebug_Log());
		} else if (mobid >= 8800003 && mobid <= 8800010) {
			boolean makeZakReal = true;
			final Collection<GameMapObject> objects = this.getAllMonster();
			for (final GameMapObject object : objects) {
				final Monster mons = (Monster) object;
				if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
					makeZakReal = false;
					break;
				}
			}
			if (makeZakReal) {
				for (final GameMapObject object : objects) {
					final Monster mons = (Monster) object;
					if (mons.getId() == 8800000) {
						final Point pos = mons.getPosition();
						this.killAllMonsters(true);
						this.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8800000), pos);
						break;
					}
				}
			}
		}
		if (withDrops) {
			if (dropOwner == null) {
				dropOwner = chr;
			}
			this.dropFromMonster(dropOwner, monster);
		}
	}

	public final void killAllMonsters(final boolean animate) {
		for (final GameMapObject monstermo : this.getAllMonster()) {
			final Monster monster = (Monster) monstermo;
			this.spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			this.broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
			this.removeMapObject(monster);
		}
	}

	public final void killMonster(final int monsId) {
		for (final GameMapObject mmo : this.getAllMonster()) {
			if (((Monster) mmo).getId() == monsId) {
				this.spawnedMonstersOnMap.decrementAndGet();
				this.removeMapObject(mmo);
				this.broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
				break;
			}
		}
	}

	private String MapDebug_Log() {
		final StringBuilder sb = new StringBuilder("Defeat time : ");
		sb.append(LogUtil.CurrentReadable_Time());

		sb.append(" | Mapid : ").append(this.mapId);

		final List<GameMapObject> players = this.getAllPlayer();
		sb.append(" Users [").append(players.size()).append("] | ");
		final Iterator<GameMapObject> itr = players.iterator();
		while (itr.hasNext()) {
			sb.append(((ChannelCharacter) itr.next()).getName()).append(", ");
		}
		return sb.toString();
	}

	public final void destroyReactor(final int oid) {
		final Reactor reactor = this.getReactorByOid(oid);
		this.broadcastMessage(ChannelPackets.destroyReactor(reactor));
		reactor.setAlive(false);
		this.removeMapObject(reactor);
		reactor.setTimerActive(false);

		if (reactor.getDelay() > 0) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public final void run() {
					GameMap.this.respawnReactor(reactor);
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
		for (final GameMapObject o : this.getAllReactor()) {
			((Reactor) o).setState((byte) 0);
			((Reactor) o).setTimerActive(false);
			this.broadcastMessage(ChannelPackets.triggerReactor((Reactor) o, 0));
		}
	}

	public final void setReactorState() {
		for (final GameMapObject o : this.getAllReactor()) {
			((Reactor) o).setState((byte) 1);
			((Reactor) o).setTimerActive(false);
			this.broadcastMessage(ChannelPackets.triggerReactor((Reactor) o, 1));
		}
	}

	/*
	 * command to shuffle the positions of all reactors in a map for PQ purposes
	 * (such as ZPQ/LMPQ)
	 */
	public final void shuffleReactors() {
		final List<Point> points = Lists.newArrayList();

		for (final GameMapObject o : this.getAllReactor()) {
			points.add(((Reactor) o).getPosition());
		}
		Collections.shuffle(points);
		for (final GameMapObject o : this.getAllReactor()) {
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

		this.mutex.lock();

		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			ChannelCharacter chr;
			while (ltr.hasNext()) {
				chr = ltr.next();
				if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
					mincontrolled = chr.getControlledMonsters().size();
					newController = chr;
				}
			}
		} finally {
			this.mutex.unlock();
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
		return this.mapObjects.get(oid);
	}

	public final int containsNPC(final int npcid) {
		for (final GameMapObject obj : this.getAllNPC()) {
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
		final GameMapObject mmo = this.getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.MONSTER) {
			return (Monster) mmo;
		}
		return null;
	}

	public final Npc getNPCByOid(final int oid) {
		final GameMapObject mmo = this.getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.NPC) {
			return (Npc) mmo;
		}
		return null;
	}

	public final Reactor getReactorByOid(final int oid) {
		final GameMapObject mmo = this.getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == GameMapObjectType.REACTOR) {
			return (Reactor) mmo;
		}
		return null;
	}

	public final Reactor getReactorByName(final String name) {
		for (final GameMapObject obj : this.getAllReactor()) {
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
		npc.setFoothold(this.getFootholds().findBelow(pos).getId());
		npc.setCustom(true);
		this.addMapObject(npc);
		this.broadcastMessage(ChannelPackets.spawnNpc(npc, true));
	}

	public final void removeNpc(final int id) {
		final List<GameMapObject> npcs = this.getAllNPC();
		for (final GameMapObject npcmo : npcs) {
			final Npc npc = (Npc) npcmo;
			if (npc.isCustom() && npc.getId() == id) {
				this.broadcastMessage(ChannelPackets.removeNpc(npc.getObjectId()));
				this.removeMapObject(npc.getObjectId());
			}
		}
	}

	public final void spawnMonster_sSack(final Monster mob, final Point pos, final int spawnType) {
		final Point spos = this.calcPointBelow(new Point(pos.x, pos.y - 1));
		mob.setPosition(spos);
		this.spawnMonster(mob, spawnType);
	}

	public final void spawnMonsterOnGroundBelow(final Monster mob, final Point pos) {
		final Point spos = this.calcPointBelow(new Point(pos.x, pos.y - 1));
		mob.setPosition(spos);
		this.spawnMonster(mob, -2);
	}

	public final void spawnZakum(final Point pos) {
		final Monster mainb = LifeFactory.getMonster(8800000);
		final Point spos = this.calcPointBelow(new Point(pos.x, pos.y - 1));
		mainb.setPosition(spos);
		mainb.setFake(true);

		// Might be possible to use the map object for reference in future.
		this.spawnFakeMonster(mainb);

		final int[] zakpart = { 8800003, 8800004, 8800005, 8800006, 8800007, 8800008, 8800009, 8800010 };

		for (final int i : zakpart) {
			final Monster part = LifeFactory.getMonster(i);
			part.setPosition(spos);

			this.spawnMonster(part, -2);
		}
	}

	public final void spawnFakeMonsterOnGroundBelow(final Monster mob, final Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = this.calcPointBelow(spos);
		spos.y -= 1;
		mob.setPosition(spos);
		this.spawnFakeMonster(mob);
	}

	private void checkRemoveAfter(final Monster monster) {
		final int ra = monster.getStats().getRemoveAfter();

		if (ra > 0) {
			MapTimer.getInstance().schedule(new Runnable() {

				@Override
				public final void run() {
					if (monster != null) {
						GameMap.this.killMonster(monster);
					}
				}
			}, ra * 1000);
		}
	}

	public final void spawnRevives(final Monster monster, final int oid) {
		monster.setMap(this);
		this.checkRemoveAfter(monster);

		this.spawnAndAddRangedMapObject(monster, new MonsterSpawnTask(oid, monster), null);
		this.updateMonsterController(monster);

		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnMonster(final Monster monster, final int spawnType) {
		monster.setMap(this);
		this.checkRemoveAfter(monster);

		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(final ChannelClient c) {
				c.write(MobPacket.spawnMonster(monster, spawnType, 0, 0));
			}
		}, null);
		this.updateMonsterController(monster);

		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnMonsterWithEffect(final Monster monster, final int effect, final Point pos) {
		try {
			monster.setMap(this);
			monster.setPosition(pos);

			this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

				@Override
				public final void sendPackets(final ChannelClient c) {
					c.write(MobPacket.spawnMonster(monster, -2, effect, 0));
				}
			}, null);
			this.updateMonsterController(monster);

			this.spawnedMonstersOnMap.incrementAndGet();
		} catch (final Exception e) {
		}
	}

	public final void spawnFakeMonster(final Monster monster) {
		monster.setMap(this);
		monster.setFake(true);

		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(final ChannelClient c) {
				c.write(MobPacket.spawnMonster(monster, -2, 0xfc, 0));
				// c.write(MobPacket.spawnFakeMonster(monster, 0));
			}
		}, null);
		this.updateMonsterController(monster);

		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public final void spawnReactor(final Reactor reactor) {
		reactor.setMap(this);

		this.spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.spawnReactor(reactor));
			}
		}, null);
	}

	private void respawnReactor(final Reactor reactor) {
		reactor.setState((byte) 0);
		reactor.setAlive(true);
		this.spawnReactor(reactor);
	}

	public final void spawnDoor(final Door door) {
		this.spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

			@Override
			public final void sendPackets(final ChannelClient c) {
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
		this.spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.spawnDragon(summon));
			}
		}, null);
	}

	public final void spawnSummon(final Summon summon) {
		this.spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.spawnSummon(summon, summon.getSkillLevel(), true));
			}
		}, null);
	}

	public final void spawnMist(final Mist mist, final int duration, final boolean poison, final boolean fake) {
		this.spawnAndAddRangedMapObject(mist, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.spawnMist(mist));
			}
		}, null);

		final MapTimer tMan = MapTimer.getInstance();
		final ScheduledFuture<?> poisonSchedule;

		if (poison) {
			poisonSchedule = tMan.register(new Runnable() {

				@Override
				public void run() {
					for (final GameMapObject mo : GameMap.this.getMapObjectsInRect(mist.getBox(), Collections.singletonList(GameMapObjectType.MONSTER))) {
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
				GameMap.this.broadcastMessage(ChannelPackets.removeMist(mist.getObjectId()));
				GameMap.this.removeMapObject(mist);
				if (poisonSchedule != null) {
					poisonSchedule.cancel(false);
				}
			}
		}, duration);
	}

	public final void disappearingItemDrop(final GameMapObject dropper, final ChannelCharacter owner, final Item item, final Point pos) {
		final Point droppos = this.calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) 1, false);
		this.broadcastMessage(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
	}

	public final void spawnMesoDrop(final int meso, final Point position, final GameMapObject dropper, final ChannelCharacter owner, final boolean playerDrop,
		final byte droptype) {
		final Point droppos = this.calcDropPos(position, position);
		final GameMapItem mdrop = new GameMapItem(meso, droppos, dropper, owner, droptype, playerDrop);
		this.spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		if (!this.isEverlast) {
			MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
		}
	}

	public final void spawnMobMesoDrop(final int meso, final Point position, final GameMapObject dropper, final ChannelCharacter owner,
		final boolean playerDrop, final byte droptype) {
		final GameMapItem mdrop = new GameMapItem(meso, position, dropper, owner, droptype, playerDrop);
		this.spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(mdrop, dropper.getPosition(), position, (byte) 1));
			}
		}, null);
		MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
	}

	private void spawnMobDrop(final Item idrop, final Point dropPos, final Monster mob, final ChannelCharacter chr, final byte droptype, final short questid) {
		final GameMapItem mdrop = new GameMapItem(idrop, dropPos, mob, chr, droptype, false, questid);
		this.spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				if (questid <= 0 || c.getPlayer().getQuestCompletionStatus(questid) == 1) {
					c.write(ChannelPackets.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1));
				}
			}
		}, null);
		MapTimer.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
		this.activateItemReactors(mdrop, chr.getClient());
	}

	public final void spawnItemDrop(final GameMapObject dropper, final ChannelCharacter owner, final Item item, final Point pos, final boolean ffaDrop,
		final boolean playerDrop) {
		final Point droppos = this.calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) 0, playerDrop);
		this.spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(final ChannelClient c) {
				c.write(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		this.broadcastMessage(ChannelPackets.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));
		if (!this.isEverlast) {
			MapTimer.getInstance().schedule(new ExpireMapItemJob(drop), 180000);
			this.activateItemReactors(drop, owner.getClient());
		}
	}

	private void activateItemReactors(final GameMapItem drop, final ChannelClient c) {
		final Item item = drop.getItem();

		for (final GameMapObject o : this.getAllReactor()) {
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

		this.mutex.lock();

		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			ChannelCharacter chars;
			while (ltr.hasNext()) {
				chars = ltr.next();
				this.broadcastMessage(ChannelPackets.updateAriantPQRanking(chars.getName(), 0, false));
				this.broadcastMessage(ChannelPackets.serverNotice(0, ChannelPackets.updateAriantPQRanking(chars.getName(), 0, false).toString()));
				if (this.getCharactersSize() > i) {
					this.broadcastMessage(ChannelPackets.updateAriantPQRanking(null, 0, true));
					this.broadcastMessage(ChannelPackets.serverNotice(0, ChannelPackets.updateAriantPQRanking(chars.getName(), 0, true).toString()));
				}
				i++;
			}
		} finally {
			this.mutex.unlock();
		}
	}

	public final void returnEverLastItem(final ChannelCharacter chr) {
		for (final GameMapObject o : this.getAllItems()) {
			final GameMapItem item = (GameMapItem) o;
			if (item.getOwner() == chr.getId()) {
				item.setPickedUp(true);
				this.broadcastMessage(ChannelPackets.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getPosition());
				if (item.getMeso() > 0) {
					chr.gainMeso(item.getMeso(), false);
				} else {
					InventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
				}
				this.removeMapObject(item);
			}
		}
	}

	public final void startMapEffect(final String msg, final int itemId) {
		if (this.mapEffect != null) {
			return;
		}
		this.mapEffect = new GameMapEffect(msg, itemId);
		this.broadcastMessage(this.mapEffect.makeStartData());
		MapTimer.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				GameMap.this.broadcastMessage(GameMap.this.mapEffect.makeDestroyData());
				GameMap.this.mapEffect = null;
			}
		}, 30000);
	}

	public final void addPlayer(final ChannelCharacter character) {
		this.mutex.lock();
		try {
			this.characters.add(character);
			this.mapObjects.put(character.getObjectId(), character);
		} finally {
			this.mutex.unlock();
		}
		if (!character.isHidden()) {
			this.broadcastMessage(ChannelPackets.spawnPlayerMapObject(character));
		}
		this.sendObjectPlacement(character);

		final ChannelClient client = character.getClient();

		client.write(ChannelPackets.spawnPlayerMapObject(character));

		if (!this.onFirstUserEnter.equals("")) {
			if (this.getCharactersSize() == 1) {
				MapScriptMethods.startScript_FirstUser(client, this.onFirstUserEnter);
			}
		}
		if (!this.onUserEnter.equals("")) {
			MapScriptMethods.startScript_User(client, this.onUserEnter);
		}
		for (final Pet pet : character.getPets()) {
			if (pet.isSummoned()) {
				final GamePacket packet = PetPacket.showPet(character, pet);
				this.broadcastMessage(character, packet, false);
			}
		}
		switch (this.mapId) {
		case 809000101:
		case 809000201:
			client.write(ChannelPackets.showEquipEffect());
			break;
		}
		if (this.getHPDec() > 0) {
			character.startHurtHp();
		}
		final PartyMember member = character.getPartyMembership();
		if (member != null) {
			try {
				final Party party = ChannelServer.getWorldInterface().getParty(member.getPartyId());
				character.silentPartyUpdate();
				client.write(ChannelPackets.updateParty(client.getChannelId(), party, PartyOperation.SILENT_UPDATE, null));
				character.updatePartyMemberHP();
				character.receivePartyMemberHP();
			} catch (final RemoteException ex) {
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
		if (this.mapEffect != null) {
			this.mapEffect.sendStartData(client);
		}
		if (this.timeLimit > 0 && this.getForcedReturnMap() != null) {
			character.startMapTimeLimitTask(this.timeLimit, this.getForcedReturnMap());
		}

		if (character.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			if (FieldLimitType.Mount.check(this.fieldLimit)) {
				character.cancelBuffStats(BuffStat.MONSTER_RIDING);
			}
		}

		final EventInstanceManager eventInstance = character.getEventInstance();
		if (eventInstance != null && eventInstance.isTimerStarted()) {
			client.write(ChannelPackets.getClock((int) (eventInstance.getTimeLeft() / 1000)));
		}

		if (this.hasClock()) {
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
			this.spawnDragon(character.getDragon());
			this.updateMapObjectVisibility(character, character.getDragon());
		}
	}

	public final void removePlayer(final ChannelCharacter character) {
		// log.warn("[dc] [level2] Player {} leaves map {}", new Object[] {
		// chr.getName(), mapid });

		if (this.isEverlast) {
			this.returnEverLastItem(character);
		}
		this.mutex.lock();
		try {
			this.characters.remove(character);
		} finally {
			this.mutex.unlock();
		}
		this.removeMapObject(Integer.valueOf(character.getObjectId()));
		this.broadcastMessage(ChannelPackets.removePlayerFromMap(character.getId()));

		for (final Monster monster : character.getControlledMonsters()) {
			monster.setController(null);
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
			this.updateMonsterController(monster);
		}
		character.leaveMap();
		character.cancelMapTimeLimitTask();

		for (final Summon summon : character.getSummons().values()) {
			if (summon.isPuppet()) {
				character.cancelBuffStats(BuffStat.PUPPET);
				character.cancelBuffStats(BuffStat.MIRROR_TARGET);
			} else {
				this.removeMapObject(summon);
			}
		}
		if (character.getDragon() != null) {
			this.removeMapObject(character.getDragon());
		}
	}

	public final void broadcastMessage(final GamePacket packet) {
		this.broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	public final void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final boolean repeatToSource) {
		this.broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
	}

	/*
	 * public void broadcastMessage(GameCharacter source, GamePacket packet,
	 * boolean repeatToSource, boolean ranged) { broadcastMessage(repeatToSource
	 * ? null : source, packet, ranged ? GameCharacter.MAX_VIEW_RANGE_SQ :
	 * Double.POSITIVE_INFINITY, source.getPosition()); }
	 */
	public final void broadcastMessage(final GamePacket packet, final Point rangedFrom) {
		this.broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
	}

	public final void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final Point rangedFrom) {
		this.broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
	}

	private void broadcastMessage(final ChannelCharacter source, final GamePacket packet, final double rangeSq, final Point rangedFrom) {
		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
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
			this.mutex.unlock();
		}
	}

	private void sendObjectPlacement(final ChannelCharacter c) {
		if (c == null) {
			return;
		}
		for (final GameMapObject o : this.getAllMonster()) {
			this.updateMonsterController((Monster) o);
		}
		for (final GameMapObject o : this.getMapObjectsInRange(c.getPosition(), GameConstants.maxViewRangeSq(), GameConstants.rangedMapObjectTypes)) {
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
		final List<GameMapObject> ret = Lists.newLinkedList();

		this.mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = this.mapObjects.values().iterator();
			GameMapObject obj;
			while (ltr.hasNext()) {
				obj = ltr.next();
				if (from.distanceSq(obj.getPosition()) <= rangeSq) {
					ret.add(obj);
				}
			}
		} finally {
			this.mutex.unlock();
		}
		return ret;
	}

	public final List<GameMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<GameMapObjectType> MapObject_types) {
		final List<GameMapObject> ret = Lists.newLinkedList();

		this.mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = this.mapObjects.values().iterator();
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
			this.mutex.unlock();
		}
		return ret;
	}

	public final List<GameMapObject> getMapObjectsInRect(final Rectangle box, final List<GameMapObjectType> MapObject_types) {
		final List<GameMapObject> ret = Lists.newLinkedList();

		this.mutex.lock();
		try {
			final Iterator<GameMapObject> ltr = this.mapObjects.values().iterator();
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
			this.mutex.unlock();
		}
		return ret;
	}

	public final List<ChannelCharacter> getPlayersInRect(final Rectangle box, final List<ChannelCharacter> CharacterList) {
		final List<ChannelCharacter> character = Lists.newLinkedList();

		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
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
			this.mutex.unlock();
		}
		return character;
	}

	public final List<GameMapObject> getAllItems() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
	}

	public final List<GameMapObject> getAllNPC() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.NPC));
	}

	public final List<GameMapObject> getAllReactor() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
	}

	public final List<GameMapObject> getAllPlayer() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.PLAYER));
	}

	public final List<GameMapObject> getAllMonster() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER));
	}

	public final List<GameMapObject> getAllDoor() {
		return this.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.DOOR));
	}

	public final void addPortal(final Portal myPortal) {
		this.portals.put(myPortal.getId(), myPortal);
	}

	public final Portal getPortal(final String portalname) {
		for (final Portal port : this.portals.values()) {
			if (port.getName().equals(portalname)) {
				return port;
			}
		}
		return null;
	}

	public final Portal getPortal(final int portalid) {
		return this.portals.get(portalid);
	}

	public final void addMapleArea(final Rectangle rec) {
		this.areas.add(rec);
	}

	public final List<Rectangle> getAreas() {
		return Lists.newArrayList(this.areas);
	}

	public final Rectangle getArea(final int index) {
		return this.areas.get(index);
	}

	public final void setFootholds(final FootholdTree footholds) {
		this.footholds = footholds;
	}

	public final FootholdTree getFootholds() {
		return this.footholds;
	}

	public final void loadMonsterRate(final boolean first) {
		final int spawnSize = this.monsterSpawn.size();
		/*
		 * if (spawnSize >= 25 || monsterRate > 1.5) { maxRegularSpawn =
		 * Math.round(spawnSize / monsterRate); } else { maxRegularSpawn =
		 * Math.round(spawnSize * monsterRate); }
		 */
		this.maxRegularSpawn = Math.round(spawnSize * this.monsterRate);
		if (this.maxRegularSpawn < 2) {
			this.maxRegularSpawn = 2;
		} else if (this.maxRegularSpawn > spawnSize) {
			this.maxRegularSpawn = spawnSize - spawnSize / 15;
		}
		final Collection<Spawns> newSpawn = Lists.newLinkedList();
		final Collection<Spawns> newBossSpawn = Lists.newLinkedList();
		for (final Spawns s : this.monsterSpawn) {
			if (s.getCarnivalTeam() >= 2) {
				continue; // Remove carnival spawned mobs
			}
			if (s.getMonster().getStats().isBoss()) {
				newBossSpawn.add(s);
			} else {
				newSpawn.add(s);
			}
		}
		this.monsterSpawn.clear();
		this.monsterSpawn.addAll(newBossSpawn);
		this.monsterSpawn.addAll(newSpawn);

		if (first && spawnSize > 0) {
			MapTimer.getInstance().register(new Runnable() {

				@Override
				public void run() {
					GameMap.this.respawn(false);
				}
			}, this.createMobInterval);
		}
	}

	public final void addMonsterSpawn(final Monster monster, final int mobTime, final byte carnivalTeam, final String msg) {
		final Point newpos = this.calcPointBelow(monster.getPosition());
		newpos.y -= 1;
		this.monsterSpawn.add(new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg));
	}

	public final void addAreaMonsterSpawn(final Monster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg) {
		pos1 = this.calcPointBelow(pos1);
		pos2 = this.calcPointBelow(pos2);
		pos3 = this.calcPointBelow(pos3);
		pos1.y -= 1;
		pos2.y -= 1;
		pos3.y -= 1;

		this.monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg));
	}

	public final Collection<ChannelCharacter> getCharacters() {
		final List<ChannelCharacter> chars = Lists.newArrayList();

		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			while (ltr.hasNext()) {
				chars.add(ltr.next());
			}
		} finally {
			this.mutex.unlock();
		}
		return chars;
	}

	public final ChannelCharacter getCharacterById_InMap(final int id) {
		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			ChannelCharacter c;
			while (ltr.hasNext()) {
				c = ltr.next();
				if (c.getId() == id) {
					return c;
				}
			}
		} finally {
			this.mutex.unlock();
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

	public void moveMonster(final Monster monster, final Point reportedPos) {
		monster.setPosition(reportedPos);

		this.mutex.lock();
		try {
			final Iterator<ChannelCharacter> ltr = this.characters.iterator();
			while (ltr.hasNext()) {
				this.updateMapObjectVisibility(ltr.next(), monster);
			}
		} finally {
			this.mutex.unlock();
		}
	}

	public void movePlayer(final ChannelCharacter player, final Point newPosition) {
		player.setPosition(newPosition);

		final Collection<GameMapObject> visibleObjects = player.getVisibleMapObjects();
		final GameMapObject[] visibleObjectsNow = visibleObjects.toArray(new GameMapObject[visibleObjects.size()]);

		for (final GameMapObject mo : visibleObjectsNow) {
			if (this.getMapObject(mo.getObjectId()) == mo) {
				this.updateMapObjectVisibility(player, mo);
			} else {
				player.removeVisibleMapObject(mo);
			}
		}
		for (final GameMapObject mo : this.getMapObjectsInRange(player.getPosition(), GameConstants.maxViewRangeSq())) {
			if (!player.isMapObjectVisible(mo)) {
				mo.sendSpawnData(player.getClient());
				player.addVisibleMapObject(mo);
			}
		}
	}

	public Portal findClosestSpawnpoint(final Point from) {
		Portal closest = null;
		double distance, shortestDistance = Double.POSITIVE_INFINITY;
		for (final Portal portal : this.portals.values()) {
			distance = portal.getPosition().distanceSq(from);
			if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public String spawnDebug() {
		final StringBuilder sb = new StringBuilder("Mapobjects in map : ");
		sb.append(this.getMapObjectSize());
		sb.append(" spawnedMonstersOnMap: ");
		sb.append(this.spawnedMonstersOnMap);
		sb.append(" spawnpoints: ");
		sb.append(this.monsterSpawn.size());
		sb.append(" maxRegularSpawn: ");
		sb.append(this.maxRegularSpawn);
		sb.append(" actual monsters: ");
		sb.append(this.getAllMonster().size());

		return sb.toString();
	}

	public final int getMapObjectSize() {
		return this.mapObjects.size();
	}

	public final int getCharactersSize() {
		return this.characters.size();
	}

	public Collection<Portal> getPortals() {
		return Collections.unmodifiableCollection(this.portals.values());
	}

	public int getSpawnedMonstersOnMap() {
		return this.spawnedMonstersOnMap.get();
	}

	private static final class MonsterSpawnTask implements DelayedPacketCreation {
		private final int oid;
		private final Monster monster;

		private MonsterSpawnTask(final int oid, final Monster monster) {
			this.oid = oid;
			this.monster = monster;
		}

		@Override
		public final void sendPackets(final ChannelClient c) {
			// TODO: effect
			c.write(MobPacket.spawnMonster(this.monster, -2, 0, this.oid));
		}
	}

	private class ExpireMapItemJob implements Runnable {

		private final GameMapItem mapitem;

		public ExpireMapItemJob(final GameMapItem mapitem) {
			this.mapitem = mapitem;
		}

		@Override
		public void run() {
			if (this.mapitem != null && this.mapitem == GameMap.this.getMapObject(this.mapitem.getObjectId())) {
				if (this.mapitem.isPickedUp()) {
					return;
				}
				this.mapitem.setPickedUp(true);
				GameMap.this.broadcastMessage(ChannelPackets.removeItemFromMap(this.mapitem.getObjectId(), 0, 0));
				GameMap.this.removeMapObject(this.mapitem);
			}
		}
	}

	private class ActivateItemReactor implements Runnable {

		private final GameMapItem mapitem;
		private final Reactor reactor;
		private final ChannelClient c;

		public ActivateItemReactor(final GameMapItem mapitem, final Reactor reactor, final ChannelClient c) {
			this.mapitem = mapitem;
			this.reactor = reactor;
			this.c = c;
		}

		@Override
		public void run() {
			if (this.mapitem != null && this.mapitem == GameMap.this.getMapObject(this.mapitem.getObjectId())) {
				if (this.mapitem.isPickedUp()) {
					this.reactor.setTimerActive(false);
					return;
				}
				this.mapitem.setPickedUp(true);
				GameMap.this.broadcastMessage(ChannelPackets.removeItemFromMap(this.mapitem.getObjectId(), 0, 0));
				GameMap.this.removeMapObject(this.mapitem);
				this.reactor.hitReactor(this.c);
				this.reactor.setTimerActive(false);
				if (this.reactor.getDelay() > 0) {
					MapTimer.getInstance().schedule(new Runnable() {

						@Override
						public void run() {
							ActivateItemReactor.this.reactor.setState((byte) 0);
							GameMap.this.broadcastMessage(ChannelPackets.triggerReactor(ActivateItemReactor.this.reactor, 0));
						}
					}, this.reactor.getDelay());
				}
			}
		}
	}

	public void respawn(final boolean force) {
		if (force) {
			final int numShouldSpawn = this.monsterSpawn.size() - this.spawnedMonstersOnMap.get();
			if (numShouldSpawn > 0) {
				int spawned = 0;
				for (final Spawns spawnPoint : this.monsterSpawn) {
					spawnPoint.spawnMonster(this);
					spawned++;
					if (spawned >= numShouldSpawn) {
						break;
					}
				}
			}
		} else {
			if (this.getCharactersSize() <= 0) {
				return;
			}
			final int numShouldSpawn = this.maxRegularSpawn - this.spawnedMonstersOnMap.get();
			if (numShouldSpawn > 0) {
				int spawned = 0;
				final List<Spawns> randomSpawn = Lists.newArrayList(this.monsterSpawn);
				Collections.shuffle(randomSpawn);
				for (final Spawns spawnPoint : randomSpawn) {
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