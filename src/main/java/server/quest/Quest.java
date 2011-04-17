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
package server.quest;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import client.GameCharacter;
import client.QuestStatus;
import scripting.NpcScriptManager;
import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataProviderFactory;
import provider.WzDataTool;
import tools.MaplePacketCreator;

public class Quest implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private static Map<Integer, Quest> quests = new LinkedHashMap<Integer, Quest>();
    protected int id;
    protected List<QuestRequirement> startReqs;
    protected List<QuestRequirement> completeReqs;
    protected List<QuestAction> startActs;
    protected List<QuestAction> completeActs;
    protected Map<Integer, Integer> relevantMobs;
    private boolean autoStart;
    private boolean autoPreComplete;
    private boolean repeatable = false, customend = false;
    private static final WzDataProvider questData = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/Quest.wz"));
    private static final WzData actions = questData.getData("Act.img");
    private static final WzData requirements = questData.getData("Check.img");
    private static final WzData info = questData.getData("QuestInfo.img");

    protected Quest() {
	relevantMobs = new LinkedHashMap<Integer, Integer>();
    }

    /** Creates a new instance of MapleQuest */
    private static boolean loadQuest(Quest ret, int id) {
	ret.id = id;
	ret.relevantMobs = new LinkedHashMap<Integer, Integer>();
	// read reqs
	final WzData basedata1 = requirements.getChildByPath(String.valueOf(id));
	final WzData basedata2 = actions.getChildByPath(String.valueOf(id));

	if (basedata1 == null && basedata2 == null) {
	    return false;
	}
	//-------------------------------------------------
	final WzData startReqData = basedata1.getChildByPath("0");
	ret.startReqs = new LinkedList<QuestRequirement>();
	if (startReqData != null) {
	    for (WzData startReq : startReqData.getChildren()) {
		final QuestRequirementType type = QuestRequirementType.getByWZName(startReq.getName());
		if (type.equals(QuestRequirementType.interval)) {
		    ret.repeatable = true;
		}
		final QuestRequirement req = new QuestRequirement(ret, type, startReq);
		if (req.getType().equals(QuestRequirementType.mob)) {
		    for (WzData mob : startReq.getChildren()) {
			ret.relevantMobs.put(
				WzDataTool.getInt(mob.getChildByPath("id")),
				WzDataTool.getInt(mob.getChildByPath("count"), 0));
		    }
		}
		ret.startReqs.add(req);
	    }

	}
	//-------------------------------------------------
	final WzData completeReqData = basedata1.getChildByPath("1");
	if (completeReqData.getChildByPath("endscript") != null) {
	    ret.customend = true;
	}
	ret.completeReqs = new LinkedList<QuestRequirement>();
	if (completeReqData != null) {
	    for (WzData completeReq : completeReqData.getChildren()) {
		QuestRequirement req = new QuestRequirement(ret, QuestRequirementType.getByWZName(completeReq.getName()), completeReq);
		if (req.getType().equals(QuestRequirementType.mob)) {
		    for (WzData mob : completeReq.getChildren()) {
			ret.relevantMobs.put(
				WzDataTool.getInt(mob.getChildByPath("id")),
				WzDataTool.getInt(mob.getChildByPath("count"), 0));
		    }
		}
		ret.completeReqs.add(req);
	    }
	}
	// read acts
	final WzData startActData = basedata2.getChildByPath("0");
	ret.startActs = new LinkedList<QuestAction>();
	if (startActData != null) {
	    for (WzData startAct : startActData.getChildren()) {
		ret.startActs.add(new QuestAction(QuestActionType.getByWZName(startAct.getName()), startAct, ret));
	    }
	}
	final WzData completeActData = basedata2.getChildByPath("1");
	ret.completeActs = new LinkedList<QuestAction>();

	if (completeActData != null) {
	    for (WzData completeAct : completeActData.getChildren()) {
		ret.completeActs.add(new QuestAction(QuestActionType.getByWZName(completeAct.getName()), completeAct, ret));
	    }
	}
	final WzData questInfo = info.getChildByPath(String.valueOf(id));
	ret.autoStart = WzDataTool.getInt("autoStart", questInfo, 0) == 1;
	ret.autoPreComplete = WzDataTool.getInt("autoPreComplete", questInfo, 0) == 1;

	return true;
    }

    public static Quest getInstance(int id) {
	Quest ret = quests.get(id);
	if (ret == null) {
	    ret = new Quest();
	    if (!loadQuest(ret, id)) {
		ret = new CustomQuest(id);
	    }
	    quests.put(id, ret);
	}
	return ret;
    }

    public boolean canStart(GameCharacter c, Integer npcid) {
	if (c.getQuest(this).getStatus() != 0 && !(c.getQuest(this).getStatus() == 2 && repeatable)) {
	    return false;
	}
	for (QuestRequirement r : startReqs) {
	    if (!r.check(c, npcid)) {
		return false;
	    }
	}
	return true;
    }

    public boolean canComplete(GameCharacter c, Integer npcid) {
	if (c.getQuest(this).getStatus() != 1) {
	    return false;
	}
	for (QuestRequirement r : completeReqs) {
	    if (!r.check(c, npcid)) {
		return false;
	    }
	}
	return true;
    }

    public final void RestoreLostItem(final GameCharacter c, final int itemid) {
	for (final QuestAction a : startActs) {
	    if (a.RestoreLostItem(c, itemid)) {
		break;
	    }
	}
    }

    public void start(GameCharacter c, int npc) {
	if ((autoStart || checkNPCOnMap(c, npc)) && canStart(c, npc)) {
	    for (QuestAction a : startActs) {
		a.runStart(c, null);
	    }
	    if (!customend) {
		final QuestStatus oldStatus = c.getQuest(this);
		final QuestStatus newStatus = new QuestStatus(this, (byte) 1, npc);
		newStatus.setCompletionTime(oldStatus.getCompletionTime());
		newStatus.setForfeited(oldStatus.getForfeited());
		c.updateQuest(newStatus);
	    } else {
		NpcScriptManager.getInstance().endQuest(c.getClient(), npc, getId(), true);
	    }
	}
    }

    public void complete(GameCharacter c, int npc) {
	complete(c, npc, null);
    }

    public void complete(GameCharacter c, int npc, Integer selection) {
	if ((autoPreComplete || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
	    for (QuestAction a : completeActs) {
		if (!a.checkEnd(c, selection)) {
		    return;
		}
	    }
	    for (QuestAction a : completeActs) {
		a.runEnd(c, selection);
	    }
	    // we save forfeits only for logging purposes, they shouldn't matter anymore
	    // completion time is set by the constructor
	    final QuestStatus newStatus = new QuestStatus(this, (byte) 2, npc);
	    newStatus.setForfeited(c.getQuest(this).getForfeited());
	    c.updateQuest(newStatus);

	    c.getClient().getSession().write(MaplePacketCreator.showSpecialEffect(9)); // Quest completion
	    c.getMap().broadcastMessage(c, MaplePacketCreator.showSpecialEffect(c.getId(), 9), false);
	}
    }

    public void forfeit(GameCharacter c) {
	if (c.getQuest(this).getStatus() != (byte) 1) {
	    return;
	}
	final QuestStatus oldStatus = c.getQuest(this);
	final QuestStatus newStatus = new QuestStatus(this, (byte) 0);
	newStatus.setForfeited(oldStatus.getForfeited() + 1);
	newStatus.setCompletionTime(oldStatus.getCompletionTime());
	c.updateQuest(newStatus);
    }

    public void forceStart(GameCharacter c, int npc, String customData) {
	final QuestStatus newStatus = new QuestStatus(this, (byte) 1, npc);
	newStatus.setForfeited(c.getQuest(this).getForfeited());
	newStatus.setCustomData(customData);
	c.updateQuest(newStatus);
    }

    public void forceComplete(GameCharacter c, int npc) {
	final QuestStatus newStatus = new QuestStatus(this, (byte) 2, npc);
	newStatus.setForfeited(c.getQuest(this).getForfeited());
	c.updateQuest(newStatus);
    }

    public int getId() {
	return id;
    }

    public Map<Integer, Integer> getRelevantMobs() {
	return relevantMobs;
    }

    private boolean checkNPCOnMap(GameCharacter player, int npcid) {
	return player.getMap().containsNPC(npcid) != -1;
    }
}
