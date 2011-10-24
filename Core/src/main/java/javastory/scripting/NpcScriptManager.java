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

import java.util.Map;
import java.util.WeakHashMap;

import javastory.channel.ChannelClient;
import javastory.game.quest.QuestInfoProvider;
import javastory.game.quest.QuestInfoProvider.QuestInfo;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public final class NpcScriptManager extends AbstractScriptManager {

	private final Map<ChannelClient, NpcConversationManager> managers;
	private final Map<ChannelClient, Invocable> scripts;
	private static final NpcScriptManager instance = new NpcScriptManager();

	private NpcScriptManager() {
		this.managers = new WeakHashMap<>();
		this.scripts = new WeakHashMap<>();
	}

	public static NpcScriptManager getInstance() {
		return instance;
	}

	public final void start(final ChannelClient c, final int npc) {
		try {
			if (!(managers.containsKey(c) && scripts.containsKey(c))) {
				final Invocable iv = getInvocable("npc/" + npc + ".js", c);
				final ScriptEngine scriptengine = (ScriptEngine) iv;
				if (iv == null) {
					return;
				}
				final NpcConversationManager cm = new NpcConversationManager(c, npc, -1, (byte) -1);
				managers.put(c, cm);
				scriptengine.put("cm", cm);

				c.getPlayer().setConversationState(1);

				scripts.put(c, iv);

				try {
					iv.invokeFunction("start");
					// Temporary until I've removed all of start
				} catch (NoSuchMethodException nsme) {
					iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
				}
			}
		} catch (final ScriptException | NoSuchMethodException e) {
			e.printStackTrace();
			System.err.println("Error executing NPC script, NPC ID : " + npc + "." + e);
			dispose(c);
		}
	}

	public final void action(final ChannelClient c, final byte mode, final byte type, final int selection) {
		if (mode != -1) {
			try {
				if (managers.get(c).isPendingDisposal()) {
					dispose(c);
				} else {
					scripts.get(c).invokeFunction("action", mode, type, selection);
				}
			} catch (final ScriptException | NoSuchMethodException e) {
				e.printStackTrace();
				System.err.println("Error executing NPC script");
				dispose(c);
			}
		}
	}

	public final void startQuest(final ChannelClient c, final int npc, final int quest) {
		if (!QuestInfoProvider.getInfo(quest).canStart(c.getPlayer(), npc)) {
			return;
		}
		try {
			if (!(managers.containsKey(c) && scripts.containsKey(c))) {
				final Invocable iv = getInvocable("quest/" + quest + ".js", c);
				final ScriptEngine scriptengine = (ScriptEngine) iv;
				if (iv == null) {
					return;
				}
				final NpcConversationManager cm = new NpcConversationManager(c, npc, quest, (byte) 0);
				managers.put(c, cm);
				scriptengine.put("qm", cm);

				c.getPlayer().setConversationState(1);

				scripts.put(c, iv);

				iv.invokeFunction("start", (byte) 1, (byte) 0, 0); // start it
																	// off as
																	// something
			}
		} catch (final Exception e) {
			System.err.println("Error executing Quest script. (" + quest + ")" + e);
			dispose(c);
		}
	}

	public final void startQuest(final ChannelClient c, final byte mode, final byte type, final int selection) {
		try {
			if (managers.get(c).isPendingDisposal()) {
				dispose(c);
			} else {
				scripts.get(c).invokeFunction("start", mode, type, selection);
			}
		} catch (ScriptException | NoSuchMethodException e) {
			dispose(c);
		}
	}

	public final void endQuest(final ChannelClient c, final int npc, final int quest, final boolean customEnd) {
		QuestInfo info = QuestInfoProvider.getInfo(quest);
		boolean canComplete = info.canComplete(c.getPlayer(), npc);
		if (!customEnd && canComplete) {
			return;
		}
		try {
			if (!(managers.containsKey(c) && scripts.containsKey(c))) {
				final Invocable iv = getInvocable("quest/" + quest + ".js", c);
				final ScriptEngine scriptengine = (ScriptEngine) iv;
				if (iv == null) {
					return;
				}
				final NpcConversationManager cm = new NpcConversationManager(c, npc, quest, (byte) 1);
				managers.put(c, cm);
				scriptengine.put("qm", cm);

				c.getPlayer().setConversationState(1);

				scripts.put(c, iv);

				iv.invokeFunction("end", (byte) 1, (byte) 0, 0); // start it off
																	// as
																	// something
			}
		} catch (ScriptException | NoSuchMethodException e) {
			System.err.println("Error executing Quest script. (" + quest + ")" + e);
		} finally {
			dispose(c);
		}
	}

	public final void endQuest(final ChannelClient c, final byte mode, final byte type, final int selection) {
		try {
			if (managers.get(c).isPendingDisposal()) {
				dispose(c);
			} else {
				scripts.get(c).invokeFunction("end", mode, type, selection);
			}
		} catch (ScriptException | NoSuchMethodException e) {
			// System.err.println("Error executing Quest script. (" +
			// c.getQM().getQuestId() + ")" + e);
			dispose(c);
		}
	}

	public final void dispose(final ChannelClient c) {
		final NpcConversationManager manager = managers.get(c);
		if (manager != null) {
			managers.remove(manager.getClient());
			scripts.remove(manager.getClient());

			if (manager.getType() == -1) {
				c.removeScriptEngine("scripts/npc/" + manager.getNpcId() + ".js");
			} else {
				c.removeScriptEngine("scripts/quest/" + manager.getQuest() + ".js");
			}
		}
		if (c.getPlayer().getConversationState() == 1) {
			c.getPlayer().setConversationState(0);
		}
	}

	public final NpcConversationManager getConversationManager(final ChannelClient client) {
		return managers.get(client);
	}
}
