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
package javastory.game;

public interface IEquip extends IItem {

	public static enum ScrollResult {

		SUCCESS,
		FAIL,
		CURSE
	}

	byte getUpgradeSlots();

	byte getLevel();

	public byte getViciousHammer();

	public byte getItemLevel();

	public short getItemEXP();

	public int getRingId();

	public short getStr();

	public short getDex();

	public short getInt();

	public short getLuk();

	public short getHp();

	public short getMp();

	public short getWatk();

	public short getMatk();

	public short getWdef();

	public short getMdef();

	public short getAcc();

	public short getAvoid();

	public short getHands();

	public short getSpeed();

	public short getJump();
}
