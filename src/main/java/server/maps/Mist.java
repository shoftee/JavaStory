/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package server.maps;

import java.awt.Point;
import java.awt.Rectangle;

import client.ISkill;
import client.GameCharacter;
import client.GameClient;
import client.SkillFactory;
import handling.GamePacket;
import server.StatEffect;
import server.life.Monster;
import server.life.MobSkill;
import tools.MaplePacketCreator;

public class Mist extends AbstractGameMapObject {

    private Rectangle mistPosition;
    private GameCharacter owner = null; // Assume this is removed, else weakref will be required
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

    public Mist(Rectangle mistPosition, GameCharacter owner, StatEffect source) {
	this.mistPosition = mistPosition;
	this.owner = owner;
	this.source = source;
	this.skilllevel = owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId()));

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

    public GameCharacter getOwner() {
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
	    return MaplePacketCreator.spawnMist(this);
	}
	return MaplePacketCreator.spawnMist(this);
    }

    @Override
    public void sendSpawnData(final GameClient c) {
	c.getSession().write(MaplePacketCreator.spawnMist(this));
    }

    @Override
    public void sendDestroyData(final GameClient c) {
	c.getSession().write(MaplePacketCreator.removeMist(getObjectId()));
    }

    public boolean makeChanceResult() {
	return source.makeChanceResult();
    }
}