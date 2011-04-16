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
package handling.channel.handler;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleClient;
import client.MapleCharacter;
import client.SkillFactory;
import client.SummonSkillEntry;
import client.status.MonsterStatusEffect;
import client.anticheat.CheatingOffense;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import server.MapleStatEffect;
import server.movement.LifeMovementFragment;
import server.life.MapleMonster;
import server.life.SummonAttackEntry;
import server.maps.MapleMap;
import server.maps.MapleSummon;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SummonMovementType;
import tools.MaplePacketCreator;

public final class SummonHandler {

    private SummonHandler() {
    }

    public static void handleMoveDragon(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
        reader.skip(8); //POS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);
        if (chr.getDragon() != null) {
            final List<LifeMovementFragment> res2 = new ArrayList<LifeMovementFragment>(res);
            final Point pos = chr.getDragon().getPosition();
            MovementParse.updatePosition(res, chr.getDragon(), 0);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveDragon(chr.getDragon(), pos, res), chr.getPosition());
            }
        }
    }

    public static void handleSummonMove(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
        final int oid = reader.readInt();
        reader.skip(8);
        final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);

        for (MapleSummon sum : chr.getSummons().values()) {
            if (sum.getObjectId() == oid && sum.getMovementType() != SummonMovementType.STATIONARY) {
                final Point startPos = sum.getPosition();
                MovementParse.updatePosition(res, sum, 0);
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveSummon(chr.getId(), oid, startPos, res), sum.getPosition());
                break;
            }
        }
    }

    public static void handleSummonDamage(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
        final int unkByte = reader.readByte();
        final int damage = reader.readInt();
        final int monsterIdFrom = reader.readInt();
        //       reader.readByte(); // stance

        final Iterator<MapleSummon> iter = chr.getSummons().values().iterator();
        MapleSummon summon;

        while (iter.hasNext()) {
            summon = iter.next();
            if (summon.isPuppet() && summon.getOwnerId() == chr.getId()) { //We can only have one puppet(AFAIK O.O) so this check is safe.
                summon.addHP((short) -damage);
                if (summon.getHP() <= 0) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.damageSummon(chr.getId(), summon.getSkill(), damage, unkByte, monsterIdFrom), summon.getPosition());
                break;
            }
        }
    }

    public static void handleSummonAttack(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        if (!chr.isAlive()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        final MapleMap map = chr.getMap();
        final MapleMapObject obj = map.getMapObject(reader.readInt());
        if (obj == null || !obj.getType().equals(MapleMapObjectType.SUMMON)) {
            return;
        }
        final MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != chr.getId()) {
            return;
        }
        final SummonSkillEntry sse = SkillFactory.getSummonData(summon.getSkill());
        if (sse == null) {
            return;
        }
        reader.skip(8);
        summon.CheckSummonAttackFrequency(chr, reader.readInt());
        reader.skip(8);
        final byte animation = reader.readByte();
        reader.skip(8);
        final byte numAttacked = reader.readByte();
        if (numAttacked > sse.mobCount) {
            //AutobanManager.getInstance().autoban(c, "Attacking more monster that summon can do (Skillid : "+summon.getSkill()+" Count : " + numAttacked + ", allowed : " + sse.mobCount + ")");
            return;
        }
        reader.skip(8);
        final List<SummonAttackEntry> allDamage = new ArrayList<SummonAttackEntry>();
        chr.getCheatTracker().checkSummonAttack();

        for (int i = 0; i < numAttacked; i++) {
            final MapleMonster mob = map.getMonsterByOid(reader.readInt());

            if (mob == null) {
                continue;
            }
            if (chr.getPosition().distanceSq(mob.getPosition()) > 250000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
            }
            reader.skip(18); // who knows
            final int damage = reader.readInt();
            allDamage.add(new SummonAttackEntry(mob, damage));
        }
        map.broadcastMessage(chr, MaplePacketCreator.summonAttack(summon.getOwnerId(), summon.getSkill(), animation, allDamage, chr.getLevel()), summon.getPosition());

        final ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());

        for (SummonAttackEntry attackEntry : allDamage) {
            final int toDamage = attackEntry.getDamage();
            final MapleMonster mob = attackEntry.getMonster();

            if (toDamage > 0 && summonEffect.getMonsterStati().size() > 0) {
                if (summonEffect.makeChanceResult()) {
                    mob.applyStatus(chr, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, null, false), summonEffect.isPoison(), 4000, false);
                }
            }
            if (chr.isGM() || toDamage < 60000) {
                mob.damage(chr, toDamage, true);
                chr.checkMonsterAggro(mob);
            } else {
                //AutobanManager.getInstance().autoban(c, "High Summon Damage (" + toDamage + " to " + attackEntry.getMonster().getId() + ")");
                // TODO : Check player's stat for damage checking.
            }
        }
    }
}
