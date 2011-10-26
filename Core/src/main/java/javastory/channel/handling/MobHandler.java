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
package javastory.channel.handling;

import java.awt.Point;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.life.MobSkill;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMap;
import javastory.channel.movement.LifeMovementFragment;
import javastory.channel.packet.MobPacket;
import javastory.game.SkillLevelEntry;
import javastory.game.data.MobSkillFactory;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.Randomizer;

public class MobHandler {

	public static void handleMoveMonster(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final Monster monster = chr.getMap().getMonsterByOid(reader.readInt());

		if (monster == null) { // movin something which is not a monster
			return;
		}
		final short moveid = reader.readShort();
		final boolean useSkill = reader.readByte() > 0;
		final byte skill = reader.readByte();
		final int skill1 = reader.readByte() & 0xFF; // unsigned?
		final int skill2 = reader.readByte();
		final int skill3 = reader.readByte();
		final int skill4 = reader.readByte();
		int realskill = 0;
		int level = 0;

		if (useSkill) {// && (skill == -1 || skill == 0)) {
			final byte size = monster.getNoSkills();
			boolean used = false;

			if (size > 0) {
				final SkillLevelEntry skillToUse = monster.getSkills().get((byte) Randomizer.nextInt(size));
				realskill = skillToUse.skill;
				level = skillToUse.level;
				// Skill ID and Level
				final MobSkill mobSkill = MobSkillFactory.getMobSkill(realskill, level);

				if (!mobSkill.checkCurrentBuff(chr, monster)) {
					final long now = System.currentTimeMillis();
					final long ls = monster.getLastSkillUsed(realskill);

					if (ls == 0 || ((now - ls) > mobSkill.getCoolTime())) {
						monster.setLastSkillUsed(realskill, now, mobSkill.getCoolTime());

						final int reqHp = (int) (((float) monster.getHp() / monster.getMobMaxHp()) * 100);
						// In case this monster have 2.1b and above HP
						if (reqHp <= mobSkill.getHP()) {
							used = true;
							mobSkill.applyEffect(chr, monster, true);
						}
					}
				}
			}
			if (!used) {
				realskill = 0;
				level = 0;
			}
		}
		reader.skip(33);
		final Point startPos = monster.getPosition();
		final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);

		c.write(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(), monster.isControllerHasAggro(), realskill, level));

		if (res != null) {
			if (reader.remaining() != 9 && reader.remaining() != 17) {
				// 9.. 0 -> endPos? -> endPos again? -> 0 -> 0
				System.out.println("reader.available != 17 (movement parsing error)");
				c.disconnect(true);
				return;
			}

			/*
			 * reader.skip(1); final short fromx = reader.readShort(); final
			 * short fromy = reader.readShort(); final short tox =
			 * reader.readShort(); final short toy = reader.readShort();
			 * System.out
			 * .println("x1 : "+fromx+", y2 : "+fromy+" x2 : "+tox+", y2 : " +
			 * toy);
			 * System.out.println(map.getFootholds().checkRelevantFH(fromx,
			 * fromy, tox, toy));
			 */
			final GameMap map = c.getPlayer().getMap();

			MovementParse.updatePosition(res, monster, -1);
			map.moveMonster(monster, monster.getPosition());
			map.broadcastMessage(chr, MobPacket.moveMonster(useSkill, skill, skill1, skill2, skill3, skill4, monster.getObjectId(), startPos, res), monster
				.getPosition());
			chr.getCheatTracker().checkMoveMonster(monster.getPosition());
		}
	}

	public static void handleFriendlyDamage(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		final GameMap map = chr.getMap();
		final Monster mobfrom = map.getMonsterByOid(reader.readInt());
		reader.skip(4); // Player ID
		final Monster mobto = map.getMonsterByOid(reader.readInt());

		if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
			final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(99)) / 2; // Temp
																							// for
																							// now
																							// until
																							// I
																							// figure
																							// out
																							// something
																							// more
																							// effective
			mobto.damage(chr, damage, true);
		}
	}

	public static void handleMonsterBomb(final int oid, final ChannelCharacter chr) {
		final Monster monster = chr.getMap().getMonsterByOid(oid);

		if (monster == null || !chr.isAlive() || chr.isHidden()) {
			return;
		}
		final byte selfd = monster.getStats().getSelfD();
		if (selfd != -1) {
			chr.getMap().killMonster(monster, chr, false, false, selfd);
		}
	}

	public static void handleAutoAggro(final int monsteroid, final ChannelCharacter chr) {
		final Monster monster = chr.getMap().getMonsterByOid(monsteroid);

		if (monster != null && chr.getPosition().distance(monster.getPosition()) < 200000) {
			if (monster.getController() != null) {
				if (chr.getMap().getCharacterById_InMap(monster.getController().getId()) == null) {
					monster.switchController(chr, true);
				} else {
					monster.switchController(monster.getController(), true);
				}
			} else {
				monster.switchController(chr, true);
			}
		}
	}

	public static void handleHypnotizeDamage(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		final Monster mob_from = chr.getMap().getMonsterByOid(reader.readInt()); // From
		reader.skip(4); // Player ID
		final int to = reader.readInt(); // mobto
		reader.skip(1); // Same as player damage, -1 = bump, integer = skill ID
		final int damage = reader.readInt();
//	reader.skip(1); // Facing direction
//	reader.skip(4); // Some type of pos, damage display, I think

		final Monster mob_to = chr.getMap().getMonsterByOid(to);

		if (mob_from != null && mob_to != null) {
			if (damage > 30000) {
				return;
			}
			mob_to.damage(chr, damage, true);
			// TODO : Get the real broadcast damage packet
			chr.getMap().broadcastMessage(chr, MobPacket.damageMonster(to, damage), false);
		}
	}
}
