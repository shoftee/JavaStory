/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package scripting;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.ScriptException;

import client.GameCharacter;
import client.QuestStatus;
import handling.world.Party;
import handling.world.PartyMember;
import server.CarnivalParty;
import server.TimerManager;
import server.Squad;
import server.quest.Quest;
import server.life.Monster;
import server.maps.GameMap;
import server.maps.GameMapFactory;
import tools.MaplePacketCreator;

public class EventInstanceManager {

    private List<GameCharacter> chars = new LinkedList<GameCharacter>();
    private List<Monster> mobs = new LinkedList<Monster>();
    private Map<GameCharacter, Integer> killCount = new HashMap<GameCharacter, Integer>();
    private EventManager em;
    private GameMapFactory mapFactory;
    private String name;
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private List<Integer> mapIds = new LinkedList<Integer>();
    private ScheduledFuture<?> eventTimer;
    private final Lock mutex = new ReentrantLock();
	private int world;

    public EventInstanceManager(EventManager em, String name, GameMapFactory factory, int world) {
	this.em = em;
	this.name = name;
	mapFactory = factory;
	this.world = world;
    }

    public void registerPlayer(GameCharacter chr) {
	try {
	    mutex.lock();
	    try {
		chars.add(chr);
	    } finally {
		mutex.unlock();
	    }
	    chr.setEventInstance(this);
	    em.getIv().invokeFunction("playerEntry", this, chr);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public void changedMap(final GameCharacter chr, final int mapid) {
	try {
	    em.getIv().invokeFunction("changedMap", this, chr, mapid);
	} catch (NullPointerException npe) {
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public void timeOut(final long delay, final EventInstanceManager eim) {
	eventTimer = TimerManager.getInstance().schedule(new Runnable() {

	    public void run() {
		try {
		    em.getIv().invokeFunction("scheduledTimeout", eim);
		} catch (NullPointerException npe) {
		} catch (ScriptException ex) {
		    ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
		    ex.printStackTrace();
		}
	    }
	}, delay);
    }

    public void stopEventTimer() {
	eventTime = 0;
	timeStarted = 0;
	if (eventTimer != null) {
	    eventTimer.cancel(false);
	}
    }

    public void restartEventTimer(long time) {
	timeStarted = System.currentTimeMillis();
	eventTime = time;
	if (eventTimer != null) {
	    eventTimer.cancel(false);
	}
	eventTimer = null;
	final int timesend = (int) time / 1000;

	mutex.lock();
	try {
	    for (final GameCharacter chr : chars) {
		chr.getClient().write(MaplePacketCreator.getClock(timesend));
	    }
	} finally {
	    mutex.unlock();
	}
	timeOut(time, this);
    }

    public void startEventTimer(long time) {
	timeStarted = System.currentTimeMillis();
	eventTime = time;
	final int timesend = (int) time / 1000;

	mutex.lock();
	try {
	    for (final GameCharacter chr : chars) {
		chr.getClient().write(MaplePacketCreator.getClock(timesend));
	    }
	} finally {
	    mutex.unlock();
	}
	timeOut(time, this);
    }

    public boolean isTimerStarted() {
	return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
	return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(Party party, GameMap map) {
	for (PartyMember pc : party.getMembers()) {
	    GameCharacter c = map.getCharacterById_InMap(pc.getId());
	    registerPlayer(c);
	}
    }

    public void unregisterPlayer(final GameCharacter chr) {
	mutex.lock();
	try {
	    chars.remove(chr);
	} finally {
	    mutex.unlock();
	}
	chr.setEventInstance(null);
    }

    public final boolean disposeIfPlayerBelow(final byte size, final int towarp) {
	GameMap map = null;
	if (towarp != 0) {
	    map = this.getMapFactory().getMap(towarp);
	}
	mutex.lock();
	try {
	    if (chars.size() <= size) {

		GameCharacter chr;
		for (int i = 0; i < chars.size(); i++) {
		    chr = chars.get(i);
		    unregisterPlayer(chr);

		    if (towarp != 0) {
			chr.changeMap(map, map.getPortal(0));
		    }
		}
		dispose();
		return true;
	    }
	} finally {
	    mutex.unlock();
	}
	return false;
    }

    public final void saveBossQuest(final int points) {
	mutex.lock();
	try {
	    for (GameCharacter chr : chars) {
		final QuestStatus record = chr.getQuestNAdd(Quest.getInstance(150001));

		if (record.getCustomData() != null) {
		    record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
		} else {
		    record.setCustomData(String.valueOf(points)); // First time
		}
	    }
	} finally {
	    mutex.unlock();
	}
    }

    public List<GameCharacter> getPlayers() {
	return Collections.unmodifiableList(chars);
    }

    public final int getPlayerCount() {
	return chars.size();
    }

    public void registerMonster(Monster mob) {
	mobs.add(mob);
	mob.setEventInstance(this);
    }

    public void unregisterMonster(Monster mob) {
	mobs.remove(mob);
	mob.setEventInstance(null);
	if (mobs.size() == 0) {
	    try {
		em.getIv().invokeFunction("allMonstersDead", this);
	    } catch (ScriptException ex) {
		ex.printStackTrace();
	    } catch (NoSuchMethodException ex) {
		ex.printStackTrace();
	    }
	}
    }

    public void playerKilled(GameCharacter chr) {
	try {
	    em.getIv().invokeFunction("playerDead", this, chr);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public boolean revivePlayer(GameCharacter chr) {
	try {
	    Object b = em.getIv().invokeFunction("playerRevive", this, chr);
	    if (b instanceof Boolean) {
		return (Boolean) b;
	    }
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
	return true;
    }

    public void playerDisconnected(final GameCharacter chr) {
	try {
	    byte ret = ((Double) em.getIv().invokeFunction("playerDisconnected", this, chr)).byteValue();

	    if (ret == 0) {
		unregisterPlayer(chr);
		if (getPlayerCount() <= 0) {
		    dispose();
		}
	    } else {
		mutex.lock();
		try {
		    if (ret > 0) {
			unregisterPlayer(chr);
			if (getPlayerCount() < ret) {
			    for (GameCharacter player : chars) {
				removePlayer(player);
			    }
			    dispose();
			}
		    } else {
			unregisterPlayer(chr);
			ret *= -1;

			if (isLeader(chr)) {
			    for (GameCharacter player : chars) {
				removePlayer(player);
			    }
			    dispose();
			} else {
			    if (getPlayerCount() < ret) {
				for (GameCharacter player : chars) {
				    removePlayer(player);
				}
				dispose();
			    }
			}
		    }
		} finally {
		    mutex.unlock();
		}
	    }
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    /**
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(final GameCharacter chr, final Monster mob) {
	try {
	    Integer kc = killCount.get(chr);
	    int inc = ((Double) em.getIv().invokeFunction("monsterValue", this, mob.getId())).intValue();
	    if (kc == null) {
		kc = inc;
	    } else {
		kc += inc;
	    }
	    killCount.put(chr, kc);
	    if (chr.getCarnivalParty() != null) {
		em.getIv().invokeFunction("monsterKilled", this, chr, mob.getStats().getCP());
	    }
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public int getKillCount(GameCharacter chr) {
	Integer kc = killCount.get(chr);
	if (kc == null) {
	    return 0;
	} else {
	    return kc;
	}
    }

    public void dispose() {
	mutex.lock();
	try {
	    chars.clear();
	    chars = null;
	} finally {
	    mutex.unlock();
	}
	mobs.clear();
	mobs = null;
	killCount.clear();
	killCount = null;
	timeStarted = 0;
	eventTime = 0;
	props.clear();
	props = null;
	for (final Integer i : mapIds) {
	    mapFactory.removeInstanceMap(i);
	}
	mapIds = null;
	mapFactory = null;
	em.disposeInstance(name);
	em = null;
    }

    public final void broadcastPlayerMsg(final int type, final String msg) {
	mutex.lock();
	try {
	    for (final GameCharacter chr : chars) {
		chr.getClient().write(MaplePacketCreator.serverNotice(type, msg));
	    }
	} finally {
	    mutex.unlock();
	}
    }

    public final GameMap createInstanceMap(final int mapid) {
	int assignedid = em.getChannelServer().getEventSM(world).getNewInstanceMapId();
	mapIds.add(assignedid);
	return mapFactory.CreateInstanceMap(mapid, true, true, true, assignedid);
    }

    public final GameMap createInstanceMapS(final int mapid) {
	final int assignedid = em.getChannelServer().getEventSM(world).getNewInstanceMapId();
	mapIds.add(assignedid);
	return mapFactory.CreateInstanceMap(mapid, false, false, false, assignedid);
    }

    public final GameMapFactory getMapFactory() {
	return mapFactory;
    }

    public final GameMap getMapInstance(int args) {
	final GameMap map = mapFactory.getInstanceMap(mapIds.get(args));

	// in case reactors need shuffling and we are actually loading the map
	if (!mapFactory.isInstanceMapLoaded(mapIds.get(args))) {
	    if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
		map.shuffleReactors();
	    }
	}
	return map;
    }

    public final void schedule(final String methodName, final long delay) {
	TimerManager.getInstance().schedule(new Runnable() {

	    public void run() {
		try {
		    em.getIv().invokeFunction(methodName, EventInstanceManager.this);
		} catch (NullPointerException npe) {
		} catch (ScriptException ex) {
		    ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
		    ex.printStackTrace();
		}
	    }
	}, delay);
    }

    public final String getName() {
	return name;
    }

    public final void setProperty(final String key, final String value) {
	props.setProperty(key, value);
    }

    public final Object setProperty(final String key, final String value, final boolean prev) {
	return props.setProperty(key, value);
    }

    public final String getProperty(final String key) {
	return props.getProperty(key);
    }

    public final void leftParty(final GameCharacter chr) {
	try {
	    em.getIv().invokeFunction("leftParty", this, chr);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public final void disbandParty() {
	try {
	    em.getIv().invokeFunction("disbandParty", this);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    //Separate function to warp players to a "finish" map, if applicable
    public final void finishPQ() {
	try {
	    em.getIv().invokeFunction("clearPQ", this);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public final void removePlayer(final GameCharacter chr) {
	try {
	    em.getIv().invokeFunction("playerExit", this, chr);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public final void registerCarnivalParty(final GameCharacter leader, final GameMap map, final byte team) {
	leader.clearCarnivalRequests();
	List<GameCharacter> characters = new LinkedList<GameCharacter>();
	final Party party = leader.getParty();

	if (party == null) {
	    return;
	}
	for (PartyMember pc : party.getMembers()) {
	    final GameCharacter c = map.getCharacterById_InMap(pc.getId());
	    characters.add(c);
	    registerPlayer(c);
	    c.resetCP();
	}
	final CarnivalParty carnivalParty = new CarnivalParty(leader, characters, team);
	try {
	    em.getIv().invokeFunction("registerCarnivalParty", this, carnivalParty);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    ex.printStackTrace();
	}
    }

    public void onMapLoad(final GameCharacter chr) {
	try {
	    em.getIv().invokeFunction("onMapLoad", this, chr);
	} catch (ScriptException ex) {
	    ex.printStackTrace();
	} catch (NoSuchMethodException ex) {
	    // Ignore, we don't want to update this for all events.
	}
    }

    public boolean isLeader(final GameCharacter chr) {
	return (chr.getParty().getLeader().getId() == chr.getId());
    }

    public void registerSquad(Squad squad, GameMap map) {
	final int mapid = map.getId();

	for (GameCharacter player : squad.getMembers()) {
	    if (player != null && player.getMapId() == mapid) {
		registerPlayer(player);
	    }
	}
    }
}
