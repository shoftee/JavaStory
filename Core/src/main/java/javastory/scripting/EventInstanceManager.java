/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.scripting;

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
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.server.CarnivalParty;
import javastory.channel.server.Squad;
import javastory.game.quest.QuestStatus;
import javastory.server.TimerManager;
import javastory.tools.packets.ChannelPackets;

import javax.script.ScriptException;

public class EventInstanceManager {

	private List<ChannelCharacter> chars = new LinkedList<>();
	private List<Monster> mobs = new LinkedList<>();
	private Map<ChannelCharacter, Integer> killCount = new HashMap<>();
	private EventManager eventManager;
	private GameMapFactory mapFactory;
	private final String name;
	private Properties props = new Properties();
	private long timeStarted = 0;
	private long eventTime = 0;
	private List<Integer> mapIds = new LinkedList<>();
	private ScheduledFuture<?> eventTimer;
	private final Lock mutex = new ReentrantLock();

	public EventInstanceManager(final EventManager em, final String name, final GameMapFactory factory) {
		this.eventManager = em;
		this.name = name;
		this.mapFactory = factory;
	}

	public void registerPlayer(final ChannelCharacter chr) {
		try {
			this.mutex.lock();
			try {
				this.chars.add(chr);
			} finally {
				this.mutex.unlock();
			}
			chr.setEventInstance(this);
			this.eventManager.getInvocable().invokeFunction("playerEntry", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void changedMap(final ChannelCharacter chr, final int mapid) {
		try {
			this.eventManager.getInvocable().invokeFunction("changedMap", this, chr, mapid);
		} catch (final NullPointerException npe) {
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void timeOut(final long delay, final EventInstanceManager eim) {
		this.eventTimer = TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					EventInstanceManager.this.eventManager.getInvocable().invokeFunction("scheduledTimeout", eim);
				} catch (final NullPointerException npe) {
				} catch (final ScriptException ex) {
					ex.printStackTrace();
				} catch (final NoSuchMethodException ex) {
					ex.printStackTrace();
				}
			}
		}, delay);
	}

	public void stopEventTimer() {
		this.eventTime = 0;
		this.timeStarted = 0;
		if (this.eventTimer != null) {
			this.eventTimer.cancel(false);
		}
	}

	public void restartEventTimer(final long time) {
		this.timeStarted = System.currentTimeMillis();
		this.eventTime = time;
		if (this.eventTimer != null) {
			this.eventTimer.cancel(false);
		}
		this.eventTimer = null;
		final int timesend = (int) time / 1000;

		this.mutex.lock();
		try {
			for (final ChannelCharacter chr : this.chars) {
				chr.getClient().write(ChannelPackets.getClock(timesend));
			}
		} finally {
			this.mutex.unlock();
		}
		this.timeOut(time, this);
	}

	public void startEventTimer(final long time) {
		this.timeStarted = System.currentTimeMillis();
		this.eventTime = time;
		final int timesend = (int) time / 1000;

		this.mutex.lock();
		try {
			for (final ChannelCharacter chr : this.chars) {
				chr.getClient().write(ChannelPackets.getClock(timesend));
			}
		} finally {
			this.mutex.unlock();
		}
		this.timeOut(time, this);
	}

	public boolean isTimerStarted() {
		return this.eventTime > 0 && this.timeStarted > 0;
	}

	public long getTimeLeft() {
		return this.eventTime - (System.currentTimeMillis() - this.timeStarted);
	}

	public void registerParty(final Party party, final GameMap map) {
		for (final PartyMember pc : party.getMembers()) {
			final ChannelCharacter c = map.getCharacterById_InMap(pc.getCharacterId());
			this.registerPlayer(c);
		}
	}

	public void unregisterPlayer(final ChannelCharacter chr) {
		this.mutex.lock();
		try {
			this.chars.remove(chr);
		} finally {
			this.mutex.unlock();
		}
		chr.setEventInstance(null);
	}

	public final boolean disposeIfPlayerBelow(final byte size, final int towarp) {
		GameMap map = null;
		if (towarp != 0) {
			map = this.getMapFactory().getMap(towarp);
		}
		this.mutex.lock();
		try {
			if (this.chars.size() <= size) {

				ChannelCharacter chr;
				for (int i = 0; i < this.chars.size(); i++) {
					chr = this.chars.get(i);
					this.unregisterPlayer(chr);

					if (towarp != 0) {
						chr.changeMap(map, map.getPortal(0));
					}
				}
				this.dispose();
				return true;
			}
		} finally {
			this.mutex.unlock();
		}
		return false;
	}

	public final void saveBossQuest(final int points) {
		this.mutex.lock();
		try {
			for (final ChannelCharacter chr : this.chars) {
				final QuestStatus record = chr.getQuestStatus(150001);
				final String customData = record.getCustomData();

				if (customData != null) {
					record.setCustomData(String.valueOf(points + Integer.parseInt(customData)));
				} else {
					record.setCustomData(String.valueOf(points)); // First time
				}
			}
		} finally {
			this.mutex.unlock();
		}
	}

	public List<ChannelCharacter> getPlayers() {
		return Collections.unmodifiableList(this.chars);
	}

	public final int getPlayerCount() {
		return this.chars.size();
	}

	public void registerMonster(final Monster mob) {
		this.mobs.add(mob);
		mob.setEventInstance(this);
	}

	public void unregisterMonster(final Monster mob) {
		this.mobs.remove(mob);
		mob.setEventInstance(null);
		if (this.mobs.isEmpty()) {
			try {
				this.eventManager.getInvocable().invokeFunction("allMonstersDead", this);
			} catch (ScriptException | NoSuchMethodException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void playerKilled(final ChannelCharacter chr) {
		try {
			this.eventManager.getInvocable().invokeFunction("playerDead", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public boolean revivePlayer(final ChannelCharacter chr) {
		try {
			final Object b = this.eventManager.getInvocable().invokeFunction("playerRevive", this, chr);
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
			byte ret = ((Double) this.eventManager.getInvocable().invokeFunction("playerDisconnected", this, chr)).byteValue();

			if (ret == 0) {
				this.unregisterPlayer(chr);
				if (this.getPlayerCount() <= 0) {
					this.dispose();
				}
			} else {
				this.mutex.lock();
				try {
					if (ret > 0) {
						this.unregisterPlayer(chr);
						if (this.getPlayerCount() < ret) {
							for (final ChannelCharacter player : this.chars) {
								this.removePlayer(player);
							}
							this.dispose();
						}
					} else {
						this.unregisterPlayer(chr);
						ret *= -1;

						if (this.isLeader(chr)) {
							for (final ChannelCharacter player : this.chars) {
								this.removePlayer(player);
							}
							this.dispose();
						} else {
							if (this.getPlayerCount() < ret) {
								for (final ChannelCharacter player : this.chars) {
									this.removePlayer(player);
								}
								this.dispose();
							}
						}
					}
				} finally {
					this.mutex.unlock();
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
			Integer kc = this.killCount.get(chr);
			final int inc = ((Double) this.eventManager.getInvocable().invokeFunction("monsterValue", this, mob.getId())).intValue();
			if (kc == null) {
				kc = inc;
			} else {
				kc += inc;
			}
			this.killCount.put(chr, kc);
			if (chr.getCarnivalParty() != null) {
				this.eventManager.getInvocable().invokeFunction("monsterKilled", this, chr, mob.getStats().getCP());
			}
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public int getKillCount(final ChannelCharacter chr) {
		final Integer kc = this.killCount.get(chr);
		if (kc == null) {
			return 0;
		} else {
			return kc;
		}
	}

	public void dispose() {
		this.mutex.lock();
		try {
			this.chars.clear();
			this.chars = null;
		} finally {
			this.mutex.unlock();
		}
		this.mobs.clear();
		this.mobs = null;
		this.killCount.clear();
		this.killCount = null;
		this.timeStarted = 0;
		this.eventTime = 0;
		this.props.clear();
		this.props = null;
		for (final Integer i : this.mapIds) {
			this.mapFactory.removeInstanceMap(i);
		}
		this.mapIds = null;
		this.mapFactory = null;
		this.eventManager.disposeInstance(this.name);
		this.eventManager = null;
	}

	public final void broadcastPlayerMsg(final int type, final String msg) {
		this.mutex.lock();
		try {
			for (final ChannelCharacter chr : this.chars) {
				chr.getClient().write(ChannelPackets.serverNotice(type, msg));
			}
		} finally {
			this.mutex.unlock();
		}
	}

	public final GameMap createInstanceMap(final int mapid) {
		final int assignedid = ChannelServer.getInstance().getEventSM().getNewInstanceMapId();
		this.mapIds.add(assignedid);
		return this.mapFactory.createInstanceMap(mapid, true, true, true, assignedid);
	}

	public final GameMap createInstanceMapS(final int mapid) {
		final int assignedid = ChannelServer.getInstance().getEventSM().getNewInstanceMapId();
		this.mapIds.add(assignedid);
		return this.mapFactory.createInstanceMap(mapid, false, false, false, assignedid);
	}

	public final GameMapFactory getMapFactory() {
		return this.mapFactory;
	}

	public final GameMap getMapInstance(final int args) {
		final GameMap map = this.mapFactory.getInstanceMap(this.mapIds.get(args));

		// in case reactors need shuffling and we are actually loading the map
		if (!this.mapFactory.isInstanceMapLoaded(this.mapIds.get(args))) {
			if (this.eventManager.getProperty("shuffleReactors") != null && this.eventManager.getProperty("shuffleReactors").equals("true")) {
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
					EventInstanceManager.this.eventManager.getInvocable().invokeFunction(methodName, EventInstanceManager.this);
				} catch (final NullPointerException npe) {
				} catch (final ScriptException ex) {
					ex.printStackTrace();
				} catch (final NoSuchMethodException ex) {
					ex.printStackTrace();
				}
			}
		}, delay);
	}

	public final String getName() {
		return this.name;
	}

	public final void setProperty(final String key, final String value) {
		this.props.setProperty(key, value);
	}

	public final Object setProperty(final String key, final String value, final boolean prev) {
		return this.props.setProperty(key, value);
	}

	public final String getProperty(final String key) {
		return this.props.getProperty(key);
	}

	public final void leftParty(final ChannelCharacter chr) {
		try {
			this.eventManager.getInvocable().invokeFunction("leftParty", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public final void disbandParty() {
		try {
			this.eventManager.getInvocable().invokeFunction("disbandParty", this);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	//Separate function to warp players to a "finish" map, if applicable
	public final void finishPQ() {
		try {
			this.eventManager.getInvocable().invokeFunction("clearPQ", this);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public final void removePlayer(final ChannelCharacter chr) {
		try {
			this.eventManager.getInvocable().invokeFunction("playerExit", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public final void registerCarnivalParty(final ChannelCharacter leader, final GameMap map, final byte team) {
		leader.clearCarnivalRequests();
		final List<ChannelCharacter> characters = new LinkedList<>();
		final Party party = leader.getParty();

		if (party == null) {
			return;
		}
		for (final PartyMember pc : party.getMembers()) {
			final ChannelCharacter c = map.getCharacterById_InMap(pc.getCharacterId());
			characters.add(c);
			this.registerPlayer(c);
			c.resetCP();
		}
		final CarnivalParty carnivalParty = new CarnivalParty(leader, characters, team);
		try {
			this.eventManager.getInvocable().invokeFunction("registerCarnivalParty", this, carnivalParty);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void onMapLoad(final ChannelCharacter chr) {
		try {
			this.eventManager.getInvocable().invokeFunction("onMapLoad", this, chr);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			// Ignore, we don't want to update this for all events.
		}
	}

	public boolean isLeader(final ChannelCharacter chr) {
		return chr.getParty().getLeader().getCharacterId() == chr.getId();
	}

	public void registerSquad(final Squad squad, final GameMap map) {
		final int mapid = map.getId();

		for (final ChannelCharacter player : squad.getMembers()) {
			if (player != null && player.getMapId() == mapid) {
				this.registerPlayer(player);
			}
		}
	}
}
