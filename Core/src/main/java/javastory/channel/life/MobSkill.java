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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.channel.client.Disease;
import javastory.channel.client.MonsterStatus;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.Mist;
import javastory.game.BanishInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MobSkill {

	private final int skillId, skillLevel;
	private int mpCon;
	private int spawnEffect;
	private int hp;
	private int x;
	private int y;
	private long duration, cooltime;
	private float prop;
	// private short effect_delay;
	private short limit;
	private List<Integer> toSummon = Lists.newArrayList();
	private Point lt, rb;

	public MobSkill(final int skillId, final int level) {
		this.skillId = skillId;
		this.skillLevel = level;
	}

	public void setMpCon(final int mpCon) {
		this.mpCon = mpCon;
	}

	public void addSummons(final List<Integer> toSummon) {
		this.toSummon = toSummon;
	}

//	public void setEffectDelay(short effect_delay) { 
//	 	this.effect_delay =* effect_delay; 
//	}

	public void setSpawnEffect(final int spawnEffect) {
		this.spawnEffect = spawnEffect;
	}

	public void setHp(final int hp) {
		this.hp = hp;
	}

	public void setX(final int x) {
		this.x = x;
	}

	public void setY(final int y) {
		this.y = y;
	}

	public void setDuration(final long duration) {
		this.duration = duration;
	}

	public void setCoolTime(final long cooltime) {
		this.cooltime = cooltime;
	}

	public void setProp(final float prop) {
		this.prop = prop;
	}

	public void setLtRb(final Point lt, final Point rb) {
		this.lt = lt;
		this.rb = rb;
	}

	public void setLimit(final short limit) {
		this.limit = limit;
	}

	public boolean checkCurrentBuff(final ChannelCharacter player, final Monster monster) {
		boolean stop = false;
		switch (this.skillId) {
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
			// int count = 0;
			// for (MapleMapObject ob_mob : player.getMap().getAllMonster()) {
			// if (((MapleMonster) ob_mob).getId() == 0) {
			// count++;
			// }
			// }
			stop = player.getMap().getAllMonster().size() >= this.limit;
			break;
		}
		return stop;
	}

	public void applyEffect(final ChannelCharacter player, final Monster monster, final boolean skill) {
		Disease disease = null;
		final Map<MonsterStatus, Integer> stats = Maps.newEnumMap(MonsterStatus.class);
		final List<Integer> reflection = Lists.newLinkedList();

		switch (this.skillId) {
		case 100:
		case 110:
			stats.put(MonsterStatus.WEAPON_ATTACK_UP, Integer.valueOf(this.x));
			break;
		case 101:
		case 111:
			stats.put(MonsterStatus.MAGIC_ATTACK_UP, Integer.valueOf(this.x));
			break;
		case 102:
		case 112:
			stats.put(MonsterStatus.WEAPON_DEFENSE_UP, Integer.valueOf(this.x));
			break;
		case 103:
		case 113:
			stats.put(MonsterStatus.MAGIC_DEFENSE_UP, Integer.valueOf(this.x));
			break;
		case 114:
			if (this.lt != null && this.rb != null && skill) {
				final List<GameMapObject> objects = this.getObjectsInRange(monster, GameMapObjectType.MONSTER);
				final int hp = this.getX() / 1000 * (int) (950 + 1050 * Math.random());
				for (final GameMapObject mons : objects) {
					((Monster) mons).heal(hp, this.getY(), true);
				}
			} else {
				monster.heal(this.getX(), this.getY(), true);
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
			if (this.lt != null && this.rb != null && skill) {
				for (final ChannelCharacter character : this.getPlayersInRange(monster, player)) {
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
			if (this.lt != null && this.rb != null && skill) {
				for (final ChannelCharacter chr : this.getPlayersInRange(monster, player)) {
					chr.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
				}
			} else {
				player.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
			}
			break;
		case 131: // Mist
			monster.getMap().spawnMist(new Mist(this.calculateBoundingBox(monster.getPosition(), true), monster, this), this.x * 10, false, false);
			break;
		case 132:
			disease = Disease.REVERSE_DIRECTION;
			break;
		case 133:
			disease = Disease.ZOMBIFY;
			break;
		case 140:
			if (this.makeChanceResult()) {
				stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(this.x));
			}
			break;
		case 141:
			if (this.makeChanceResult()) {
				stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(this.x));
			}
			break;
		case 143: // Weapon Reflect
			stats.put(MonsterStatus.WEAPON_DAMAGE_REFLECT, Integer.valueOf(this.x));
			stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(this.x));
			reflection.add(this.x);
			break;
		case 144: // Magic Reflect
			stats.put(MonsterStatus.MAGIC_DAMAGE_REFLECT, Integer.valueOf(this.x));
			stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(this.x));
			reflection.add(this.x);
			break;
		case 145: // Weapon / Magic reflect
			stats.put(MonsterStatus.WEAPON_DAMAGE_REFLECT, Integer.valueOf(this.x));
			stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(this.x));
			stats.put(MonsterStatus.MAGIC_DAMAGE_REFLECT, Integer.valueOf(this.x));
			stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(this.x));
			reflection.add(this.x);
			reflection.add(this.x);
			break;
		case 200:
			for (final Integer mobId : this.getSummons()) {
				final Monster toSpawn = LifeFactory.getMonster(mobId);
				toSpawn.setPosition(monster.getPosition());
				int ypos = (int) monster.getPosition().getY(), xpos = (int) monster.getPosition().getX();

				switch (mobId) {
				case 8500003: // Pap bomb high
					toSpawn.setFoothold((int) Math.ceil(Math.random() * 19.0));
					ypos = -590;
					break;
				case 8500004: // Pap bomb
					// Spawn between -500 and 500 from the monsters X position
					xpos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
					ypos = (int) monster.getPosition().getY();
					break;
				case 8510100: // Pianus bomb
					if (Math.ceil(Math.random() * 5) == 1) {
						ypos = 78;
						xpos = (int) (0 + Math.ceil(Math.random() * 5)) + (Math.ceil(Math.random() * 2) == 1 ? 180 : 0);
					} else {
						xpos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
					}
					break;
				}
				// Get spawn coordinates (This fixes monster lock)
				// TODO get map left and right wall.
				switch (monster.getMap().getId()) {
				case 220080001: // Pap map
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
				monster.getMap().spawnMonsterWithEffect(toSpawn, this.getSpawnEffect(), monster.getMap().calcPointBelow(new Point(xpos, ypos - 1)));
			}
			break;
		}

		if (stats.size() > 0) {
			if (this.lt != null && this.rb != null && skill) {
				for (final GameMapObject mons : this.getObjectsInRange(monster, GameMapObjectType.MONSTER)) {
					((Monster) mons).applyMonsterBuff(stats, this.getX(), this.getSkillId(), this.getDuration(), this, reflection);
				}
			} else {
				monster.applyMonsterBuff(stats, this.getX(), this.getSkillId(), this.getDuration(), this, reflection);
			}
		}
		if (disease != null) {
			if (this.lt != null && this.rb != null && skill) {
				for (final ChannelCharacter chr : this.getPlayersInRange(monster, player)) {
					chr.giveDebuff(disease, this);
				}
			} else {
				player.giveDebuff(disease, this);
			}
		}
		monster.setMp(monster.getMp() - this.getMpCon());
	}

	public int getSkillId() {
		return this.skillId;
	}

	public int getSkillLevel() {
		return this.skillLevel;
	}

	public int getMpCon() {
		return this.mpCon;
	}

	public List<Integer> getSummons() {
		return Collections.unmodifiableList(this.toSummon);
	}

	/*
	 * public short getEffectDelay() { return effect_delay; }
	 */
	public int getSpawnEffect() {
		return this.spawnEffect;
	}

	public int getHP() {
		return this.hp;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public long getDuration() {
		return this.duration;
	}

	public long getCoolTime() {
		return this.cooltime;
	}

	public Point getLt() {
		return this.lt;
	}

	public Point getRb() {
		return this.rb;
	}

	public int getLimit() {
		return this.limit;
	}

	public boolean makeChanceResult() {
		return this.prop == 1.0 || Math.random() < this.prop;
	}

	private Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
		Point mylt, myrb;
		if (facingLeft) {
			mylt = new Point(this.lt.x + posFrom.x, this.lt.y + posFrom.y);
			myrb = new Point(this.rb.x + posFrom.x, this.rb.y + posFrom.y);
		} else {
			myrb = new Point(this.lt.x * -1 + posFrom.x, this.rb.y + posFrom.y);
			mylt = new Point(this.rb.x * -1 + posFrom.x, this.lt.y + posFrom.y);
		}
		final Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
		return bounds;
	}

	private List<ChannelCharacter> getPlayersInRange(final Monster monster, final ChannelCharacter player) {
		final Rectangle bounds = this.calculateBoundingBox(monster.getPosition(), monster.isFacingLeft());
		final List<ChannelCharacter> players = Lists.newArrayList();
		players.add(player);
		return monster.getMap().getPlayersInRect(bounds, players);
	}

	private List<GameMapObject> getObjectsInRange(final Monster monster, final GameMapObjectType objectType) {
		final Rectangle bounds = this.calculateBoundingBox(monster.getPosition(), monster.isFacingLeft());
		final List<GameMapObjectType> objectTypes = Lists.newArrayList();
		objectTypes.add(objectType);
		return monster.getMap().getMapObjectsInRect(bounds, objectTypes);
	}
}
