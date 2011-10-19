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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.ChannelCharacter;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.server.CarnivalParty;
import javastory.channel.server.Squad;

import javax.script.ScriptException;

import server.TimerManager;
import tools.MaplePacketCreator;
import client.QuestStatus;

public class EventInstanceManager {

    private List<ChannelCharacter> chars = new LinkedList<>();
    private List<Monster> mobs = new LinkedList<>();
    private Map<ChannelCharacter, Integer> killCount = new HashMap<>();
    private EventManager eventManager;
    private GameMapFactory mapFactory;
    private String name;
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private List<Integer> mapIds = new LinkedList<>();
    private ScheduledFuture<?> eventTimer;
    private final Lock mutex = new ReentrantLock();
    private int world;

    public EventInstanceManager(EventManager em, String name, GameMapFactory factory, int world) {
        this.eventManager = em;
        this.name = name;
        mapFactory = factory;
        this.world = world;
    }

    public void registerPlayer(ChannelCharacter chr) {
        try {
            mutex.lock();
            try {
                chars.add(chr);
            } finally {
                mutex.unlock();
            }
            chr.setEventInstance(this);
            eventManager.getInvocable().invokeFunction("playerEntry", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void changedMap(final ChannelCharacter chr, final int mapid) {
        try {
            eventManager.getInvocable().invokeFunction("changedMap", this, chr, mapid);
        } catch (NullPointerException npe) {
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void timeOut(final long delay, final EventInstanceManager eim) {
        eventTimer = TimerManager.getInstance().schedule(new Runnable() {

            @Override
			public void run() {
                try {
                    eventManager.getInvocable().invokeFunction("scheduledTimeout", eim);
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
            for (final ChannelCharacter chr : chars) {
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
            for (final ChannelCharacter chr : chars) {
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
            ChannelCharacter c = map.getCharacterById_InMap(pc.getCharacterId());
            registerPlayer(c);
        }
    }

    public void unregisterPlayer(final ChannelCharacter chr) {
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

                ChannelCharacter chr;
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
            for (ChannelCharacter chr : chars) {
                final QuestStatus record = chr.getQuestStatus(150001);
                final String customData = record.getCustomData();

                if (customData != null) {
                    record.setCustomData(String.valueOf(points +
                            Integer.parseInt(customData)));
                } else {
                    record.setCustomData(String.valueOf(points)); // First time
                }
            }
        } finally {
            mutex.unlock();
        }
    }

    public List<ChannelCharacter> getPlayers() {
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
        if (mobs.isEmpty()) {
            try {
                eventManager.getInvocable().invokeFunction("allMonstersDead", this);
            } catch (ScriptException | NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void playerKilled(ChannelCharacter chr) {
        try {
            eventManager.getInvocable().invokeFunction("playerDead", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public boolean revivePlayer(ChannelCharacter chr) {
        try {
            Object b = eventManager.getInvocable().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public void playerDisconnected(final ChannelCharacter chr) {
        try {
            byte ret = ((Double) eventManager.getInvocable().invokeFunction("playerDisconnected", this, chr)).byteValue();

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
                            for (ChannelCharacter player : chars) {
                                removePlayer(player);
                            }
                            dispose();
                        }
                    } else {
                        unregisterPlayer(chr);
                        ret *= -1;

                        if (isLeader(chr)) {
                            for (ChannelCharacter player : chars) {
                                removePlayer(player);
                            }
                            dispose();
                        } else {
                            if (getPlayerCount() < ret) {
                                for (ChannelCharacter player : chars) {
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
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(final ChannelCharacter chr, final Monster mob) {
        try {
            Integer kc = killCount.get(chr);
            int inc = ((Double) eventManager.getInvocable().invokeFunction("monsterValue", this, mob.getId())).intValue();
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr, kc);
            if (chr.getCarnivalParty() != null) {
                eventManager.getInvocable().invokeFunction("monsterKilled", this, chr, mob.getStats().getCP());
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public int getKillCount(ChannelCharacter chr) {
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
        eventManager.disposeInstance(name);
        eventManager = null;
    }

    public final void broadcastPlayerMsg(final int type, final String msg) {
        mutex.lock();
        try {
            for (final ChannelCharacter chr : chars) {
                chr.getClient().write(MaplePacketCreator.serverNotice(type, msg));
            }
        } finally {
            mutex.unlock();
        }
    }

    public final GameMap createInstanceMap(final int mapid) {
        int assignedid = eventManager.getChannelServer().getEventSM(world).getNewInstanceMapId();
        mapIds.add(assignedid);
        return mapFactory.CreateInstanceMap(mapid, true, true, true, assignedid);
    }

    public final GameMap createInstanceMapS(final int mapid) {
        final int assignedid = eventManager.getChannelServer().getEventSM(world).getNewInstanceMapId();
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
            if (eventManager.getProperty("shuffleReactors") != null &&
                    eventManager.getProperty("shuffleReactors").equals("true")) {
                map.shuffleReactors();
            }
        }
        return map;
    }

    public final void schedule(final String methodName, final long delay) {
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
			public void run() {
                try {
                    eventManager.getInvocable().invokeFunction(methodName, EventInstanceManager.this);
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

    public final void leftParty(final ChannelCharacter chr) {
        try {
            eventManager.getInvocable().invokeFunction("leftParty", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public final void disbandParty() {
        try {
            eventManager.getInvocable().invokeFunction("disbandParty", this);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    //Separate function to warp players to a "finish" map, if applicable
    public final void finishPQ() {
        try {
            eventManager.getInvocable().invokeFunction("clearPQ", this);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public final void removePlayer(final ChannelCharacter chr) {
        try {
            eventManager.getInvocable().invokeFunction("playerExit", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public final void registerCarnivalParty(final ChannelCharacter leader, final GameMap map, final byte team) {
        leader.clearCarnivalRequests();
        List<ChannelCharacter> characters = new LinkedList<>();
        final Party party = leader.getParty();

        if (party == null) {
            return;
        }
        for (PartyMember pc : party.getMembers()) {
            final ChannelCharacter c = map.getCharacterById_InMap(pc.getCharacterId());
            characters.add(c);
            registerPlayer(c);
            c.resetCP();
        }
        final CarnivalParty carnivalParty = new CarnivalParty(leader, characters, team);
        try {
            eventManager.getInvocable().invokeFunction("registerCarnivalParty", this, carnivalParty);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void onMapLoad(final ChannelCharacter chr) {
        try {
            eventManager.getInvocable().invokeFunction("onMapLoad", this, chr);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            // Ignore, we don't want to update this for all events.
        }
    }

    public boolean isLeader(final ChannelCharacter chr) {
        return (chr.getParty().getLeader().getCharacterId() == chr.getId());
    }

    public void registerSquad(Squad squad, GameMap map) {
        final int mapid = map.getId();

        for (ChannelCharacter player : squad.getMembers()) {
            if (player != null && player.getMapId() == mapid) {
                registerPlayer(player);
            }
        }
    }
}
