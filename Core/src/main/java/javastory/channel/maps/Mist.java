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
package javastory.channel.maps;

import java.awt.Point;
import java.awt.Rectangle;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.ISkill;
import javastory.channel.client.SkillFactory;
import javastory.channel.life.MobSkill;
import javastory.channel.life.Monster;
import javastory.channel.server.StatEffect;
import javastory.io.GamePacket;
import javastory.tools.packets.ChannelPackets;

public class Mist extends AbstractGameMapObject {

	private Rectangle mistPosition;
	private ChannelCharacter owner = null; // Assume this is removed, else
											// weakref will be required
	private Monster mob = null;
	private StatEffect source;
	private MobSkill skill;
	private boolean isMobMist, isPoisonMist;
	private int skillDelay, skilllevel;

	public Mist(Rectangle mistPosition, Monster mob, MobSkill skill) {
		this.mistPosition = mistPosition;
		this.mob = mob;
		this.skill = skill;
		skilllevel = skill.getSkillId();

		isMobMist = true;
		isPoisonMist = true;
		skillDelay = 0;
	}

	public Mist(Rectangle mistPosition, ChannelCharacter owner, StatEffect source) {
		this.mistPosition = mistPosition;
		this.owner = owner;
		this.source = source;
		this.skilllevel = owner.getCurrentSkillLevel(SkillFactory.getSkill(source.getSourceId()));

		switch (source.getSourceId()) {
		case 4221006: // Smoke Screen
			isMobMist = false;
			isPoisonMist = false;
			skillDelay = 8;
			break;
		case 2111003: // FP mist
		case 12111005: // Flame wizard, [Flame Gear]
			isMobMist = false;
			isPoisonMist = true;
			skillDelay = 8;
			break;
		}
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.MIST;
	}

	@Override
	public Point getPosition() {
		return mistPosition.getLocation();
	}

	public ISkill getSourceSkill() {
		return SkillFactory.getSkill(source.getSourceId());
	}

	public boolean isMobMist() {
		return isMobMist;
	}

	public boolean isPoisonMist() {
		return isPoisonMist;
	}

	public int getSkillDelay() {
		return skillDelay;
	}

	public int getSkillLevel() {
		return skilllevel;
	}

	public Monster getMobOwner() {
		return mob;
	}

	public ChannelCharacter getOwner() {
		return owner;
	}

	public MobSkill getMobSkill() {
		return this.skill;
	}

	public Rectangle getBox() {
		return mistPosition;
	}

	@Override
	public void setPosition(Point position) {
	}

	public GamePacket fakeSpawnData(int level) {
		if (owner != null) {
			return ChannelPackets.spawnMist(this);
		}
		return ChannelPackets.spawnMist(this);
	}

	@Override
	public void sendSpawnData(final ChannelClient c) {
		c.write(ChannelPackets.spawnMist(this));
	}

	@Override
	public void sendDestroyData(final ChannelClient c) {
		c.write(ChannelPackets.removeMist(getObjectId()));
	}

	public boolean makeChanceResult() {
		return source.makeChanceResult();
	}
}