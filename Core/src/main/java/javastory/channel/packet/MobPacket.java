package javastory.channel.packet;

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javastory.channel.client.MonsterStatus;
import javastory.channel.client.MonsterStatusEffect;
import javastory.channel.life.Monster;
import javastory.channel.movement.LifeMovementFragment;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;

public class MobPacket {

	public static GamePacket damageMonster(final int oid, final int damage) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
		builder.writeInt(oid);
		builder.writeAsByte(0);
		builder.writeInt(damage);

		return builder.getPacket();
	}

	public static GamePacket damageFriendlyMob(final Monster mob, final int damage) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
		builder.writeInt(mob.getObjectId());
		builder.writeAsByte(1);
		builder.writeInt(damage);
		builder.writeInt(mob.getHp());
		builder.writeInt(mob.getMobMaxHp());

		return builder.getPacket();
	}

	public static GamePacket killMonster(final int oid, final int animation) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.KILL_MONSTER.getValue());
		builder.writeInt(oid);
		builder.writeAsByte(animation); // 0 = dissapear, 1 = fade out, 2+ =
										// special

		return builder.getPacket();
	}

	public static GamePacket healMonster(final int oid, final int heal) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
		builder.writeInt(oid);
		builder.writeAsByte(1);
		builder.writeInt(-heal);

		return builder.getPacket();
	}

	public static GamePacket showMonsterHP(final int oid, final int remhppercentage) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_MONSTER_HP.getValue());
		builder.writeInt(oid);
		builder.writeAsByte(remhppercentage);

		return builder.getPacket();
	}

	public static GamePacket showBossHP(final Monster mob) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsByte(5);
		builder.writeInt(mob.getId());
		builder.writeInt(mob.getHp());
		builder.writeInt(mob.getMobMaxHp());
		builder.writeByte(mob.getStats().getTagColor());
		builder.writeByte(mob.getStats().getTagBgColor());

		return builder.getPacket();
	}

	public static GamePacket moveMonster(final boolean useskill, final int skill, final int skill1, final int skill2, final int skill3, final int skill4, final int oid, final Point startPos,
		final List<LifeMovementFragment> moves) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MOVE_MONSTER.getValue());
		builder.writeInt(oid);
		builder.writeAsShort(0);
		builder.writeAsByte(useskill ? 1 : 0);
		builder.writeAsByte(skill);
		builder.writeAsByte(skill1);
		builder.writeAsByte(skill2);
		builder.writeAsByte(skill3);
		builder.writeAsByte(skill4);
		builder.writeZeroBytes(8);
		builder.writeVector(startPos);
		builder.writeInt(4275593);
		serializeMovementList(builder, moves);

		return builder.getPacket();
	}

	private static void serializeMovementList(final PacketBuilder builder, final List<LifeMovementFragment> moves) {
		builder.writeAsByte(moves.size());
		for (final LifeMovementFragment move : moves) {
			move.serialize(builder);
		}
	}

	public static GamePacket spawnMonster(final Monster life, final int spawnType, final int effect, final int link) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER.getValue());
		builder.writeInt(life.getObjectId());
		builder.writeAsByte(1); // 1 = Control normal, 5 = Control none
		builder.writeInt(life.getId());
		builder.writeZeroBytes(15);
		builder.writeAsByte(0x88);
		builder.writeZeroBytes(6);
		builder.writeAsShort(life.getPosition().x);
		builder.writeAsShort(life.getPosition().y);
		builder.writeAsByte(life.getStance());
		builder.writeAsShort(0); // FH
		builder.writeAsShort(life.getFoothold()); // Origin FH
		if (effect != 0 || link != 0) {
			builder.writeAsByte(effect != 0 ? effect : -3);
			builder.writeInt(link);
		} else {
			if (spawnType == 0) {
				builder.writeAsByte(effect);
				builder.writeAsByte(0);
				builder.writeAsShort(0);
			}
			builder.writeAsByte(spawnType); // newSpawn ? -2 : -1
		}
		builder.writeByte(life.getCarnivalTeam());
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket controlMonster(final Monster life, final boolean newSpawn, final boolean aggro) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
		builder.writeAsByte(aggro ? 2 : 1);
		builder.writeInt(life.getObjectId());
		builder.writeAsByte(1); // 1 = Control normal, 5 = Control none
		builder.writeInt(life.getId());
		builder.writeZeroBytes(15);
		builder.writeAsByte(0x88);
		builder.writeZeroBytes(6);
		builder.writeAsShort(life.getPosition().x);
		builder.writeAsShort(life.getPosition().y);
		builder.writeAsByte(life.getStance()); // Bitfield
		builder.writeAsShort(0); // FH
		builder.writeAsShort(life.getFoothold()); // Origin FH
		builder.writeAsByte(life.isFake() ? 0xfc : newSpawn ? -2 : -1);
		builder.writeByte(life.getCarnivalTeam());
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket stopControllingMonster(final int oid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
		builder.writeAsByte(0);
		builder.writeInt(oid);

		return builder.getPacket();
	}

	public static GamePacket makeMonsterInvisible(final Monster life) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
		builder.writeAsByte(0);
		builder.writeInt(life.getObjectId());

		return builder.getPacket();
	}

	public static GamePacket makeMonsterReal(final Monster life) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER.getValue());
		builder.writeInt(life.getObjectId());
		builder.writeAsByte(1); // 1 = Control normal, 5 = Control none
		builder.writeInt(life.getId());
		builder.writeZeroBytes(15); // Added on v.82 MSEA
		builder.writeAsByte(0x88);
		builder.writeZeroBytes(6);
		builder.writeAsShort(life.getPosition().x);
		builder.writeAsShort(life.getPosition().y);
		builder.writeAsByte(life.getStance());
		builder.writeAsShort(0); // FH
		builder.writeAsShort(life.getFoothold()); // Origin FH
		builder.writeAsShort(-1);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket moveMonsterResponse(final int objectid, final short moveid, final int currentMp, final boolean useSkills, final int skillId, final int skillLevel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
		builder.writeInt(objectid);
		builder.writeAsShort(moveid);
		builder.writeAsByte(useSkills ? 1 : 0);
		builder.writeAsShort(currentMp);
		builder.writeAsByte(skillId);
		builder.writeAsByte(skillLevel);

		return builder.getPacket();
	}

	private static long getSpecialLongMask(final Collection<MonsterStatus> statups) {
		long mask = 0;
		for (final MonsterStatus statup : statups) {
			if (statup.isFirst()) {
				mask |= statup.getValue();
			}
		}
		return mask;
	}

	private static long getLongMask(final Collection<MonsterStatus> statups) {
		long mask = 0;
		for (final MonsterStatus statup : statups) {
			if (!statup.isFirst()) {
				mask |= statup.getValue();
			}
		}
		return mask;
	}

	private static void writeIntMask(final PacketBuilder builder, final Map<MonsterStatus, Integer> stats) {
		builder.writeLong(getSpecialLongMask(stats.keySet()));
		builder.writeLong(getLongMask(stats.keySet()));
	}

	public static GamePacket applyMonsterStatus(final int oid, final MonsterStatusEffect mse) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.APPLY_MONSTER_STATUS.getValue());
		builder.writeInt(oid);
		writeIntMask(builder, mse.getEffects());
		for (final Map.Entry<MonsterStatus, Integer> stat : mse.getEffects().entrySet()) {
			builder.writeAsShort(stat.getValue());
			if (mse.isMonsterSkill()) {
				builder.writeAsShort(mse.getMobSkill().getSkillId());
				builder.writeAsShort(mse.getMobSkill().getSkillLevel());
			} else {
				builder.writeInt(mse.getSkill().getId());
			}
			builder.writeAsShort(0); // might actually be the buffTime but it's
										// not displayed anywhere
		}
		builder.writeAsShort(0); // delay in ms
		builder.writeAsByte(mse.getEffects().size()); // size
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket applyMonsterStatus(final int oid, final MonsterStatusEffect mse, final List<Integer> reflection) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.APPLY_MONSTER_STATUS.getValue());
		builder.writeInt(oid);
		writeIntMask(builder, mse.getEffects());
		for (final Map.Entry<MonsterStatus, Integer> stat : mse.getEffects().entrySet()) {
			builder.writeAsShort(stat.getValue());
			if (mse.isMonsterSkill()) {
				builder.writeAsShort(mse.getMobSkill().getSkillId());
				builder.writeAsShort(mse.getMobSkill().getSkillLevel());
			} else {
				builder.writeInt(mse.getSkill().getId());
			}
			builder.writeAsShort(0); // might actually be the buffTime but it's
										// not displayed anywhere
		}
		for (final Integer ref : reflection) {
			builder.writeInt(ref);
		}
		builder.writeInt(0);
		builder.writeAsShort(0); // delay in ms
		int size = mse.getEffects().size(); // size
		if (reflection.size() > 0) {
			size /= 2; // This gives 2 buffs per reflection but it's really one
						// buff
		}
		builder.writeAsByte(size); // size
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket cancelMonsterStatus(final int oid, final Map<MonsterStatus, Integer> stats) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
		builder.writeInt(oid);
		writeIntMask(builder, stats);
		builder.writeAsByte(1); // reflector is 3~!??
		builder.writeAsByte(2);

		return builder.getPacket();
	}
}