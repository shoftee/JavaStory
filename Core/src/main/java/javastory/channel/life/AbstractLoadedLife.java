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
package javastory.channel.life;

import javastory.channel.maps.AbstractAnimatedGameMapObject;

public abstract class AbstractLoadedLife extends AbstractAnimatedGameMapObject {

	private final int id;
	private int facingDirection;
	private boolean isHidden;
	private int foothold, originFoothold;
	private int cy;
	private int rx0;
	private int rx1;

	public AbstractLoadedLife(final int id) {
		this.id = id;
	}

	public AbstractLoadedLife(final AbstractLoadedLife life) {
		this(life.getId());
		this.facingDirection = life.facingDirection;
		this.isHidden = life.isHidden;
		this.foothold = life.foothold;
		this.originFoothold = life.foothold;
		this.cy = life.cy;
		this.rx0 = life.rx0;
		this.rx1 = life.rx1;
	}

	@Override
	public int getFacingDirection() {
		return this.facingDirection;
	}

	public void setFacingDirection(final int facingDirection) {
		this.facingDirection = facingDirection;
	}

	public boolean isHidden() {
		return this.isHidden;
	}

	public void setHidden(final boolean hide) {
		this.isHidden = hide;
	}

	public int getOriginFoothold() {
		return this.originFoothold;
	}

	public int getFoothold() {
		return this.foothold;
	}

	public void setFoothold(final int foothold) {
		this.foothold = foothold;
	}

	public int getCy() {
		return this.cy;
	}

	public void setCy(final int cy) {
		this.cy = cy;
	}

	public int getRx0() {
		return this.rx0;
	}

	public void setRx0(final int rx0) {
		this.rx0 = rx0;
	}

	public int getRx1() {
		return this.rx1;
	}

	public void setRx1(final int rx1) {
		this.rx1 = rx1;
	}

	public int getId() {
		return this.id;
	}
}
