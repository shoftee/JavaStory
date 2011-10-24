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
package javastory.channel.movement;

import java.awt.Point;

import javastory.io.PacketBuilder;

public class BounceMovement extends AbstractLifeMovement {

	private int unk;
	private int fh;

	public BounceMovement(int type, Point position, int duration, int newstate) {
		super(type, position, duration, newstate);
	}

	public int getUnk() {
		return unk;
	}

	public void setUnk(int unk) {
		this.unk = unk;
	}

	public int getFH() {
		return fh;
	}

	public void setFH(int fh) {
		this.fh = fh;
	}

	@Override
	public void serialize(PacketBuilder builder) {
		builder.writeAsByte(getType());
		builder.writeVector(getPosition());
		builder.writeAsShort(getUnk());
		builder.writeAsShort(getFH());
		builder.writeAsByte(getNewstate());
		builder.writeAsShort(getDuration());
	}
}