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
package javastory.server.channel;

public class GuildRankingInfo {

	private final String name;
	private final int gp, logo, logocolor, logobg, logobgcolor;

	public GuildRankingInfo(final String name, final int gp, final int logo, final int logocolor, final int logobg, final int logobgcolor) {
		this.name = name;
		this.gp = gp;
		this.logo = logo;
		this.logocolor = logocolor;
		this.logobg = logobg;
		this.logobgcolor = logobgcolor;
	}

	public String getName() {
		return this.name;
	}

	public int getGP() {
		return this.gp;
	}

	public int getLogo() {
		return this.logo;
	}

	public int getLogoColor() {
		return this.logocolor;
	}

	public int getLogoBg() {
		return this.logobg;
	}

	public int getLogoBgColor() {
		return this.logobgcolor;
	}
}
