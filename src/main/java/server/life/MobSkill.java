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
package server.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import org.javastory.client.ChannelCharacter;
import client.Disease;
import client.status.MonsterStatus;
import com.google.common.collect.Maps;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.maps.Mist;

public class MobSkill {

    private int skillId, skillLevel, mpCon, spawnEffect, hp, x, y;
    private long duration, cooltime;
    private float prop;
//    private short effect_delay;
    private short limit;
    private List<Integer> toSummon = new ArrayList<>();
    private Point lt, rb;

    public MobSkill(int skillId, int level) {
	this.skillId = skillId;
	this.skillLevel = level;
    }

    public void setMpCon(int mpCon) {
	this.mpCon = mpCon;
    }

    public void addSummons(List<Integer> toSummon) {
	this.toSummon = toSummon;
    }

    /*   public void setEffectDelay(short effect_delay) {
    this.effect_delay = effect_delay;
    }*/
    public void setSpawnEffect(int spawnEffect) {
	this.spawnEffect = spawnEffect;
    }

    public void setHp(int hp) {
	this.hp = hp;
    }

    public void setX(int x) {
	this.x = x;
    }

    public void setY(int y) {
	this.y = y;
    }

    public void setDuration(long duration) {
	this.duration = duration;
    }

    public void setCoolTime(long cooltime) {
	this.cooltime = cooltime;
    }

    public void setProp(float prop) {
	this.prop = prop;
    }

    public void setLtRb(Point lt, Point rb) {
	this.lt = lt;
	this.rb = rb;
    }

    public void setLimit(short limit) {
	this.limit = limit;
    }

