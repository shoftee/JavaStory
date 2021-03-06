package javastory.channel.handling;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.BuffStat;
import javastory.channel.client.CancelCooldownAction;
import javastory.channel.client.ISkill;
import javastory.channel.client.KeyBinding;
import javastory.channel.client.SkillMacro;
import javastory.channel.life.MobSkill;
import javastory.channel.life.Monster;
import javastory.channel.maps.FieldLimitType;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.maps.ItemDropType;
import javastory.channel.movement.AbsoluteLifeMovement;
import javastory.channel.movement.LifeMovementFragment;
import javastory.channel.packet.MTSCSPacket;
import javastory.channel.packet.MobPacket;
import javastory.channel.server.AutobanManager;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Portal;
import javastory.channel.server.StatEffect;
import javastory.game.AttackType;
import javastory.game.GameConstants;
import javastory.game.Inventory;
import javastory.game.Item;
import javastory.game.Skills;
import javastory.game.data.ItemInfoProvider;
import javastory.game.data.MobAttackInfo;
import javastory.game.data.MobAttackInfoFactory;
import javastory.game.data.MobSkillFactory;
import javastory.game.data.SkillInfoProvider;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.server.TimerManager;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.UIPacket;

public class PlayerHandler {

	private static boolean isFinisher(final int skillid) {
		switch (skillid) {
		case 1111003:
		case 1111004:
		case 1111005:
		case 1111006:
		case 11111002:
		case 11111003:
			return true;
		}
		return false;
	}

	public static void handleChangeMonsterBookCover(final int bookid, final ChannelClient c, final ChannelCharacter chr) {
		if (bookid == 0 || GameConstants.isMonsterCard(bookid)) {
			chr.setMonsterBookCover(bookid);
			chr.getMonsterBook().updateCard(c, bookid);
		}
	}

	public static void handleSkillMacro(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		final int num = reader.readByte();
		String name;
		int shout, skill1, skill2, skill3;
		SkillMacro macro;

		for (int i = 0; i < num; i++) {
			name = reader.readLengthPrefixedString();
			shout = reader.readByte();
			skill1 = reader.readInt();
			skill2 = reader.readInt();
			skill3 = reader.readInt();

			macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
			chr.getSkillMacros().update(i, macro);
		}
	}

