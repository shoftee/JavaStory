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
package javastory.quest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.channel.client.ISkill;
import javastory.channel.client.SkillFactory;
import javastory.channel.server.InventoryManipulator;
import javastory.game.GameConstants;
import javastory.game.Gender;
import javastory.game.Jobs;
import javastory.server.ItemInfoProvider;
import javastory.tools.Randomizer;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;
import tools.MaplePacketCreator;
import client.Inventory;
import client.QuestStatus;
import client.Stat;

public class QuestAction implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private QuestActionType actionType;
    private WzData data;
    private int questId;

    /** Creates a new instance of MapleQuestAction */
    public QuestAction(int quest, QuestActionType type, WzData data) {
        this.actionType = type;
        this.data = data;
        this.questId = quest;
    }

    private static boolean canGetItem(WzData item, ChannelCharacter c) {
        if (item.getChildByPath("gender") != null) {
            final Gender gender = Gender.fromNumber(
                    WzDataTool.getInt(item.getChildByPath("gender")));
            if (!gender.equals(Gender.UNSPECIFIED)) {
                return false;
            } else if (!gender.equals(c.getGender())) {
                return false;
            }
        }
        if (item.getChildByPath("job") != null) {
            final int job = WzDataTool.getInt(item.getChildByPath("job"));
            if (job < 100) {
                final int codec = getJobBy5ByteEncoding(job);
                if (codec / 100 != c.getJobId() / 100) {
                    return false;
                }
            } else if (job > 3000) {
                final int playerjob = c.getJobId();
                final int codec = getJobByEncoding(job, playerjob);
                if (codec >= 1000) {
                    if (codec / 1000 != c.getJobId() / 1000) {
                        return false;
                    }
                } else {
                    if (codec / 100 != c.getJobId() / 100) {
                        return false;
                    }
                }
            } else {
                if (job != c.getJobId()) {
                    return false;
                }
            }
        }
        return true;
    }

    public final boolean restoreLostItem(final ChannelCharacter c, final int itemid) {
        if (actionType == QuestActionType.ITEM) {
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

    public void runStart(ChannelCharacter c, Integer extSelection) {
        QuestStatus status;
        switch (actionType) {
            case EXP:
                status = c.getQuestStatus(questId);
                if (status.getForfeited() > 0) {
                    break;
                }
                c.gainExp((WzDataTool.getInt(data, 0)
                        * (c.getLevel() <= 10 ? 1 : c.getClient().getChannelServer().getExpRate())), true, true, true);
                break;
            case ITEM:
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<>();
                WzData prop;
                for (WzData iEntry : data.getChildren()) {
                    prop = iEntry.getChildByPath("prop");
                    if (prop != null && WzDataTool.getInt(prop) != -1
                            && canGetItem(iEntry, c)) {
                        for (int i = 0; i
                                < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
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
                        if (WzDataTool.getInt(iEntry.getChildByPath("prop"))
                                == -1) {
                            if (extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    final short count = (short) WzDataTool.getInt(iEntry.getChildByPath("count"), 1);
                    if (count < 0) { // remove items
                        // NOTE: This is very wrong: do not use exceptions for expected scenarios.
                        // They're supposed to be used for EXCEPTIONAL scenarios. Duh.
                        //try {
                            final Inventory inventory = c.getInventoryForItem(id);
                            InventoryManipulator.removeById(c.getClient(), inventory, id, (count
                                    * -1), true, false);
                        //} catch (InventoryException ie) {
                            //System.err.println("[h4x] Completing a quest without meeting the requirements"                                    + ie);
                        //}
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
            case MESO:
                status = c.getQuestStatus(questId);
                if (status.getForfeited() > 0) {
                    break;
                }
                c.gainMeso(WzDataTool.getInt(data, 0), true, false, true);
                break;
            case QUEST:
                for (WzData qEntry : data) {
                    final int nextQuestId = WzDataTool.getInt(qEntry.getChildByPath("id"));
                    final int nextQuestState = WzDataTool.getInt(qEntry.getChildByPath("state"), 0);
                    //TODO: Hmmmmm.
                    c.getQuestStatus(nextQuestId).setState((byte) nextQuestState);
                }
                break;
            case SKILL:
                //TODO needs gain/lost message?
                for (WzData sEntry : data) {
                    final int skillid = WzDataTool.getInt(sEntry.getChildByPath("id"));
                    int skillLevel = WzDataTool.getInt(sEntry.getChildByPath("skillLevel"), 0);
                    int masterLevel = WzDataTool.getInt(sEntry.getChildByPath("masterLevel"), 0);
                    final ISkill skillObject = SkillFactory.getSkill(skillid);

                    for (WzData applicableJob : sEntry.getChildByPath("job")) {
                        if (skillObject.isBeginnerSkill() || c.getJobId()
                                == WzDataTool.getInt(applicableJob)) {
                            c.changeSkillLevel(skillObject,
                                    (byte) Math.max(skillLevel, c.getCurrentSkillLevel(skillObject)),
                                    (byte) Math.max(masterLevel, c.getMasterSkillLevel(skillObject)));
                            break;
                        }
                    }
                }
                break;
            case FAME:
                status = c.getQuestStatus(questId);
                if (status.getForfeited() > 0) {
                    break;
                }
                final int fameGain = WzDataTool.getInt(data, 0);
                c.addFame(fameGain);
                c.updateSingleStat(Stat.FAME, c.getFame());
                c.getClient().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            case BUFF_ITEM_ID:
                status = c.getQuestStatus(questId);
                if (status.getForfeited() > 0) {
                    break;
                }
                final int tobuff = WzDataTool.getInt(data, -1);
                if (tobuff == -1) {
                    break;
                }
                ItemInfoProvider.getInstance().getItemEffect(tobuff).applyTo(c);
                break;
            case INFO_NUMBER: {
//		System.out.println("quest : "+MapleDataTool.getInt(data, 0)+"");
//		MapleQuest.getInstance(MapleDataTool.getInt(data, 0)).forceComplete(c, 0);
//		break;
            }
            default:
                break;
        }
    }

    public boolean checkEnd(ChannelCharacter c, Integer extSelection) {
        switch (actionType) {
            case ITEM: {
                // first check for randomness in item selection
                final Map<Integer, Integer> props = new HashMap<>();

                for (WzData iEntry : data.getChildren()) {
                    final WzData prop = iEntry.getChildByPath("prop");
                    if (prop != null && WzDataTool.getInt(prop) != -1
                            && canGetItem(iEntry, c)) {
                        for (int i = 0; i
                                < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
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
                        if (WzDataTool.getInt(iEntry.getChildByPath("prop"))
                                == -1) {
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
            case MESO: {
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

    public void runEnd(ChannelCharacter c, Integer extSelection) {
        switch (actionType) {
            case EXP: {
                c.gainExp((WzDataTool.getInt(data, 0)
                        * (c.getLevel() <= 10 ? 1 : c.getClient().getChannelServer().getExpRate())), true, true, true);
                break;
            }
            case ITEM: {
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<>();

                for (WzData iEntry : data.getChildren()) {
                    final WzData prop = iEntry.getChildByPath("prop");
                    if (prop != null && WzDataTool.getInt(prop) != -1
                            && canGetItem(iEntry, c)) {
                        for (int i = 0; i
                                < WzDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
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
                        if (WzDataTool.getInt(iEntry.getChildByPath("prop"))
                                == -1) {
                            if (extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    final short count = (short) WzDataTool.getInt(iEntry.getChildByPath("count"), 1);
                    if (count < 0) { // remove items
                        InventoryManipulator.removeById(c.getClient(), c.getInventoryForItem(id), id, (count
                                * -1), true, false);
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
            case MESO: {
                c.gainMeso(WzDataTool.getInt(data, 0), true, false, true);
                break;
            }
            case QUEST: {
                for (WzData qEntry : data) {
                    final int nextQuestId = WzDataTool.getInt(qEntry.getChildByPath("id"));
                    final int nextQuestState = WzDataTool.getInt(qEntry.getChildByPath("state"), 0);
                    //TODO: Hmmmmm.
                    c.getQuestStatus(nextQuestId).setState((byte) nextQuestState);
                }
                break;
            }
            case SKILL: {
                for (WzData sEntry : data) {
                    final int skillid = WzDataTool.getInt(sEntry.getChildByPath("id"));
                    int skillLevel = WzDataTool.getInt(sEntry.getChildByPath("skillLevel"), 0);
                    int masterLevel = WzDataTool.getInt(sEntry.getChildByPath("masterLevel"), 0);
                    final ISkill skillObject = SkillFactory.getSkill(skillid);

                    for (WzData applicableJob : sEntry.getChildByPath("job")) {
                        if (skillObject.isBeginnerSkill() || c.getJobId()
                                == WzDataTool.getInt(applicableJob)) {
                            c.changeSkillLevel(skillObject,
                                    (byte) Math.max(skillLevel, c.getCurrentSkillLevel(skillObject)),
                                    (byte) Math.max(masterLevel, c.getMasterSkillLevel(skillObject)));
                            break;
                        }
                    }
                }
                break;
            }
            case FAME: {
                final int fameGain = WzDataTool.getInt(data, 0);
                c.addFame(fameGain);
                c.updateSingleStat(Stat.FAME, c.getFame());
                c.getClient().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            }
            case BUFF_ITEM_ID: {
                final int tobuff = WzDataTool.getInt(data, -1);
                if (tobuff == -1) {
                    break;
                }
                ItemInfoProvider.getInstance().getItemEffect(tobuff).applyTo(c);
                break;
            }
            case INFO_NUMBER: {
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
                if (Jobs.isCygnus(playerjob)) {
                    return 1000;
                }
                return 0;
            case 2099202:
                if (Jobs.isCygnus(playerjob)) {
                    return 1100;
                } else if (Jobs.isAran(playerjob)) {
                    return 2000;
                }
                return 100;
            case 4100:
                if (Jobs.isCygnus(playerjob)) {
                    return 1200;
                }
                return 200;
            case 8200:
                if (Jobs.isCygnus(playerjob)) {
                    return 1300;
                }
                return 300;
            case 16400:
                if (Jobs.isCygnus(playerjob)) {
                    return 1400;
                }
                return 400;
            case 32800:
                if (Jobs.isCygnus(playerjob)) {
                    return 1500;
                }
                return 500;
            default:
                return 0;
        }
    }

    public QuestActionType getType() {
        return actionType;
    }

    @Override
    public String toString() {
        return actionType + ": " + data;
    }
}
