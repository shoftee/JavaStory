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

public abstract class AbstractLifeMovement implements LifeMovement {

	private final Point position;
	private final int duration;
	private final int newstate;
	private final int type;

	public AbstractLifeMovement(final int type, final Point position, final int duration, final int newstate) {
		super();
		this.type = type;
		this.position = position;
		this.duration = duration;
		this.newstate = newstate;
	}

	@Override
	public int getType() {
		return this.type;
	}

	@Override
	public int getDuration() {
		return this.duration;
	}

	@Override
	public int getNewstate() {
		return this.newstate;
	}

	@Override
	public Point getPosition() {
		return this.position;
	}
}
