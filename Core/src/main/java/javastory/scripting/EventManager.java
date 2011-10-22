/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.scripting;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.WeakHashMap;
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

public class EventManager {

	private Invocable invocable;
	private WeakHashMap<String, EventInstanceManager> instances = new WeakHashMap<>();
	private Properties props = new Properties();
	private String name;

	public EventManager(Invocable invocable, String name) {
		this.invocable = invocable;
		this.name = name;
	}

	public void cancel() {
		try {
			invocable.invokeFunction("cancelSchedule", (Object) null);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void schedule(final String methodName, long delay) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					invocable.invokeFunction(methodName, (Object) null);
				} catch (ScriptException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				} catch (NoSuchMethodException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				}
			}
		}, delay);
	}

	public void schedule(final String methodName, long delay,
			final EventInstanceManager eim) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					invocable.invokeFunction(methodName, eim);
				} catch (ScriptException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				} catch (NoSuchMethodException ex) {
					ex.printStackTrace();
					System.out.println("method Name : " + methodName + "");
				}
			}
		}, delay);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(final String methodName,
			long timestamp) {
		return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {

			@Override
			public void run() {
				try {
					invocable.invokeFunction(methodName, (Object) null);
				} catch (ScriptException ex) {
					ex.printStackTrace();
				} catch (NoSuchMethodException ex) {
					ex.printStackTrace();
				}
			}
		}, timestamp);
	}

	public EventInstanceManager getInstance(String name) {
		return instances.get(name);
	}

	public Collection<EventInstanceManager> getInstances() {
		return Collections.unmodifiableCollection(instances.values());
	}

	public EventInstanceManager newInstance(String name) {
		EventInstanceManager ret = new EventInstanceManager(this, name, ChannelServer.getMapFactory());
		instances.put(name, ret);
		return ret;
	}

	public void disposeInstance(String name) {
		instances.remove(name);
	}

	public Invocable getInvocable() {
		return invocable;
	}

	public void setProperty(String key, String value) {
		props.setProperty(key, value);
	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public String getName() {
		return name;
	}

	public void startInstance() {
		try {
			invocable.invokeFunction("setup", (Object) null);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void startInstance(ChannelCharacter character) {
		try {
			EventInstanceManager eim = (EventInstanceManager) (invocable
					.invokeFunction("setup", (Object) null));
			eim.registerPlayer(character);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	// PQ method: starts a PQ
	public void startInstance(Party party, GameMap map) {
		try {
			EventInstanceManager eim = (EventInstanceManager) (invocable
					.invokeFunction("setup", (Object) null));
			eim.registerParty(party, map);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	// non-PQ method for starting instance
	public void startInstance(EventInstanceManager eim, String leader) {
		try {
			invocable.invokeFunction("setup", eim);
			eim.setProperty("leader", leader);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void startInstance(Squad squad, GameMap map) {
		try {
			EventInstanceManager eim = (EventInstanceManager) (invocable
					.invokeFunction("setup", squad.getLeader().getId()));
			eim.registerSquad(squad, map);
		} catch (ScriptException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void warpAllPlayer(int from, int to) {
		final GameMap tomap = ChannelServer.getMapFactory().getMap(to);
		for (GameMapObject mmo : ChannelServer.getMapFactory().getMap(from)
				.getAllPlayer()) {
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
		ChannelServer.getMapFactory().getMap(mapid)
				.broadcastMessage(ChannelPackets.boatPacket(effect));
	}

	public void broadcastServerMsg(final int type, final String msg,
			final boolean weather) {
		if (!weather) {
			ChannelServer.getInstance().broadcastPacket(ChannelPackets.serverNotice(type, msg));
		} else {
			for (Entry<Integer, GameMap> map : ChannelServer.getMapFactory()
					.getMaps().entrySet()) {
				final GameMap load = map.getValue();
				if (load.getCharactersSize() > 0) {
					load.startMapEffect(msg, type);
				}
			}
		}
	}
}