    public boolean checkCurrentBuff(ChannelCharacter player, Monster monster) {
	boolean stop = false;
	switch (skillId) {
	    case 100:
	    case 110:
		stop = monster.isBuffed(MonsterStatus.WEAPON_ATTACK_UP);
		break;
	    case 101:
	    case 111:
		stop = monster.isBuffed(MonsterStatus.MAGIC_ATTACK_UP);
		break;
	    case 102:
	    case 112:
		stop = monster.isBuffed(MonsterStatus.WEAPON_DEFENSE_UP);
		break;
	    case 103:
	    case 113:
		stop = monster.isBuffed(MonsterStatus.MAGIC_DEFENSE_UP);
		break;
	    case 140:
	    case 141:
	    case 143:
	    case 144:
	    case 145:
		stop = monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) || monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY);
		break;
	    case 200:
//		int count = 0;
//		for (MapleMapObject ob_mob : player.getMap().getAllMonster()) {
//		    if (((MapleMonster) ob_mob).getId() == 0) {
//			count++;
//		    }
//		}
		stop = player.getMap().getAllMonster().size() >= limit;
		break;
	}
	return stop;
    }

    public void applyEffect(ChannelCharacter player, Monster monster, boolean skill) {
	Disease disease = null;
	Map<MonsterStatus, Integer> stats = Maps.newEnumMap(MonsterStatus.class);
	List<Integer> reflection = new LinkedList<>();

	switch (skillId) {
	    case 100:
	    case 110:
		stats.put(MonsterStatus.WEAPON_ATTACK_UP, Integer.valueOf(x));
		break;
	    case 101:
	    case 111:
		stats.put(MonsterStatus.MAGIC_ATTACK_UP, Integer.valueOf(x));
		break;
	    case 102:
	    case 112:
		stats.put(MonsterStatus.WEAPON_DEFENSE_UP, Integer.valueOf(x));
		break;
	    case 103:
	    case 113:
		stats.put(MonsterStatus.MAGIC_DEFENSE_UP, Integer.valueOf(x));
		break;
	    case 114:
		if (lt != null && rb != null && skill) {
		    List<GameMapObject> objects = getObjectsInRange(monster, GameMapObjectType.MONSTER);
		    final int hp = (getX() / 1000) * (int) (950 + 1050 * Math.random());
		    for (GameMapObject mons : objects) {
			((Monster) mons).heal(hp, getY(), true);
		    }
		} else {
		    monster.heal(getX(), getY(), true);
		}
		break;
	    case 120:
		disease = Disease.SEAL;
		break;
	    case 121:
		disease = Disease.DARKNESS;
		break;
	    case 122:
		disease = Disease.WEAKEN;
		break;
	    case 123:
		disease = Disease.STUN;
		break;
	    case 124:
		disease = Disease.CURSE;
		break;
	    case 125:
		disease = Disease.POISON;
		break;
	    case 126: // Slow
		disease = Disease.SLOW;
		break;
	    case 127:
		if (lt != null && rb != null && skill) {
		    for (ChannelCharacter character : getPlayersInRange(monster, player)) {
			character.dispel();
		    }
		} else {
		    player.dispel();
		}
		break;
	    case 128: // Seduce
		disease = Disease.SEDUCE;
		break;
	    case 129: // Banish
		final BanishInfo info = monster.getStats().getBanishInfo();
		if (lt != null && rb != null && skill) {
		    for (ChannelCharacter chr : getPlayersInRange(monster, player)) {
			chr.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
		    }
		} else {
		    player.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
		}
		break;
	    case 131: // Mist
		monster.getMap().spawnMist(new Mist(calculateBoundingBox(monster.getPosition(), true), monster, this), x * 10, false, false);
		break;
	    case 132:
		disease = Disease.REVERSE_DIRECTION;
		break;
	    case 133:
		disease = Disease.ZOMBIFY;
		break;
	    case 140:
		if (makeChanceResult()) {
		    stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
		}
		break;
	    case 141:
		if (makeChanceResult()) {
		    stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
		}
		break;
	    case 143: // Weapon Reflect
		stats.put(MonsterStatus.WEAPON_DAMAGE_REFLECT, Integer.valueOf(x));
		stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
		reflection.add(x);
		break;
	    case 144: // Magic Reflect
		stats.put(MonsterStatus.MAGIC_DAMAGE_REFLECT, Integer.valueOf(x));
		stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
		reflection.add(x);
		break;
	    case 145: // Weapon / Magic reflect
		stats.put(MonsterStatus.WEAPON_DAMAGE_REFLECT, Integer.valueOf(x));
		stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
		stats.put(MonsterStatus.MAGIC_DAMAGE_REFLECT, Integer.valueOf(x));
		stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
		reflection.add(x);
		reflection.add(x);
		break;
	    case 200:
		for (Integer mobId : getSummons()) {
		    final Monster toSpawn = LifeFactory.getMonster(mobId);
		    toSpawn.setPosition(monster.getPosition());
		    int ypos = (int) monster.getPosition().getY(), xpos = (int) monster.getPosition().getX();

		    switch (mobId) {
			case 8500003: // Pap bomb high
			    toSpawn.setFoothold((int) Math.ceil(Math.random() * 19.0));
			    ypos = -590;
			    break;
			case 8500004: // Pap bomb
			    //Spawn between -500 and 500 from the monsters X position
			    xpos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
			    ypos = (int) monster.getPosition().getY();
			    break;
			case 8510100: //Pianus bomb
			    if (Math.ceil(Math.random() * 5) == 1) {
				ypos = 78;
				xpos = (int) (0 + Math.ceil(Math.random() * 5)) + ((Math.ceil(Math.random() * 2) == 1) ? 180 : 0);
			    } else {
				xpos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
			    }
			    break;
		    }
		    // Get spawn coordinates (This fixes monster lock)
		    // TODO get map left and right wall.
		    switch (monster.getMap().getId()) {
			case 220080001: //Pap map
			    if (xpos < -890) {
				xpos = (int) (-890 + Math.ceil(Math.random() * 150));
			    } else if (xpos > 230) {
				xpos = (int) (230 - Math.ceil(Math.random() * 150));
			    }
			    break;
			case 230040420: // Pianus map
			    if (xpos < -239) {
				xpos = (int) (-239 + Math.ceil(Math.random() * 150));
			    } else if (xpos > 371) {
				xpos = (int) (371 - Math.ceil(Math.random() * 150));
			    }
			    break;
		    }
		    monster.getMap().spawnMonsterWithEffect(toSpawn, getSpawnEffect(), monster.getMap().calcPointBelow(new Point(xpos, ypos - 1)));
		}
		break;
	}

	if (stats.size() > 0) {
	    if (lt != null && rb != null && skill) {
		for (GameMapObject mons : getObjectsInRange(monster, GameMapObjectType.MONSTER)) {
		    ((Monster) mons).applyMonsterBuff(stats, getX(), getSkillId(), getDuration(), this, reflection);
		}
	    } else {
		monster.applyMonsterBuff(stats, getX(), getSkillId(), getDuration(), this, reflection);
	    }
	}
	if (disease != null) {
	    if (lt != null && rb != null && skill) {
		for (ChannelCharacter chr : getPlayersInRange(monster, player)) {
		    chr.giveDebuff(disease, this);
		}
	    } else {
		player.giveDebuff(disease, this);
	    }
	}
	monster.setMp(monster.getMp() - getMpCon());
    }

    public int getSkillId() {
	return skillId;
    }

    public int getSkillLevel() {
	return skillLevel;
    }

    public int getMpCon() {
	return mpCon;
    }

    public List<Integer> getSummons() {
	return Collections.unmodifiableList(toSummon);
    }

    /*    public short getEffectDelay() {
    return effect_delay;
    }*/
    public int getSpawnEffect() {
	return spawnEffect;
    }

    public int getHP() {
	return hp;
    }

    public int getX() {
	return x;
    }

    public int getY() {
	return y;
    }

    public long getDuration() {
	return duration;
    }

    public long getCoolTime() {
	return cooltime;
    }

    public Point getLt() {
	return lt;
    }

    public Point getRb() {
	return rb;
    }

    public int getLimit() {
	return limit;
    }

    public boolean makeChanceResult() {
	return prop == 1.0 || Math.random() < prop;
    }

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
	Point mylt, myrb;
	if (facingLeft) {
	    mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
	    myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
	} else {
	    myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
	    mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
	}
	final Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
	return bounds;
    }

    private List<ChannelCharacter> getPlayersInRange(Monster monster, ChannelCharacter player) {
	final Rectangle bounds = calculateBoundingBox(monster.getPosition(), monster.isFacingLeft());
	List<ChannelCharacter> players = new ArrayList<>();
	players.add(player);
	return monster.getMap().getPlayersInRect(bounds, players);
    }

    private List<GameMapObject> getObjectsInRange(Monster monster, GameMapObjectType objectType) {
	final Rectangle bounds = calculateBoundingBox(monster.getPosition(), monster.isFacingLeft());
	List<GameMapObjectType> objectTypes = new ArrayList<>();
	objectTypes.add(objectType);
	return monster.getMap().getMapObjectsInRect(bounds, objectTypes);
    }
}
