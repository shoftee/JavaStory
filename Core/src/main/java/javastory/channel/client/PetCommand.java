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

/**
 * 
 * @author Danny (Leifde)
 */
public class PetCommand {

	private final int petId;
	private final int skillId;
	private final int probability;
	private final int increase;

	public PetCommand(final int petId, final int skillId, final int prob, final int inc) {
		this.petId = petId;
		this.skillId = skillId;
		this.probability = prob;
		this.increase = inc;
	}

	public int getPetId() {
		return this.petId;
	}

	public int getSkillId() {
		return this.skillId;
	}

	public int getProbability() {
		return this.probability;
	}

	public int getIncrease() {
		return this.increase;
	}
}
