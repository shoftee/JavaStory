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
package javastory.game.data;

public final class MobGlobalDropInfo {

	public final byte DropType;
	public final short QuestId;
	public final int ItemId, Chance, Minimum, Maximum;

	public MobGlobalDropInfo(int itemId, int chance, int continent, byte dropType, int minimum, int maximum, short questId) {
		this.ItemId = itemId;
		this.Chance = chance;
		this.DropType = dropType;
		this.QuestId = questId;
		this.Minimum = minimum;
		this.Maximum = maximum;
	}
}