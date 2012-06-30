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

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.life.OverrideMonsterStats;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.maps.GameMapObject;
import javastory.channel.server.Squad;
import javastory.server.TimerManager;
import javastory.tools.packets.ChannelPackets;

import javax.script.Invocable;
import javax.script.ScriptException;

import com.google.common.collect.MapMaker;

public class EventManager {

	private final Invocable invocable;
	private final ConcurrentMap<String, EventInstanceManager> instances = new MapMaker().weakValues().makeMap();
	private final Properties props = new Properties();
	private final String name;

	public EventManager(final Invocable invocable, final String name) {
		this.invocable = invocable;
		this.name = name;
	}

	public void cancel() {
		try {
			this.invocable.invokeFunction("cancelSchedule", (Object) null);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void schedule(final String methodName, final long delay) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					EventManager.this.invocable.invokeFunction(methodName, (Object) null);
				} catch (final ScriptException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				} catch (final NoSuchMethodException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				}
			}
		}, delay);
	}

	public void schedule(final String methodName, final long delay, final EventInstanceManager eim) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					EventManager.this.invocable.invokeFunction(methodName, eim);
				} catch (final ScriptException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				} catch (final NoSuchMethodException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				}
			}
		}, delay);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, final long timestamp) {
		return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {

			@Override
			public void run() {
				try {
					EventManager.this.invocable.invokeFunction(methodName, (Object) null);
				} catch (final ScriptException ex) {
					ex.printStackTrace();
				} catch (final NoSuchMethodException ex) {
					ex.printStackTrace();
				}
			}
		}, timestamp);
	}

	public EventInstanceManager getInstance(final String name) {
		return this.instances.get(name);
	}

	public Collection<EventInstanceManager> getInstances() {
		return Collections.unmodifiableCollection(this.instances.values());
	}

	public EventInstanceManager newInstance(final String name) {
		final EventInstanceManager ret = new EventInstanceManager(this, name, ChannelServer.getMapFactory());
		this.instances.put(name, ret);
		return ret;
	}

	public void disposeInstance(final String name) {
		this.instances.remove(name);
	}

	public Invocable getInvocable() {
		return this.invocable;
	}

	public void setProperty(final String key, final String value) {
		this.props.setProperty(key, value);
	}

	public String getProperty(final String key) {
		return this.props.getProperty(key);
	}

	public String getName() {
		return this.name;
	}

	public void startInstance() {
		try {
			this.invocable.invokeFunction("setup", (Object) null);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void startInstance(final ChannelCharacter character) {
		try {
			final EventInstanceManager eim = (EventInstanceManager) this.invocable.invokeFunction("setup", (Object) null);
			eim.registerPlayer(character);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	// PQ method: starts a PQ
	public void startInstance(final Party party, final GameMap map) {
		try {
			final EventInstanceManager eim = (EventInstanceManager) this.invocable.invokeFunction("setup", (Object) null);
			eim.registerParty(party, map);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	// non-PQ method for starting instance
	public void startInstance(final EventInstanceManager eim, final String leader) {
		try {
			this.invocable.invokeFunction("setup", eim);
			eim.setProperty("leader", leader);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void startInstance(final Squad squad, final GameMap map) {
		try {
			final EventInstanceManager eim = (EventInstanceManager) this.invocable.invokeFunction("setup", squad.getLeader().getId());
			eim.registerSquad(squad, map);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void warpAllPlayer(final int from, final int to) {
		final GameMap tomap = ChannelServer.getMapFactory().getMap(to);
		for (final GameMapObject mmo : ChannelServer.getMapFactory().getMap(from).getAllPlayer()) {
			((ChannelCharacter) mmo).changeMap(tomap, tomap.getPortal(0));
		}
	}

	public GameMapFactory getMapFactory() {
		return ChannelServer.getMapFactory();
	}

	public OverrideMonsterStats newMonsterStats() {
		return new OverrideMonsterStats();
	}

	public Monster getMonster(final int id) {
		return LifeFactory.getMonster(id);
	}

	public void broadcastShip(final int mapid, final int effect) {
		ChannelServer.getMapFactory().getMap(mapid).broadcastMessage(ChannelPackets.boatPacket(effect));
	}

	public void broadcastServerMsg(final int type, final String msg, final boolean weather) {
		if (!weather) {
			ChannelServer.getInstance().broadcastPacket(ChannelPackets.serverNotice(type, msg));
		} else {
			for (final Entry<Integer, GameMap> map : ChannelServer.getMapFactory().getMaps().entrySet()) {
				final GameMap load = map.getValue();
				if (load.getCharacterCount() > 0) {
					load.startMapEffect(msg, type);
				}
			}
		}
	}
}