	public static void HandleChangeKeymap(final PacketReader reader, final ChannelCharacter chr) {
		if (reader.remaining() != 8) {
			try {
				// else = pet auto pot
				reader.skip(4);
				final int numChanges = reader.readInt();

				int key, type, action;
				KeyBinding newbinding;

				for (int i = 0; i < numChanges; i++) {
					key = reader.readInt();
					type = reader.readByte();
					action = reader.readInt();
					newbinding = new KeyBinding(type, action);
					chr.changeKeybinding(key, newbinding);
				}
			} catch (final PacketFormatException ex) {
				Logger.getLogger(PlayerHandler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void handleUseChair(final int itemId, final ChannelClient c, final ChannelCharacter chr) {
		final Item toUse = chr.getSetupInventory().findById(itemId);

		if (toUse == null || toUse.getItemId() != itemId) {
			chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
			return;
		}
		if (itemId == 3011000) {
			for (final Item item : c.getPlayer().getCashInventory()) {
				if (item.getItemId() == 5340000) {
					chr.startFishingTask(false);
					break;
				} else if (item.getItemId() == 5340001) {
					chr.startFishingTask(true);
					break;
				}
			}
		}
		chr.setChair(itemId);
		chr.getMap().broadcastMessage(chr, ChannelPackets.showChair(chr.getId(), itemId), false);
		c.write(ChannelPackets.enableActions());
	}

	public static void handleCancelChair(final short id, final ChannelClient c, final ChannelCharacter chr) {
		if (id == -1) { // Cancel Chair
			if (chr.getChair() == 3011000) {
				chr.cancelFishingTask();
			}
			chr.setChair(0);
			c.write(ChannelPackets.cancelChair(-1));
			chr.getMap().broadcastMessage(chr, ChannelPackets.showChair(chr.getId(), 0), false);
		} else { // Use In-Map Chair
			chr.setChair(id);
			c.write(ChannelPackets.cancelChair(id));
		}
	}

	public static void handleTeleportRockAddMap(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte addrem = reader.readByte();
		final byte vip = reader.readByte();

		if (addrem == 0) {
			chr.deleteFromRocks(reader.readInt());
		} else if (addrem == 1) {
			if (chr.getMap().getForcedReturnId() == 999999999) {
				chr.addRockMap();
			}
		}
		c.write(MTSCSPacket.getTrockRefresh(chr, vip == 1, addrem == 3));
	}

	public static void handleCharacterInfoRequest(final int objectid, final ChannelClient c, final ChannelCharacter chr) {
		final ChannelCharacter player = (ChannelCharacter) c.getPlayer().getMap().getMapObject(objectid);

		if (player != null) {
			if (!player.isGM() || c.getPlayer().isGM() && player.isGM()) {
				c.write(ChannelPackets.charInfo(player, c.getPlayer().equals(player)));
			} else {
				c.write(ChannelPackets.enableActions());
			}
		}
	}

	public static void handleTakeDamage(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4); // Ticks
		final byte type = reader.readByte();
		reader.skip(1); // Element - 0x00 = elementless, 0x01 = ice, 0x02 =
						// fire, 0x03 = lightning
		int damage = reader.readInt();

		int oid = 0;
		int monsteridfrom = 0;
		final int reflect = 0;
		byte direction = 0;
		final int pos_x = 0;
		final int pos_y = 0;
		int fake = 0;
		int mpattack = 0;
		boolean is_pg = false;
		boolean isDeadlyAttack = false;
		Monster attacker = null;
		final ActivePlayerStats stats = chr.getStats();

		if (type != -2 && type != -3 && type != -4) { // Not map damage
			monsteridfrom = reader.readInt();
			oid = reader.readInt();
			attacker = chr.getMap().getMonsterByOid(oid);
			direction = reader.readByte();

			if (attacker == null) {
				return;
			}
			if (type != -1) { // Bump damage
				final MobAttackInfo attackInfo = MobAttackInfoFactory.getInstance().getMobAttackInfo(attacker.getId(), type);
				if (attackInfo.IsDeadlyAttack) {
					isDeadlyAttack = true;
					mpattack = stats.getMp() - 1;
				} else {
					mpattack += attackInfo.MpBurn;
				}
				final MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.DiseaseSkill, attackInfo.DiseaseLevel);
				if (skill != null && (damage == -1 || damage > 0)) {
					skill.applyEffect(chr, attacker, false);
				}
				attacker.setMp(attacker.getMp() - attackInfo.MpCon);
			}
		}

		if (damage == -1) {
			fake = 4020002 + (chr.getJobId() / 10 - 40) * 100000;
		} else if (damage < -1 || damage > 60000) {
			AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
			return;
		}
		chr.getCheatTracker().checkTakeDamage(damage);

		if (damage > 0) {
			chr.getCheatTracker().setAttacksWithoutHit(false);

			if (chr.getBuffedValue(BuffStat.MORPH) != null) {
				chr.cancelMorphs();
			}

			if (type == -1) {
				if (chr.getBuffedValue(BuffStat.POWERGUARD) != null) {
					attacker = (Monster) chr.getMap().getMapObject(oid);
					if (attacker != null) {
						int bouncedamage = (int) (damage * (chr.getBuffedValue(BuffStat.POWERGUARD).doubleValue() / 100));
						bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
						attacker.damage(chr, bouncedamage, true);
						damage -= bouncedamage;
						chr.getMap().broadcastMessage(chr, MobPacket.damageMonster(oid, bouncedamage), chr.getPosition());
						is_pg = true;
					}
				}
			} else if (type != -2 && type != -3 && type != -4) {
				switch (chr.getJobId()) {
				case 112: {
					final ISkill skill = SkillInfoProvider.getSkill(1120004);
					if (chr.getCurrentSkillLevel(skill) > 0) {
						damage = (int) (skill.getEffect(chr.getCurrentSkillLevel(skill)).getX() / 1000.0 * damage);
					}
					break;
				}
				case 122: {
					final ISkill skill = SkillInfoProvider.getSkill(1220005);
					if (chr.getCurrentSkillLevel(skill) > 0) {
						damage = (int) (skill.getEffect(chr.getCurrentSkillLevel(skill)).getX() / 1000.0 * damage);
					}
					break;
				}
				case 132: {
					final ISkill skill = SkillInfoProvider.getSkill(1320005);
					if (chr.getCurrentSkillLevel(skill) > 0) {
						damage = (int) (skill.getEffect(chr.getCurrentSkillLevel(skill)).getX() / 1000.0 * damage);
					}
					break;
				}
				}
			}

			if (chr.getBuffedValue(BuffStat.MAGIC_GUARD) != null) {
				int hploss = 0, mploss = 0;
				if (isDeadlyAttack) {
					if (stats.getHp() > 1) {
						hploss = stats.getHp() - 1;
					}
					if (stats.getMp() > 1) {
						mploss = stats.getMp() - 1;
					}
					chr.addMPHP(-hploss, -mploss);
				} else if (mpattack > 0) {
					chr.addMPHP(-damage, -mpattack);
				} else {
					mploss = (int) (damage * (chr.getBuffedValue(BuffStat.MAGIC_GUARD).doubleValue() / 100.0));
					hploss = damage - mploss;

					if (mploss > stats.getMp()) {
						hploss += mploss - stats.getMp();
						mploss = stats.getMp();
					}
					chr.addMPHP(-hploss, -mploss);
				}

			} else if (chr.getBuffedValue(BuffStat.MESOGUARD) != null) {
				damage = damage % 2 == 0 ? damage / 2 : damage / 2 + 1;

				final int mesoloss = (int) (damage * (chr.getBuffedValue(BuffStat.MESOGUARD).doubleValue() / 100.0));
				if (chr.getMeso() < mesoloss) {
					chr.gainMeso(-chr.getMeso(), false);
					chr.cancelBuffStats(BuffStat.MESOGUARD);
				} else {
					chr.gainMeso(-mesoloss, false);
				}
				if (isDeadlyAttack && stats.getMp() > 1) {
					mpattack = stats.getMp() - 1;
				}
				chr.addMPHP(-damage, -mpattack);
			} else {
				if (isDeadlyAttack) {
					chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, stats.getMp() > 1 ? -(stats.getMp() - 1) : 0);
				} else {
					chr.addMPHP(-damage, -mpattack);
				}
			}
		}
		if (!chr.isHidden()) {
			chr.getMap().broadcastMessage(chr,
				ChannelPackets.damagePlayer(type, monsteridfrom, chr.getId(), damage, fake, direction, reflect, is_pg, oid, pos_x, pos_y), false);
		}
	}

	public static void handleAranCombo(final ChannelClient c, final ChannelCharacter chr) {
		if (chr.getJobId() >= 2000 && chr.getJobId() <= 2112) {
			short combo = chr.getCombo();
			final long curr = System.currentTimeMillis();
			if (combo > 0 && curr - chr.getLastCombo() > 5000) {
				// Official MS timing is 2.5 seconds, so 5 seconds should be
				// safe.
				chr.getCheatTracker().registerOffense(CheatingOffense.ARAN_COMBO_HACK);
			}
			if (combo < 30000) {
				combo++;
			}
			chr.setLastCombo(curr);
			chr.setCombo(combo);
			c.write(ChannelPackets.testCombo(combo));
			switch (combo) { // Hackish method xD
			case 10:
			case 20:
			case 30:
			case 40:
			case 50:
			case 60:
			case 70:
			case 80:
			case 90:
			case 100:
				SkillInfoProvider.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
				break;
			}
		}
	}

	public static void handleUseItemEffect(final int itemId, final ChannelClient c, final ChannelCharacter chr) {
		final Item toUse = chr.getCashInventory().findById(itemId);
		if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		chr.setItemEffect(itemId);
		chr.getMap().broadcastMessage(chr, ChannelPackets.itemEffect(chr.getId(), itemId), false);
	}

	public static void handleCancelItemEffect(final int id, final ChannelCharacter chr) {
		chr.cancelEffect(ItemInfoProvider.getInstance().getItemEffect(-id), false, -1);
	}

	public static void handleCancelBuff(final int sourceid, final ChannelCharacter chr) {
		final ISkill skill = SkillInfoProvider.getSkill(sourceid);

		if (skill.isChargeSkill()) {
			chr.setKeyDownSkill_Time(0);
			chr.getMap().broadcastMessage(chr, ChannelPackets.skillCancel(chr, sourceid), false);
		} else {
			chr.cancelEffect(SkillInfoProvider.getSkill(sourceid).getEffect(1), false, -1);
		}
	}

	public static void handleSkillEffect(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		final int skillId = reader.readInt();
		final byte level = reader.readByte();
		final byte flags = reader.readByte();
		final byte speed = reader.readByte();
		final byte unk = reader.readByte(); // Added on v.82

		final ISkill skill = SkillInfoProvider.getSkill(skillId);
		final int skilllevel_serv = chr.getCurrentSkillLevel(skill);

		if (skilllevel_serv > 0 && skilllevel_serv == level && skill.isChargeSkill() && level > 0) {
			chr.setKeyDownSkill_Time(System.currentTimeMillis());
			chr.getMap().broadcastMessage(chr, ChannelPackets.skillEffect(chr, skillId, level, flags, speed, unk), false);
		}
	}

	public static void handleSpecialMove(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		reader.skip(4); // Old X and Y
		final int skillid = reader.readInt();
		final int skillLevel = reader.readByte();
		final ISkill skill = SkillInfoProvider.getSkill(skillid);

		if (chr.getCurrentSkillLevel(skill) == 0 || chr.getCurrentSkillLevel(skill) != skillLevel) {
			if (!Skills.isMulungSkill(skillid)) {
				c.disconnect(true);
				return;
			}
			if (chr.getMapId() / 10000 != 92502) {
				// AutobanManager.getInstance().autoban(c,
				// "Using Mu Lung dojo skill out of dojo maps.");
			} else {
				chr.mulung_EnergyModify(false);
			}
		}
		final StatEffect effect = skill.getEffect(chr.getCurrentSkillLevel(skill));

		if (effect.getCooldown() > 0) {
			if (chr.isInCooldown(skillid)) {
				c.write(ChannelPackets.enableActions());
				return;
			}
			if (skillid != 5221006) { // Battleship
				c.write(ChannelPackets.skillCooldown(skillid, effect.getCooldown()));
				final ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(chr, skillid), effect.getCooldown() * 1000);
				chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
			}
		}
		final ChannelCharacter player = c.getPlayer();

		switch (skillid) {
		case 1121001:
		case 1221001:
		case 1321001:
		case 9001020: // GM magnet
			final byte number_of_mobs = reader.readByte();
			reader.skip(3);
			for (int i = 0; i < number_of_mobs; i++) {
				final int mobId = reader.readInt();

				final Monster mob = chr.getMap().getMonsterByOid(mobId);
				if (mob != null) {
//			chr.getMap().broadcastMessage(chr, MaplePacketCreator.showMagnet(mobId, reader.readByte()), chr.getPosition());
					mob.switchController(chr, mob.isControllerHasAggro());
				}
			}
			chr.getMap().broadcastMessage(chr, ChannelPackets.showBuffeffect(chr.getId(), skillid, 1, reader.readByte()), chr.getPosition());
			c.write(ChannelPackets.enableActions());
			break;
		default:
			Point pos = null;
			if (reader.remaining() == 7) {
				pos = reader.readVector();
			}
			if (skill.getId() == 2311002) { // Mystic Door
				if (chr.canDoor()) {
					if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
						effect.applyTo(player, pos);
					} else {
						c.write(ChannelPackets.enableActions());
					}
				} else {
					chr.sendNotice(5, "Please wait 5 seconds before casting Mystic Door again.");
					c.write(ChannelPackets.enableActions());
				}
			} else {
				if (StatEffect.parseMountInfo(player, skill.getId()) != 0 && player.getBuffedValue(BuffStat.MONSTER_RIDING) == null
					&& player.getDragon() != null) {
					player.getMap().broadcastMessage(ChannelPackets.removeDragon(player.getId()));
					player.getMap().removeMapObject(player.getDragon());
					player.setDragon(null);
				}
				effect.applyTo(player, pos);
			}
			break;
		}
	}

	public static void handleMeleeAttack(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
			return;
		}
		final AttackInfo attack = DamageParse.parseDmgM(reader);

		double maxdamage = chr.getStats().getCurrentMaxBaseDamage();
		final boolean mirror = chr.getBuffedValue(BuffStat.MIRROR_IMAGE) != null;
		int attackCount = chr.getJobId() >= 430 && chr.getJobId() <= 434 ? 2 : 1, skillLevel = 0;
		StatEffect effect = null;
		ISkill skill = null;

		if (attack.skill != 0) {
			skill = SkillInfoProvider.getSkill(Skills.getLinkedAranSkill(attack.skill));
			skillLevel = chr.getCurrentSkillLevel(skill);
			if (skillLevel == 0) {
				c.disconnect(true);
				return;
			}
			effect = attack.getAttackEffect(chr, skillLevel, skill);
			maxdamage *= effect.getDamage() / 100.0;
			attackCount = effect.getAttackCount();

			if (effect.getCooldown() > 0) {
				if (chr.isInCooldown(attack.skill)) {
					c.write(ChannelPackets.enableActions());
					return;
				}
				c.write(ChannelPackets.skillCooldown(attack.skill, effect.getCooldown()));
				chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(
					new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
			}
		}
		attackCount *= mirror ? 2 : 1;
		// handle combo orbconsume
		int numFinisherOrbs = 0;
		final Integer comboBuff = chr.getBuffedValue(BuffStat.COMBO);

		if (isFinisher(attack.skill)) { // finisher
			if (comboBuff != null) {
				numFinisherOrbs = comboBuff.intValue() - 1;
			}
			chr.handleOrbconsume();

		} else if (attack.targets > 0 && comboBuff != null) {
			// handle combo orbgain
			switch (chr.getJobId()) {
			case 111:
			case 112:
			case 1110:
			case 1111:
				if (attack.skill != 1111008) { // shout should not give orbs
					chr.handleOrbgain();
				}
				break;
			}
		}
		switch (chr.getJobId()) {
		case 511:
		case 512: {
			chr.handleEnergyCharge(5110001, attack.targets);
			break;
		}
		case 1510:
		case 1511:
		case 1512: {
			chr.handleEnergyCharge(15100004, attack.targets);
			break;
		}
		}
		// handle sacrifice hp loss
		if (attack.targets > 0 && attack.skill == 1211002) { // handle charged
																// blow
			final int advcharge_level = chr.getCurrentSkillLevel(SkillInfoProvider.getSkill(1220010));
			if (advcharge_level > 0) {
				if (!SkillInfoProvider.getSkill(1220010).getEffect(advcharge_level).makeChanceResult()) {
					chr.cancelEffectFromBuffStat(BuffStat.WK_CHARGE);
				}
			} else {
				chr.cancelEffectFromBuffStat(BuffStat.WK_CHARGE);
			}
		}
		final ChannelCharacter player = c.getPlayer();

		if (numFinisherOrbs > 0) {
			maxdamage *= numFinisherOrbs;
		} else if (comboBuff != null) {
			ISkill combo;
			if (player.getJobId() == 1110 || player.getJobId() == 1111) {
				combo = SkillInfoProvider.getSkill(11111001);
			} else {
				combo = SkillInfoProvider.getSkill(1111002);
			}
			maxdamage *= 1.0 + (combo.getEffect(player.getCurrentSkillLevel(combo)).getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
		}

		if (isFinisher(attack.skill)) {
			if (numFinisherOrbs == 0) {
				return;
			}
			maxdamage = ChannelCharacter.getDamageCap(); // FIXME reenable
															// damage
															// calculation for
															// finishers
		}
		DamageParse.applyAttack(attack, skill, player, attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);

		chr.getMap().broadcastMessage(
			chr,
			ChannelPackets.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed,
				attack.allDamage, chr.getLevel()), chr.getPosition());
	}

	public static void handleRangedAttack(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
			return;
		}
		final AttackInfo attack = DamageParse.parseDmgR(reader);

		int bulletCount = 1, skillLevel = 0;
		StatEffect effect = null;
		ISkill skill = null;

		if (attack.skill != 0) {
			skill = SkillInfoProvider.getSkill(attack.skill);
			skillLevel = chr.getCurrentSkillLevel(skill);
			if (skillLevel == 0) {
				c.disconnect(true);
				return;
			}
			effect = attack.getAttackEffect(chr, skillLevel, skill);

			switch (attack.skill) {
			case 21110004: // Ranged but uses attackcount instead
			case 14101006: // Vampure
				bulletCount = effect.getAttackCount();
				break;
			default:
				bulletCount = effect.getBulletCount();
				break;
			}
			if (effect.getCooldown() > 0) {
				if (chr.isInCooldown(attack.skill)) {
					c.write(ChannelPackets.enableActions());
					return;
				}
				c.write(ChannelPackets.skillCooldown(attack.skill, effect.getCooldown()));
				chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(
					new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
			}
		}
		final Integer ShadowPartner = chr.getBuffedValue(BuffStat.SHADOWPARTNER);
		if (ShadowPartner != null) {
			bulletCount *= 2;
		}
		int projectile = 0, visProjectile = 0;
		if (attack.AOE != 0 && chr.getBuffedValue(BuffStat.SOULARROW) == null && attack.skill != 4111004) {
			projectile = chr.getUseInventory().getItem(attack.slot).getItemId();

			if (attack.csstar > 0) {
				visProjectile = chr.getCashInventory().getItem(attack.csstar).getItemId();
			} else {
				visProjectile = projectile;
			}
			// Handle bulletcount
			if (chr.getBuffedValue(BuffStat.SPIRIT_CLAW) == null) {
				int bulletConsume = bulletCount;
				if (effect != null && effect.getBulletConsume() != 0) {
					bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
				}
				InventoryManipulator.removeById(c, chr.getUseInventory(), projectile, bulletConsume, false, true);
			}
		}

		double basedamage;
		int projectileWatk = 0;
		if (projectile != 0) {
			projectileWatk = ItemInfoProvider.getInstance().getWatkForProjectile(projectile);
		}
		final ActivePlayerStats statst = chr.getStats();
		switch (attack.skill) {
		case 4001344: // Lucky Seven
		case 4121007: // Triple Throw
		case 14001004: // Lucky seven
		case 14111005: // Triple Throw
			basedamage = statst.getTotalLuk() * 5.0f * (statst.getTotalWatk() + projectileWatk) / 100;
			break;
		case 4111004: // Shadow Meso
//		basedamage = ((effect.getMoneyCon() * 10) / 100) * effect.getProb(); // Not sure
			basedamage = 13000;
			break;
		default:
			if (projectileWatk != 0) {
				basedamage = statst.calculateMaxBaseDamage(statst.getTotalWatk() + projectileWatk);
			} else {
				basedamage = statst.getCurrentMaxBaseDamage();
			}
			switch (attack.skill) {
			case 3101005: // arrowbomb is hardcore like that
				basedamage *= effect.getX() / 100.0;
				break;
			}
			break;
		}
		if (effect != null) {
			basedamage *= effect.getDamage() / 100.0;

			int money = effect.getMoneyCon();
			if (money != 0) {
				if (money > chr.getMeso()) {
					money = chr.getMeso();
				}
				chr.gainMeso(-money, false);
			}
		}
		DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null
			? AttackType.RANGED_WITH_SHADOWPARTNER
			: AttackType.RANGED);

		chr.getMap().broadcastMessage(
			chr,
			ChannelPackets.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed, visProjectile,
				attack.allDamage, attack.position, chr.getLevel()), chr.getPosition());
	}

	public static void MagicDamage(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
			return;
		}
		final AttackInfo attack = DamageParse.parseDmgMa(reader);
		final ISkill skill = SkillInfoProvider.getSkill(attack.skill);
		final int skillLevel = chr.getCurrentSkillLevel(skill);
		if (skillLevel == 0) {
			c.disconnect(true);
			return;
		}
		final StatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);

		if (effect.getCooldown() > 0) {
			if (chr.isInCooldown(attack.skill)) {
				c.write(ChannelPackets.enableActions());
				return;
			}
			c.write(ChannelPackets.skillCooldown(attack.skill, effect.getCooldown()));
			chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(
				new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
		}
		DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect);

		chr.getMap().broadcastMessage(
			chr,
			ChannelPackets.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed, attack.allDamage,
				attack.charge, chr.getLevel()), chr.getPosition());
	}

	public static void handleWheelOfFortuneEffect(final int itemId, final ChannelCharacter chr) {
		// BA 03 00 00 72 01 00 00 << extra int
		switch (itemId) {
		case 5510000: {
			if (!chr.isAlive()) {
				chr.getMap().broadcastMessage(chr, ChannelPackets.showSpecialEffect(chr.getId(), itemId), false);
			}
			break;
		}
		default:
			System.out.println("Unhandled item effect [WheelOfFortuneHandler] id : " + itemId);
			break;
		}
	}

	public static void handleMesoDrop(final int meso, final ChannelCharacter chr) {
		if (!chr.isAlive() || meso < 10 || meso > 50000 || meso > chr.getMeso()) {
			chr.getClient().write(ChannelPackets.enableActions());
			return;
		}
		chr.gainMeso(-meso, false, true);
		chr.getMap().spawnMesoDrop(meso, chr.getPosition(), chr, chr, true, ItemDropType.DEFAULT);
	}

	public static void handleFaceExpression(final int emote, final ChannelCharacter chr) {
		if (emote > 7) {
			final int emoteid = 5159992 + emote;
			final Inventory inventory = chr.getInventoryForItem(emoteid);
			if (inventory.findById(emoteid) == null) {
				chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(emoteid));
				return;
			}
		}
		if (emote > 0) {
			chr.getMap().broadcastMessage(chr, ChannelPackets.facialExpression(chr, emote), false);
		}
	}

	public static void handleHealthRegeneration(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4);
		final int healHP = reader.readShort();
		final int healMP = reader.readShort();
		final ActivePlayerStats stats = chr.getStats();
		if (stats.getHp() <= 0) {
			return;
		}
		if (healHP != 0) {
			if (healHP > stats.getHealHP()) {
				// chr.getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_HP,
				// String.valueOf(healHP));
			}
			chr.addHP(healHP);
		}
		if (healMP != 0) {
			if (healMP > stats.getHealMP()) {
				chr.getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_MP, String.valueOf(healMP));
			}
			chr.addMP(healMP);
		}
	}

	public static void handleMovePlayer(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
//	reader.skip(5); // unknown
		final Point Original_Pos = chr.getPosition(); // 4 bytes Added on v.80
														// MSEA
		reader.skip(37);

		// log.trace("Movement command received: unk1 {} unk2 {}", new Object[]
		// { unk1, unk2 });
		final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);

		if (res != null) { 
			// TODO more validation of input data
			if (reader.remaining() != 18) {
				// System.out.println("reader.remaining != 18 (movement parsing error)");
				return;
			}
			final GameMap map = c.getPlayer().getMap();

			if (chr.isHidden()) {
				chr.setLastRes(res);
			} else {
				speedCheck(res, c);
				map.broadcastMessage(chr, ChannelPackets.movePlayer(chr.getId(), res, Original_Pos), false);
			}
			MovementParse.updatePosition(res, chr, 0);
			map.movePlayer(chr, chr.getPosition());

			/*
			 * int count = c.getPlayer().getFallCounter(); if
			 * (map.getFootholds().findBelow(c.getPlayer().getPosition()) ==
			 * null) { if (count > 3) { c.getPlayer().changeMap(map,
			 * map.getPortal(0)); } else {
			 * c.getPlayer().setFallCounter(++count); } } else if (count > 0) {
			 * c.getPlayer().setFallCounter(0); }
			 */
		}
	}

	private static void speedCheck(final List<LifeMovementFragment> res, final ChannelClient c) {
		double speedMod;
		final double playerSpeedMod = c.getPlayer().getStats().getSpeedMod() + 0.005;
		for (final LifeMovementFragment lmf : res) {
			if (lmf.getClass() == AbsoluteLifeMovement.class) {
				final AbsoluteLifeMovement alm = (AbsoluteLifeMovement) lmf;
				speedMod = Math.abs(alm.getPixelsPerSecond().x) / 125.0;
				if (speedMod > playerSpeedMod) {
					if (alm.getUnk() != 0) {
						if (speedMod > playerSpeedMod) {
							c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.FAST_MOVE);
							break;
						}
					}
				}
			}
		}
	}

	public static void handleChangeMapSpecial(final String portal_name, final ChannelClient c, final ChannelCharacter chr) {
		final Portal portal = chr.getMap().getPortal(portal_name);
//	reader.skip(2);

		if (portal != null) {
			portal.enterPortal(c);
		}
	}

	public static void handleChangeMap(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (reader.remaining() != 0) {
			reader.skip(7); // 1 = from dying 2 = regular portals
			final int targetid = reader.readInt(); // FF FF FF FF
			final Portal portal = chr.getMap().getPortal(reader.readLengthPrefixedString());
			if (reader.remaining() >= 7) {

				reader.skip(4); // 2F 03 04 00

			}
			reader.skip(1);
			final boolean wheel = reader.readShort() > 0;
			final GameMapFactory mapFactory = ChannelServer.getMapFactory();

			if (targetid != -1 && !chr.isAlive()) {
				if (chr.getEventInstance() != null) {
					chr.getEventInstance().revivePlayer(chr);
				}
				chr.setStance(0);

				if (!wheel) {
					chr.getStats().setHp(50);

					final GameMap to = chr.getMap().getReturnMap();
					chr.changeMap(to, to.getPortal(0));
				} else {
					if (chr.haveItem(5510000, 1, false, true)) { // Wheel of
																	// Fortune
						chr.getStats().setHp(chr.getStats().getMaxHp() / 100 * 40);
						InventoryManipulator.removeById(c, chr.getCashInventory(), 5510000, 1, true, false);

						final GameMap to = chr.getMap();
						chr.changeMap(to, to.getPortal(0));
					} else {
						c.disconnect();
					}
				}
			} else if (targetid != -1 && chr.isGM()) {
				final GameMap to = mapFactory.getMap(targetid);
				chr.changeMap(to, to.getPortal(0));

			} else if (targetid != -1 && !chr.isGM()) {
				final int divi = chr.getMapId() / 100;
				if (divi == 9130401) { // Only allow warp if player is already
										// in Intro map, or else = hack

					if (targetid == 130000000 || targetid / 100 == 9130401) { // Cygnus
																				// introduction
						final GameMap to = mapFactory.getMap(targetid);
						chr.changeMap(to, to.getPortal(0));
					}
				} else if (divi == 9140900) { // Aran Introduction
					if (targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000) {
						final GameMap to = mapFactory.getMap(targetid);
						chr.changeMap(to, to.getPortal(0));
					}
				} else if (divi == 9140901 && targetid == 140000000) {
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (divi == 9140902 && (targetid == 140030000 || targetid == 140000000)) { // thing
																									// is.
																									// dont
																									// really
																									// know
																									// which
																									// one!
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (divi == 9000900 && targetid / 100 == 9000900 && targetid > chr.getMapId()) {
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (divi / 1000 == 9000 && targetid / 100000 == 9000) {
					if (targetid < 900090000 || targetid > 900090004) { // 1
																		// movie
						c.write(UIPacket.IntroDisableUI(false));
						c.write(UIPacket.IntroLock(false));
						c.write(ChannelPackets.enableActions());
					}
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (divi / 10 == 1020 && targetid == 1020000) { // Adventurer
																		// movie
																		// clip
																		// Intro
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));

				} else if (chr.getMapId() == 900090101 && targetid == 100030100) {
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (chr.getMapId() == 2010000 && targetid == 104000000) {
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (chr.getMapId() == 106020001 || chr.getMapId() == 106020502) {
					if (targetid == chr.getMapId() - 1) {
						c.write(UIPacket.IntroDisableUI(false));
						c.write(UIPacket.IntroLock(false));
						c.write(ChannelPackets.enableActions());
						final GameMap to = mapFactory.getMap(targetid);
						chr.changeMap(to, to.getPortal(0));
					}
				} else if (chr.getMapId() == 0 && targetid == 10000) {
					c.write(UIPacket.IntroDisableUI(false));
					c.write(UIPacket.IntroLock(false));
					c.write(ChannelPackets.enableActions());
					final GameMap to = mapFactory.getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				}
			} else {
				if (portal != null) {
					portal.enterPortal(c);
				} else {
					c.write(ChannelPackets.enableActions());
				}
			}
		}
	}

	public static void handleUseInnerPortal(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final Portal portal = c.getPlayer().getMap().getPortal(reader.readLengthPrefixedString());
		final Point target = reader.readVector();

		if (portal == null) {
			c.disconnect();
			return;
		} else if (portal.getPosition().distanceSq(chr.getPosition()) > 22500) {
			chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
		}
		chr.getMap().movePlayer(chr, target);
	}
}
