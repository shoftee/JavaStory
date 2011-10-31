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

package javastory.channel.client;

import java.io.Serializable;

public class SkillMacro implements Serializable {

	private static final long serialVersionUID = -63413738569L;
	private int macroId;
	private int skill1;
	private int skill2;
	private int skill3;
	private String name;
	private int shout;
	private int position;

	public SkillMacro(final int skill1, final int skill2, final int skill3, final String name, final int shout, final int position) {
		this.skill1 = skill1;
		this.skill2 = skill2;
		this.skill3 = skill3;
		this.name = name;
		this.shout = shout;
		this.position = position;
	}

	public int getMacroId() {
		return this.macroId;
	}

	public int getSkill1() {
		return this.skill1;
	}

	public int getSkill2() {
		return this.skill2;
	}

	public int getSkill3() {
		return this.skill3;
	}

	public String getName() {
		return this.name;
	}

	public int getShout() {
		return this.shout;
	}

	public int getPosition() {
		return this.position;
	}

	public void setMacroId(final int macroId) {
		this.macroId = macroId;
	}

	public void setSkill1(final int skill1) {
		this.skill1 = skill1;
	}

	public void setSkill2(final int skill2) {
		this.skill2 = skill2;
	}

	public void setSkill3(final int skill3) {
		this.skill3 = skill3;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setShout(final int shout) {
		this.shout = shout;
	}

	public void setPosition(final int position) {
		this.position = position;
	}
}