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
package javastory.channel;

import java.io.Serializable;

public class GuildSummary implements Serializable {

	public static final long serialVersionUID = 3565477792085301248L;
	private final String name;
	private final short logoBG;
	private final byte logoBGColor;
	private final short logo;
	private final byte logoColor;

	public GuildSummary(final Guild g) {
		this.name = g.getName();
		this.logoBG = (short) g.getLogoBG();
		this.logoBGColor = (byte) g.getLogoBGColor();
		this.logo = (short) g.getLogo();
		this.logoColor = (byte) g.getLogoColor();
	}

	public String getName() {
		return this.name;
	}

	public short getLogoBG() {
		return this.logoBG;
	}

	public byte getLogoBGColor() {
		return this.logoBGColor;
	}

	public short getLogo() {
		return this.logo;
	}

	public byte getLogoColor() {
		return this.logoColor;
	}
}
