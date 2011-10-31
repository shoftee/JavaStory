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

public class AbsoluteLifeMovement extends AbstractLifeMovement {

	private Point pixelsPerSecond, offset;
	private int unk;

	public AbsoluteLifeMovement(final int type, final Point position, final int duration, final int newstate) {
		super(type, position, duration, newstate);
	}

	public Point getPixelsPerSecond() {
		return this.pixelsPerSecond;
	}

	public void setPixelsPerSecond(final Point wobble) {
		this.pixelsPerSecond = wobble;
	}

	public Point getOffset() {
		return this.offset;
	}

	public void setOffset(final Point wobble) {
		this.offset = wobble;
	}

	public int getUnk() {
		return this.unk;
	}

	public void setUnk(final int unk) {
		this.unk = unk;
	}

	@Override
	public void serialize(final PacketBuilder builder) {
		builder.writeAsByte(this.getType());
		builder.writeVector(this.getPosition());
		builder.writeVector(this.pixelsPerSecond);
		builder.writeAsShort(this.unk);
		builder.writeVector(this.offset);
		builder.writeAsByte(this.getNewstate());
		builder.writeAsShort(this.getDuration());
	}
}
