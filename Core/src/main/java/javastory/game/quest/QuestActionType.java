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
package javastory.game.quest;

public enum QuestActionType {

	UNDEFINED(-1), EXP(0), ITEM(1), NEXT_QUEST(2), MESO(3), QUEST(4), SKILL(5), FAME(6), BUFF_ITEM_ID(7), INFO_NUMBER(8);
	//
	final byte type;

	private QuestActionType(final int type) {
		this.type = (byte) type;
	}

	public byte getType() {
		return this.type;
	}

	public static QuestActionType getByType(final byte type) {
		for (final QuestActionType l : QuestActionType.values()) {
			if (l.getType() == type) {
				return l;
			}
		}
		return null;
	}

	public static QuestActionType getByWZName(final String name) {
		switch (name) {
		case "exp":
			return EXP;
		case "item":
			return ITEM;
		case "money":
			return MESO;
		case "QUEST":
			return QUEST;
		case "skill":
			return SKILL;
		case "pop":
			return FAME;
		case "buffItemID":
			return BUFF_ITEM_ID;
		case "infoNumber":
			return INFO_NUMBER;
		default:
			return UNDEFINED;
		}
	}
}
