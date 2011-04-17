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
package client;

import java.io.Serializable;

public enum BuffStat implements Serializable {

    DASH_SPEED(0x100000, true),
    DASH_JUMP(0x200000, true),
    MONSTER_RIDING(0x400000, true),
    HOMING_BEACON(0x1000000, true),
    ELEMENT_RESET(0x200000000L, true),
    ARAN_COMBO(0x1000000000L, true),
    COMBO_DRAIN(0x2000000000L, true),
    BODY_PRESSURE(0x8000000000L, true),
    SMART_KNOCKBACK(0x10000000000L, true),
    ENERGY_CHARGE(0x20000000000L, true),
    EXPRATE(0x40000000000L, true),
    SPEED_INFUSION(0x800000000000L, true),
    SOARING(0x4000000000000L, true),
    MIRROR_IMAGE(0x20000000000000L, true),
    OWL_SPIRIT(0x40000000000000L, true),
    MIRROR_TARGET(0x80000000000000L, true), //or 0x400000000000000L
    FINAL_CUT(0x100000000000000L, true),
    THORNS(0x200000000000000L, true),
    MORPH(0x2),
    RECOVERY(0x4),
    MAPLE_WARRIOR(0x8),
    STANCE(0x10),
    SHARP_EYES(0x20),
    MANA_REFLECTION(0x40),
    DRAGON_ROAR(0x80), // Stuns the user
    SPIRIT_CLAW(0x100),
    INFINITY(0x200),
    HOLY_SHIELD(0x400),
    HAMSTRING(0x800),
    BLIND(0x1000),
    CONCENTRATE(0x2000),
    UNKNOWN2(0x4000),
    ECHO_OF_HERO(0x8000),
    UNKNOWN3(0x10000),
    GHOST_MORPH(0x20000),
    ARIANT_COSS_IMU(0x40000), // The white ball around you
    REVERSE_DIR(0x80000), // No idea, we could just use disease x_X
    UNKNOWN6(0x1000000),
    UNKNOWN7(0x2000000),
    UNKNOWN8(0x4000000),
    BERSERK_FURY(0x8000000),
    DIVINE_BODY(0x10000000),
    UNKNOWN9(0x20000000),
    ARIANT_COSS_IMU2(0x40000000), // no idea, seems the same
    FINALATTACK(0x80000000),
    WATK(0x100000000L),
    WDEF(0x200000000L),
    MATK(0x400000000L),
    MDEF(0x800000000L),
    ACC(0x1000000000L),
    AVOID(0x2000000000L),
    HANDS(0x4000000000L),
    DASH(0x6000000000L),
    SPEED(0x8000000000L),
    JUMP(0x10000000000L),
    MAGIC_GUARD(0x20000000000L),
    DARKSIGHT(0x40000000000L),
    BOOSTER(0x80000000000L),
    POWERGUARD(0x100000000000L),
    MAXHP(0x200000000000L),
    MAXMP(0x400000000000L),
    INVINCIBLE(0x800000000000L),
    SOULARROW(0x1000000000000L),
    STUN(0x2000000000000L),
    POISON(0x4000000000000L),
    SEAL(0x8000000000000L),
    DARKNESS(0x10000000000000L),
    COMBO(0x20000000000000L),
    SUMMON(0x20000000000000L), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    WK_CHARGE(0x40000000000000L),
    DRAGONBLOOD(0x80000000000000L),
    HOLY_SYMBOL(0x100000000000000L),
    MESOUP(0x200000000000000L),
    SHADOWPARTNER(0x400000000000000L),
    PICKPOCKET(0x800000000000000L),
    PUPPET(0x800000000000000L), // HACK - shares buffmask with pickpocket - odin special ^.-
    MESOGUARD(0x1000000000000000L),
    UNKNOWN11(0x2000000000000000L),
    WEAKEN(0x4000000000000000L), //SWITCH_CONTROLS(0x8000000000000L)
    UNKNOWN12(0x8000000000000000L),;
    private static final long serialVersionUID = 0L;
    private final long buffstat;
    private final boolean first;

    private BuffStat(long buffstat) {
        this.buffstat = buffstat;
        first = false;
    }

    private BuffStat(long buffstat, boolean first) {
        this.buffstat = buffstat;
        this.first = first;
    }

    public final boolean isFirst() {
        return first;
    }

    public final long getValue() {
        return buffstat;
    }
}