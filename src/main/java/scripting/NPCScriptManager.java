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

import java.util.Map;
import java.util.WeakHashMap;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import client.MapleClient;
import server.quest.MapleQuest;

public class NPCScriptManager extends AbstractScriptManager {

    private final Map<MapleClient, NPCConversationManager> cms = new WeakHashMap<MapleClient, NPCConversationManager>();
    private final Map<MapleClient, Invocable> scripts = new WeakHashMap<MapleClient, Invocable>();
    private static final NPCScriptManager instance = new NPCScriptManager();

    public static final NPCScriptManager getInstance() {
	return instance;
    }

    public final void start(final MapleClient c, final int npc) {
	try {
	    if (!(cms.containsKey(c) && scripts.containsKey(c))) {
		final Invocable iv = getInvocable("npc/" + npc + ".js", c);
		final ScriptEngine scriptengine = (ScriptEngine) iv;
		if (iv == null) {
		    return;
		}
		final NPCConversationManager cm = new NPCConversationManager(c, npc, -1, (byte) -1);
		cms.put(c, cm);
		scriptengine.put("cm", cm);

		c.getPlayer().setConversation(1);

		scripts.put(c, iv);

		try {
		    iv.invokeFunction("start"); // Temporary until I've removed all of start
		} catch (NoSuchMethodException nsme) {
		    iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
		}
	    }
	} catch (final Exception e) {
	    e.printStackTrace();
	    System.err.println("Error executing NPC script, NPC ID : " + npc + "." + e);
	    dispose(c);
	}
    }

    public final void action(final MapleClient c, final byte mode, final byte type, final int selection) {
	if (mode != -1) {
	    try {
		if (cms.get(c).pendingDisposal) {
		    dispose(c);
		} else {
		    scripts.get(c).invokeFunction("action", mode, type, selection);
		}
	    } catch (final Exception e) {
		e.printStackTrace();
		System.err.println("Error executing NPC script");
		dispose(c);
	    }
	}
    }

    public final void startQuest(final MapleClient c, final int npc, final int quest) {
	if (!MapleQuest.getInstance(quest).canStart(c.getPlayer(), null)) {
	    return;
	}
	try {
	    if (!(cms.containsKey(c) && scripts.containsKey(c))) {
		final Invocable iv = getInvocable("quest/" + quest + ".js", c);
		final ScriptEngine scriptengine = (ScriptEngine) iv;
		if (iv == null) {
		    return;
		}
		final NPCConversationManager cm = new NPCConversationManager(c, npc, quest, (byte) 0);
		cms.put(c, cm);
		scriptengine.put("qm", cm);

		c.getPlayer().setConversation(1);

		scripts.put(c, iv);

		iv.invokeFunction("start", (byte) 1, (byte) 0, 0); // start it off as something
	    }
	} catch (final Exception e) {
	    System.err.println("Error executing Quest script. (" + quest + ")" + e);
	    dispose(c);
	}
    }

    public final void startQuest(final MapleClient c, final byte mode, final byte type, final int selection) {
	try {
	    if (cms.get(c).pendingDisposal) {
		dispose(c);
	    } else {
		scripts.get(c).invokeFunction("start", mode, type, selection);
	    }
	} catch (Exception e) {
//		System.err.println("Error executing Quest script. (" + c.getQM().getQuestId() + ")" + e);
	    dispose(c);
	}
    }

    public final void endQuest(final MapleClient c, final int npc, final int quest, final boolean customEnd) {
	if (!customEnd && !MapleQuest.getInstance(quest).canComplete(c.getPlayer(), null)) {
	    return;
	}
	try {
	    if (!(cms.containsKey(c) && scripts.containsKey(c))) {
		final Invocable iv = getInvocable("quest/" + quest + ".js", c);
		final ScriptEngine scriptengine = (ScriptEngine) iv;
		if (iv == null) {
		    dispose(c);
		    return;
		}
		final NPCConversationManager cm = new NPCConversationManager(c, npc, quest, (byte) 1);
		cms.put(c, cm);
		scriptengine.put("qm", cm);

		c.getPlayer().setConversation(1);

		scripts.put(c, iv);

		iv.invokeFunction("end", (byte) 1, (byte) 0, 0); // start it off as something
	    }
	} catch (Exception e) {
	    System.err.println("Error executing Quest script. (" + quest + ")" + e);
	    dispose(c);
	}
    }

    public final void endQuest(final MapleClient c, final byte mode, final byte type, final int selection) {
	try {
	    if (cms.get(c).pendingDisposal) {
		dispose(c);
	    } else {
		scripts.get(c).invokeFunction("end", mode, type, selection);
	    }
	} catch (Exception e) {
//		System.err.println("Error executing Quest script. (" + c.getQM().getQuestId() + ")" + e);
	    dispose(c);
	}
    }

    public final void dispose(final MapleClient c) {
	final NPCConversationManager npccm = cms.get(c);
	if (npccm != null) {
	    cms.remove(npccm.getC());
	    scripts.remove(npccm.getC());

	    if (npccm.getType() == -1) {
		c.removeScriptEngine("scripts/npc/" + npccm.getNpc() + ".js");
	    } else {
		c.removeScriptEngine("scripts/quest/" + npccm.getQuest() + ".js");
	    }
	}
	if (c.getPlayer().getConversation() == 1) {
	    c.getPlayer().setConversation(0);
	}
    }

    public final NPCConversationManager getCM(final MapleClient c) {
	return cms.get(c);
    }
}
