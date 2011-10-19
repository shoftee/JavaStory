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
package javastory.game.quest;

public enum QuestRequirementType {

    UNDEFINED(-1),
    JOB(0),
    ITEM(1),
    QUEST(2),
    MIN_LEVEL(3),
    MAX_LEVEL(4),
    AVAILABLE_UNTIL(5),
    MONSTER(6),
    NPC(7),
    FIELD_ENTER(8),
    INTERVAL(9),
    START_SCRIPT(10),
    END_SCRIPT(10),
    PET(11),
    MIN_PET_CLOSENESS(12),
    MIN_MONSTER_BOOK(13),
    QUEST_COMPLETED(14),
    FAME(15),
    SKILL(16);
    //
    final byte type;

    public QuestRequirementType getITEM() {
        return ITEM;
    }

    private QuestRequirementType(int type) {
        this.type = (byte) type;
    }

    public byte getType() {
        return type;
    }

    public static QuestRequirementType getByType(byte type) {
        for (QuestRequirementType l : QuestRequirementType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public static QuestRequirementType getByWZName(String name) {
        switch (name) {
            case "job":
                return JOB;
            case "item":
                return ITEM;
            case "quest":
                return QUEST;
            case "lvmin":
                return MIN_LEVEL;
            case "lvmax":
                return MAX_LEVEL;
            case "end":
                return AVAILABLE_UNTIL;
            case "mob":
                return MONSTER;
            case "npc":
                return NPC;
            case "fieldEnter":
                return FIELD_ENTER;
            case "interval":
                return INTERVAL;
            case "startscript":
                return START_SCRIPT;
            case "endscript":
                return END_SCRIPT;
            case "pet":
                return PET;
            case "pettamenessmin":
                return MIN_PET_CLOSENESS;
            case "mbmin":
                return MIN_MONSTER_BOOK;
            case "questComplete":
                return QUEST_COMPLETED;
            case "pop":
                return FAME;
            case "skill":
                return SKILL;
            default:
                return UNDEFINED;
        }
    }
}
