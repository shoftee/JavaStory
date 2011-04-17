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

public enum QuestActionType {

    UNDEFINED(-1), exp(0), item(1), nextQuest(2), money(3), QUEST(4), skill(5), pop(6), buffItemID(7), infoNumber(8);
    final byte type;

    private QuestActionType(int type) {
	this.type = (byte) type;
    }

    public byte getType() {
	return type;
    }

    public static QuestActionType getByType(byte type) {
	for (QuestActionType l : QuestActionType.values()) {
	    if (l.getType() == type) {
		return l;
	    }
	}
	return null;
    }

    public static QuestActionType getByWZName(String name) {
	try {
	    return valueOf(name);
	} catch (IllegalArgumentException ex) {
	    return UNDEFINED;
	}
    }
}
