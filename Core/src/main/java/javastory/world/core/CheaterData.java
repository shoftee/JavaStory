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
package javastory.world.core;

import java.io.Serializable;

public class CheaterData implements Serializable, Comparable<CheaterData> {

	private static final long serialVersionUID = -8733673311051249885L;
	private int points;
	private String info;

	public CheaterData(int points, String info) {
		this.points = points;
		this.info = info;
	}

	public String getInfo() {
		return info;
	}

	public int getPoints() {
		return points;
	}

	@Override
	public int compareTo(CheaterData o) {
		int thisValue = getPoints();
		int otherValue = o.getPoints();
		if (thisValue < otherValue)
			return 1;
		else if (thisValue == otherValue)
			return 0;
		else
			return -1;
	}
}
