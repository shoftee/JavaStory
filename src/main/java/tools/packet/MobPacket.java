package tools.packet;

import java.util.Map;
import java.util.List;
import java.awt.Point;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import handling.GamePacket;
import handling.ServerPacketOpcode;
import java.util.Collection;
import server.life.MapleMonster;
import server.movement.LifeMovementFragment;
import org.javastory.io.PacketBuilder;

public class MobPacket {

    public static GamePacket damageMonster(final int oid, final int damage) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
        builder.writeInt(oid);
        builder.writeAsByte(0);
        builder.writeInt(damage);

        return builder.getPacket();
    }

    public static GamePacket damageFriendlyMob(final MapleMonster mob, final int damage) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
        builder.writeInt(mob.getObjectId());
        builder.writeAsByte(1);
        builder.writeInt(damage);
        builder.writeInt(mob.getHp());
        builder.writeInt(mob.getMobMaxHp());

        return builder.getPacket();
    }

    public static GamePacket killMonster(final int oid, final int animation) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.KILL_MONSTER.getValue());
        builder.writeInt(oid);
        builder.writeAsByte(animation); // 0 = dissapear, 1 = fade out, 2+ = special

        return builder.getPacket();
    }

    public static GamePacket healMonster(final int oid, final int heal) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DAMAGE_MONSTER.getValue());
        builder.writeInt(oid);
        builder.writeAsByte(1);
        builder.writeInt(-heal);

        return builder.getPacket();
    }

    public static GamePacket showMonsterHP(int oid, int remhppercentage) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_MONSTER_HP.getValue());
        builder.writeInt(oid);
        builder.writeAsByte(remhppercentage);

        return builder.getPacket();
    }

    public static GamePacket showBossHP(final MapleMonster mob) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
        builder.writeAsByte(5);
        builder.writeInt(mob.getId());
        builder.writeInt(mob.getHp());
        builder.writeInt(mob.getMobMaxHp());
        builder.writeByte(mob.getStats().getTagColor());
        builder.writeByte(mob.getStats().getTagBgColor());

        return builder.getPacket();
    }

    public static GamePacket moveMonster(boolean useskill, int skill, int skill1, int skill2, int skill3, int skill4, int oid, Point startPos, List<LifeMovementFragment> moves) {
        PacketBuilder builder = new PacketBuilder();

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

    private static void serializeMovementList(PacketBuilder builder, List<LifeMovementFragment> moves) {
        builder.writeAsByte(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(builder);
        }
    }

    public static GamePacket spawnMonster(MapleMonster life, int spawnType, int effect, int link) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsShort(life.getFh()); // Origin FH
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

    public static GamePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsShort(life.getFh()); // Origin FH
        builder.writeAsByte(life.isFake() ? 0xfc : newSpawn ? -2 : -1);
        builder.writeByte(life.getCarnivalTeam());
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket stopControllingMonster(int oid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        builder.writeAsByte(0);
        builder.writeInt(oid);

        return builder.getPacket();
    }

    public static GamePacket makeMonsterInvisible(MapleMonster life) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        builder.writeAsByte(0);
        builder.writeInt(life.getObjectId());

        return builder.getPacket();
    }

    public static GamePacket makeMonsterReal(MapleMonster life) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsShort(life.getFh()); // Origin FH
        builder.writeAsShort(-1);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        builder.writeInt(objectid);
        builder.writeAsShort(moveid);
        builder.writeAsByte(useSkills ? 1 : 0);
        builder.writeAsShort(currentMp);
        builder.writeAsByte(skillId);
        builder.writeAsByte(skillLevel);

        return builder.getPacket();
    }

    private static long getSpecialLongMask(Collection<MonsterStatus> statups) {
        long mask = 0;
        for (MonsterStatus statup : statups) {
            if (statup.isFirst()) {
                mask |= statup.getValue();
            }
        }
        return mask;
    }

    private static long getLongMask(Collection<MonsterStatus> statups) {
        long mask = 0;
        for (MonsterStatus statup : statups) {
            if (!statup.isFirst()) {
                mask |= statup.getValue();
            }
        }
        return mask;
    }

    private static void writeIntMask(PacketBuilder builder, Map<MonsterStatus, Integer> stats) {
        builder.writeLong(getSpecialLongMask(stats.keySet()));
        builder.writeLong(getLongMask(stats.keySet()));
    }

    public static GamePacket applyMonsterStatus(final int oid, final MonsterStatusEffect mse) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        builder.writeInt(oid);
        writeIntMask(builder, mse.getEffects());
        for (Map.Entry<MonsterStatus, Integer> stat : mse.getEffects().entrySet()) {
            builder.writeAsShort(stat.getValue());
            if (mse.isMonsterSkill()) {
                builder.writeAsShort(mse.getMobSkill().getSkillId());
                builder.writeAsShort(mse.getMobSkill().getSkillLevel());
            } else {
                builder.writeInt(mse.getSkill().getId());
            }
            builder.writeAsShort(0); // might actually be the buffTime but it's not displayed anywhere
        }
        builder.writeAsShort(0); // delay in ms
        builder.writeAsByte(mse.getEffects().size()); // size
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket applyMonsterStatus(final int oid, final MonsterStatusEffect mse, final List<Integer> reflection) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        builder.writeInt(oid);
        writeIntMask(builder, mse.getEffects());
        for (Map.Entry<MonsterStatus, Integer> stat : mse.getEffects().entrySet()) {
            builder.writeAsShort(stat.getValue());
            if (mse.isMonsterSkill()) {
                builder.writeAsShort(mse.getMobSkill().getSkillId());
                builder.writeAsShort(mse.getMobSkill().getSkillLevel());
            } else {
                builder.writeInt(mse.getSkill().getId());
            }
            builder.writeAsShort(0); // might actually be the buffTime but it's not displayed anywhere
        }
        for (Integer ref : reflection) {
            builder.writeInt(ref);
        }
        builder.writeInt(0);
        builder.writeAsShort(0); // delay in ms
        int size = mse.getEffects().size(); // size
        if (reflection.size() > 0) {
            size /= 2; // This gives 2 buffs per reflection but it's really one buff
        }
        builder.writeAsByte(size); // size
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        builder.writeInt(oid);
        writeIntMask(builder, stats);
        builder.writeAsByte(1); // reflector is 3~!??
        builder.writeAsByte(2);

        return builder.getPacket();
    }
}