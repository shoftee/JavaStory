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
package javastory.channel.maps;

import java.awt.Point;

import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class Foothold implements Comparable<Foothold> {
	private final Point p1;
	private final Point p2;
	private final int id;
	private final short nextId, prevId;

	public Foothold(final WzData data) {
		this.p1 = WzDataTool.getPoint("x1", "y1", data);
		this.p2 = WzDataTool.getPoint("x2", "y2", data);

		this.id = Integer.parseInt(data.getName());

		this.nextId = (short) WzDataTool.getInt("next", data);
		this.prevId = (short) WzDataTool.getInt("prev", data);
	}

	public boolean isWall() {
		return this.p1.x == this.p2.x;
	}

	public int getX1() {
		return this.p1.x;
	}

	public int getX2() {
		return this.p2.x;
	}

	public int getY1() {
		return this.p1.y;
	}

	public int getY2() {
		return this.p2.y;
	}

	@Override
	public int compareTo(final Foothold other) {
		if (this.p2.y < other.getY1()) {
			return -1;
		} else if (this.p1.y > other.getY2()) {
			return 1;
		} else {
			return 0;
		}
	}

	public int getId() {
		return this.id;
	}

	public short getNextId() {
		return this.nextId;
	}

	public short getPrevId() {
		return this.prevId;
	}
}
