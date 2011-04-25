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

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import client.ISkill;
import client.GameConstants;
import client.InventoryException;
import client.GameCharacter;
import client.Inventory;
import client.InventoryType;
import client.QuestStatus;
import client.Stat;
import client.SkillFactory;
import provider.WzData;
import provider.WzDataTool;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import org.javastory.tools.Randomizer;
import tools.MaplePacketCreator;

public class QuestAction implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private QuestActionType type;
    private WzData data;
    private Quest quest;

    /** Creates a new instance of MapleQuestAction */
    public QuestAction(QuestActionType type, WzData data, Quest quest) {
	this.type = type;
	this.data = data;
	this.quest = quest;
    }

    private static boolean canGetItem(WzData item, GameCharacter c) {
	if (item.getChildByPath("gender") != null) {
	    final int gender = WzDataTool.getInt(item.getChildByPath("gender"));
	    if (gender != 2 && gender != c.getGender()) {
		return false;
	    }
	}
	if (item.getChildByPath("job") != null) {
	    final int job = WzDataTool.getInt(item.getChildByPath("job"));
	    if (job < 100) {
		final int codec = getJobBy5ByteEncoding(job);
		if (codec / 100 != c.getJob() / 100) {
		    return false;
		}
	    } else if (job > 3000) {
		final int playerjob = c.getJob();
		final int codec = getJobByEncoding(job, playerjob);
		if (codec >= 1000) {
		    if (codec / 1000 != c.getJob() / 1000) {
			return false;
		    }
		} else {
		    if (codec / 100 != c.getJob() / 100) {
			return false;
		    }
		}
	    } else {
		if (job != c.getJob()) {
		    return false;
		}
	    }
	}
	return true;
    }

    public final boolean RestoreLostItem(final GameCharacter c, final int itemid) {
	if (type == QuestActionType.item) {
	    int retitem;

	    for (final WzData iEntry : data.getChildren()) {
		retitem = WzDataTool.getInt(iEntry.getChildByPath("id"), -1);
		if (retitem == itemid) {
		    if (!c.haveItem(retitem, 1, true, false)) {
			InventoryManipulator.addById(c.getClient(), retitem, (short) 1);
		    }
		    return true;
		}
	    }
	}
	return false;
    }

    public void runStart(GameCharacter c, Integer extSelection) {
	QuestStatus status;
	switch (type) {
	    case exp:
		status = c.getQuest(quest);
		if (status.getForfeited() > 0) {
		    break;
		}
		c.gainExp((WzDataTool.getInt(data, 0) * (c.getLevel() <= 10 ? 1 : c.getClient().getChannelServer().getExpRate())), true, true, true);
		break;
	    case item:
		// first check for randomness in item selection
		Map<Integer, Integer> props = new HashMap<Integer, Integer>();
		WzData prop;
		for (WzData iEntry : data.getChildren()) {
		    prop = iEntry.getChildByPath("prop");
		    if (prop != null && WzDataTool.getInt(prop) != -1 && canGetItem(iEntry, c)) {
			for (int i = 0; i < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
			    props.put(props.size(), WzDataTool.getInt(iEntry.getChildByPath("id")));
			}
		    }
		}
		int selection = 0;
		int extNum = 0;
		if (props.size() > 0) {
		    selection = props.get(Randomizer.nextInt(props.size()));
		}
		for (WzData iEntry : data.getChildren()) {
		    if (!canGetItem(iEntry, c)) {
			continue;
		    }
		    final int id = WzDataTool.getInt(iEntry.getChildByPath("id"), -1);
		    if (iEntry.getChildByPath("prop") != null) {
			if (WzDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
			    if (extSelection != extNum++) {
				continue;
			    }
			} else if (id != selection) {
			    continue;
			}
		    }
		    final short count = (short) WzDataTool.getInt(iEntry.getChildByPath("count"), 1);
		    if (count < 0) { // remove items
			try {
                            final Inventory inventory = c.getInventoryForItem(id);
			    InventoryManipulator.removeById(c.getClient(), inventory, id, (count * -1), true, false);
			} catch (InventoryException ie) {
			    // it's better to catch this here so we'll atleast try to remove the other items
			    System.err.println("[h4x] Completing a quest without meeting the requirements" + ie);
			}
			c.getClient().write(MaplePacketCreator.getShowItemGain(id, count, true));
		    } else { // add items
//			final int period = MapleDataTool.getInt(iEntry.getChildByPath("period"), 0);
			InventoryManipulator.addById(c.getClient(), id, count/*, "", -1, 0*/);
			c.getClient().write(MaplePacketCreator.getShowItemGain(id, count, true));
		    }
		}
		break;
//			case NEXTQUEST:
//				int nextquest = MapleDataTool.getInt(data);
//				Need to somehow make the chat popup for the next quest...
//				break;
	    case money:
		status = c.getQuest(quest);
		if (status.getForfeited() > 0) {
		    break;
		}
		c.gainMeso(WzDataTool.getInt(data, 0), true, false, true);
		break;
	    case QUEST:
		for (WzData qEntry : data) {
		    c.updateQuest(
			    new QuestStatus(Quest.getInstance(WzDataTool.getInt(qEntry.getChildByPath("id"))),
			    (byte) WzDataTool.getInt(qEntry.getChildByPath("state"), 0)));
		}
		break;
	    case skill:
		//TODO needs gain/lost message?
		for (WzData sEntry : data) {
		    final int skillid = WzDataTool.getInt(sEntry.getChildByPath("id"));
		    int skillLevel = WzDataTool.getInt(sEntry.getChildByPath("skillLevel"), 0);
		    int masterLevel = WzDataTool.getInt(sEntry.getChildByPath("masterLevel"), 0);
		    final ISkill skillObject = SkillFactory.getSkill(skillid);

		    for (WzData applicableJob : sEntry.getChildByPath("job")) {
			if (skillObject.isBeginnerSkill() || c.getJob() == WzDataTool.getInt(applicableJob)) {
			    c.changeSkillLevel(skillObject,
				    (byte) Math.max(skillLevel, c.getCurrentSkillLevel(skillObject)),
				    (byte) Math.max(masterLevel, c.getMasterSkillLevel(skillObject)));
			    break;
			}
		    }
		}
		break;
	    case pop:
		status = c.getQuest(quest);
		if (status.getForfeited() > 0) {
		    break;
		}
		final int fameGain = WzDataTool.getInt(data, 0);
		c.addFame(fameGain);
		c.updateSingleStat(Stat.FAME, c.getFame());
		c.getClient().write(MaplePacketCreator.getShowFameGain(fameGain));
		break;
	    case buffItemID:
		status = c.getQuest(quest);
		if (status.getForfeited() > 0) {
		    break;
		}
		final int tobuff = WzDataTool.getInt(data, -1);
		if (tobuff == -1) {
		    break;
		}
		ItemInfoProvider.getInstance().getItemEffect(tobuff).applyTo(c);
		break;
	    case infoNumber: {
//		System.out.println("quest : "+MapleDataTool.getInt(data, 0)+"");
//		MapleQuest.getInstance(MapleDataTool.getInt(data, 0)).forceComplete(c, 0);
//		break;
	    }
	    default:
		break;
	}
    }

    public boolean checkEnd(GameCharacter c, Integer extSelection) {
	switch (type) {
	    case item: {
		// first check for randomness in item selection
		final Map<Integer, Integer> props = new HashMap<Integer, Integer>();

		for (WzData iEntry : data.getChildren()) {
		    final WzData prop = iEntry.getChildByPath("prop");
		    if (prop != null && WzDataTool.getInt(prop) != -1 && canGetItem(iEntry, c)) {
			for (int i = 0; i < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
			    props.put(props.size(), WzDataTool.getInt(iEntry.getChildByPath("id")));
			}
		    }
		}
		int selection = 0;
		int extNum = 0;
		if (props.size() > 0) {
		    selection = props.get(Randomizer.nextInt(props.size()));
		}
		byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;

		for (WzData iEntry : data.getChildren()) {
		    if (!canGetItem(iEntry, c)) {
			continue;
		    }
		    final int id = WzDataTool.getInt(iEntry.getChildByPath("id"), -1);
		    if (iEntry.getChildByPath("prop") != null) {
			if (WzDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
			    if (extSelection != extNum++) {
				continue;
			    }
			} else if (id != selection) {
			    continue;
			}
		    }
		    final short count = (short) WzDataTool.getInt(iEntry.getChildByPath("count"), 1);
		    if (count < 0) { // remove items
			if (!c.haveItem(id, count, false, true)) {
			    c.sendNotice(1, "You are short of some item to complete quest.");
			    return false;
			}
		    } else { // add items
			switch (GameConstants.getInventoryType(id)) {
			    case EQUIP:
				eq++;
				break;
			    case USE:
				use++;
				break;
			    case SETUP:
				setup++;
				break;
			    case ETC:
				etc++;
				break;
			    case CASH:
				cash++;
				break;
			}
		    }
		}
		if (c.getEquipInventory().getNumFreeSlot() <= eq) {
		    c.sendNotice(1, "Plaase make space for your Equip inventory.");
		    return false;
		} else if (c.getUseInventory().getNumFreeSlot() <= use) {
		    c.sendNotice(1, "Plaase make space for your Use inventory.");
		    return false;
		} else if (c.getSetupInventory().getNumFreeSlot() <= setup) {
		    c.sendNotice(1, "Plaase make space for your Setup inventory.");
		    return false;
		} else if (c.getEtcInventory().getNumFreeSlot() <= etc) {
		    c.sendNotice(1, "Plaase make space for your Etc inventory.");
		    return false;
		} else if (c.getCashInventory().getNumFreeSlot() <= cash) {
		    c.sendNotice(1, "Plaase make space for your Cash inventory.");
		    return false;
		}
		return true;
	    }
	    case money: {
		final int meso = WzDataTool.getInt(data, 0);
		if (c.getMeso() + meso < 0) { // Giving, overflow
		    c.sendNotice(1, "Meso exceed the max amount, 2147483647.");
		    return false;
		} else if (c.getMeso() < meso) {
		    c.sendNotice(1, "Insufficient meso.");
		    return false;
		}
		return true;
	    }
	}
	return true;
    }

    public void runEnd(GameCharacter c, Integer extSelection) {
	switch (type) {
	    case exp: {
		c.gainExp((WzDataTool.getInt(data, 0) * (c.getLevel() <= 10 ? 1 : c.getClient().getChannelServer().getExpRate())), true, true, true);
		break;
	    }
	    case item: {
		// first check for randomness in item selection
		Map<Integer, Integer> props = new HashMap<Integer, Integer>();

		for (WzData iEntry : data.getChildren()) {
		    final WzData prop = iEntry.getChildByPath("prop");
		    if (prop != null && WzDataTool.getInt(prop) != -1 && canGetItem(iEntry, c)) {
			for (int i = 0; i < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
			    props.put(props.size(), WzDataTool.getInt(iEntry.getChildByPath("id")));
			}
		    }
		}
		int selection = 0;
		int extNum = 0;
		if (props.size() > 0) {
		    selection = props.get(Randomizer.nextInt(props.size()));
		}
		for (WzData iEntry : data.getChildren()) {
		    if (!canGetItem(iEntry, c)) {
			continue;
		    }
		    final int id = WzDataTool.getInt(iEntry.getChildByPath("id"), -1);
		    if (iEntry.getChildByPath("prop") != null) {
			if (WzDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
			    if (extSelection != extNum++) {
				continue;
			    }
			} else if (id != selection) {
			    continue;
			}
		    }
		    final short count = (short) WzDataTool.getInt(iEntry.getChildByPath("count"), 1);
		    if (count < 0) { // remove items
			InventoryManipulator.removeById(c.getClient(), c.getInventoryForItem(id), id, (count * -1), true, false);
			c.getClient().write(MaplePacketCreator.getShowItemGain(id, count, true));
		    } else { // add items
//			final int period = MapleDataTool.getInt(iEntry.getChildByPath("period"), 0);
			InventoryManipulator.addById(c.getClient(), id, count, ""/*, -1, period * 60 * 1000*/);
			c.getClient().write(MaplePacketCreator.getShowItemGain(id, count, true));
		    }
		}
		break;
	    }
//			case NEXTQUEST:
//				int nextquest = MapleDataTool.getInt(data);
//				Need to somehow make the chat popup for the next quest...
//				break;
	    case money: {
		c.gainMeso(WzDataTool.getInt(data, 0), true, false, true);
		break;
	    }
	    case QUEST: {
		for (WzData qEntry : data) {
		    c.updateQuest(
			    new QuestStatus(Quest.getInstance(WzDataTool.getInt(qEntry.getChildByPath("id"))),
			    (byte) WzDataTool.getInt(qEntry.getChildByPath("state"), 0)));
		}
		break;
	    }
	    case skill: {
		for (WzData sEntry : data) {
		    final int skillid = WzDataTool.getInt(sEntry.getChildByPath("id"));
		    int skillLevel = WzDataTool.getInt(sEntry.getChildByPath("skillLevel"), 0);
		    int masterLevel = WzDataTool.getInt(sEntry.getChildByPath("masterLevel"), 0);
		    final ISkill skillObject = SkillFactory.getSkill(skillid);

		    for (WzData applicableJob : sEntry.getChildByPath("job")) {
			if (skillObject.isBeginnerSkill() || c.getJob() == WzDataTool.getInt(applicableJob)) {
			    c.changeSkillLevel(skillObject,
				    (byte) Math.max(skillLevel, c.getCurrentSkillLevel(skillObject)),
				    (byte) Math.max(masterLevel, c.getMasterSkillLevel(skillObject)));
			    break;
			}
		    }
		}
		break;
	    }
	    case pop: {
		final int fameGain = WzDataTool.getInt(data, 0);
		c.addFame(fameGain);
		c.updateSingleStat(Stat.FAME, c.getFame());
		c.getClient().write(MaplePacketCreator.getShowFameGain(fameGain));
		break;
	    }
	    case buffItemID: {
		final int tobuff = WzDataTool.getInt(data, -1);
		if (tobuff == -1) {
		    break;
		}
		ItemInfoProvider.getInstance().getItemEffect(tobuff).applyTo(c);
		break;
	    }
	    case infoNumber: {
//		System.out.println("quest : "+MapleDataTool.getInt(data, 0)+"");
//		MapleQuest.getInstance(MapleDataTool.getInt(data, 0)).forceComplete(c, 0);
//		break;
	    }
	    default:
		break;
	}
    }

    private static int getJobBy5ByteEncoding(int encoded) {
	switch (encoded) {
	    case 2:
	    case 3:
		return 100;
	    case 4:
		return 200;
	    case 8:
		return 300;
	    case 16:
		return 400;
	    case 32:
	    case 63:
		return 500;
	    default:
		return 0;
	}
    }

    private static int getJobByEncoding(int encoded, int playerjob) {
	switch (encoded) {
	    case 1049601:
		if (GameConstants.isKOC(playerjob)) {
		    return 1000;
		}
		return 0;
	    case 2099202:
		if (GameConstants.isKOC(playerjob)) {
		    return 1100;
		} else if (GameConstants.isAran(playerjob)) {
		    return 2000;
		}
		return 100;
	    case 4100:
		if (GameConstants.isKOC(playerjob)) {
		    return 1200;
		}
		return 200;
	    case 8200:
		if (GameConstants.isKOC(playerjob)) {
		    return 1300;
		}
		return 300;
	    case 16400:
		if (GameConstants.isKOC(playerjob)) {
		    return 1400;
		}
		return 400;
	    case 32800:
		if (GameConstants.isKOC(playerjob)) {
		    return 1500;
		}
		return 500;
	    default:
		return 0;
	}
    }

    public QuestActionType getType() {
	return type;
    }

    @Override
    public String toString() {
	return type + ": " + data;
    }
}
