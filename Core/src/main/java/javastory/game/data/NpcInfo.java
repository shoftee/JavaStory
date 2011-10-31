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

import java.util.HashMap;
import java.util.Map;

public class NpcInfo {

	private String name;
	private Map<Byte, Integer> equips;
	private int face, hair;
	private byte skin;
	private int foothold, rangeXStart, rangeXEnd, centerY;

	public NpcInfo(final String name, final boolean playerNpc) {
		this.name = name;

		if (playerNpc) {
			this.equips = new HashMap<>();
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Map<Byte, Integer> getEquips() {
		return this.equips;
	}

	public int getFH() {
		return this.foothold;
	}

	public int getRX0() {
		return this.rangeXStart;
	}

	public int getRX1() {
		return this.rangeXEnd;
	}

	public int getCY() {
		return this.centerY;
	}

	public byte getSkin() {
		return this.skin;
	}

	public int getFace() {
		return this.face;
	}

	public int getHair() {
		return this.hair;
	}

	public void setEquips(final Map<Byte, Integer> equips) {
		this.equips = equips;
	}

	public void setFH(final int FH) {
		this.foothold = FH;
	}

	public void setRX0(final int RX0) {
		this.rangeXStart = RX0;
	}

	public void setRX1(final int RX1) {
		this.rangeXEnd = RX1;
	}

	public void setCY(final int CY) {
		this.centerY = CY;
	}

	public void setSkin(final byte skin) {
		this.skin = skin;
	}

	public void setFace(final int face) {
		this.face = face;
	}

	public void setHair(final int hair) {
		this.hair = hair;
	}
}
