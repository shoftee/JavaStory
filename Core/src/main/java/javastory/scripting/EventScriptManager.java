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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javastory.channel.ChannelServer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * 
 * @author Matze
 */
public class EventScriptManager extends AbstractScriptManager {

	private class EventEntry {

		public String script;
		public Invocable invocable;
		public EventManager eventManager;

		public EventEntry(
				final String script,
				final Invocable invocable,
				final EventManager eventManager) {
			this.script = script;
			this.invocable = invocable;
			this.eventManager = eventManager;
		}

	}

	private final Map<String, EventEntry> events = new LinkedHashMap<String, EventEntry>();
	private final AtomicInteger runningInstanceMapId = new AtomicInteger(0);

	public final int getNewInstanceMapId() {
		return runningInstanceMapId.addAndGet(1);
	}

	public EventScriptManager(final ChannelServer cserv,
			final String[] scripts, int world) {
		super();
		for (final String script : scripts) {
			if (!script.equals("")) {
				final String path = "event/" + script + ".js";
				final Invocable invocable = getInvocable(path, null);

				if (invocable != null) {
					final EventManager eventManager =
							new EventManager(cserv, invocable, script, world);
					final EventEntry eventEntry =
							new EventEntry(script, invocable, eventManager);
					events.put(script, eventEntry);
				}
			}
		}
	}

	public final EventManager getEventManager(final String event) {
		final EventEntry entry = events.get(event);
		if (entry == null) {
			return null;
		}
		return entry.eventManager;
	}

	public final void init() {
		for (final EventEntry entry : events.values()) {
			try {
				((ScriptEngine) entry.invocable).put("em", entry.eventManager);
				entry.invocable.invokeFunction("init", (Object) null);
			} catch (final ScriptException ex) {
				ex.printStackTrace();
			} catch (final NoSuchMethodException ex) {
				ex.printStackTrace();
			}
		}
	}

	public final void cancel() {
		for (final EventEntry entry : events.values()) {
			entry.eventManager.cancel();
		}
	}
}
