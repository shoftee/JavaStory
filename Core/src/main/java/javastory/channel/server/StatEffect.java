package javastory.channel.server;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.BuffStat;
import javastory.channel.client.Disease;
import javastory.channel.client.ISkill;
import javastory.channel.client.MonsterStatus;
import javastory.channel.client.MonsterStatusEffect;
import javastory.channel.client.SkillFactory;
import javastory.channel.life.Monster;
import javastory.channel.maps.Door;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.Mist;
import javastory.channel.maps.Summon;
import javastory.channel.maps.SummonMovementType;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.client.Stat;
import javastory.game.GameConstants;
import javastory.server.BuffStatValue;
import javastory.server.StatValue;
import javastory.server.TimerManager;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.PlayerCooldownValueHolder;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public class StatEffect implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private byte mastery, mhpR, mmpR, mobCount, attackCount, bulletCount;
    private short hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, mpCon, hpCon, damage, prop;
    private double hpR, mpR;
    private int duration, sourceid, moveTo, x, y, z, itemCon, itemConNo, bulletConsume, moneyCon, cooldown, morphId = 0, expinc;
    private boolean overTime, skill;
    private List<BuffStatValue> statups;
    private Map<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
//    private List<Pair<Integer, Integer>> randomMorph;
    private List<Disease> cureDebuffs;

    public static StatEffect loadSkillEffectFromData(final WzData source, final int skillid, final boolean overtime) {
        return loadFromData(source, skillid, true, overtime);
    }

    public static StatEffect loadItemEffectFromData(final WzData source, final int itemid) {
        return loadFromData(source, itemid, false, false);
    }

    private static void addBuffStatPairToListIfNotZero(final List<BuffStatValue> list, final BuffStat buffstat, final Integer val) {
        if (val.intValue() != 0) {
            list.add(new BuffStatValue(buffstat, val));
        }
    }

    private static StatEffect loadFromData(final WzData source, final int sourceid, final boolean skill, final boolean overTime) {
        final StatEffect ret = new StatEffect();
        ret.duration = WzDataTool.getIntConvert("time", source, -1);
        ret.hp = (short) WzDataTool.getInt("hp", source, 0);
        ret.hpR = WzDataTool.getInt("hpR", source, 0) / 100.0;
        ret.mp = (short) WzDataTool.getInt("mp", source, 0);
        ret.mpR = WzDataTool.getInt("mpR", source, 0) / 100.0;
        ret.mhpR = (byte) WzDataTool.getInt("mhpR", source, 0);
        ret.mmpR = (byte) WzDataTool.getInt("mmpR", source, 0);
        ret.mpCon = (short) WzDataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) WzDataTool.getInt("hpCon", source, 0);
        ret.prop = (short) WzDataTool.getInt("prop", source, 100);
        ret.cooldown = WzDataTool.getInt("cooltime", source, 0);
        ret.expinc = WzDataTool.getInt("expinc", source, 0);
        ret.morphId = WzDataTool.getInt("morph", source, 0);
        ret.mobCount = (byte) WzDataTool.getInt("mobCount", source, 1);

        if (skill) {
            switch (sourceid) {
                case 1100002:
                case 1100003:
                case 1200002:
                case 1200003:
                case 1300002:
                case 1300003:
                case 3100001:
                case 3200001:
                case 11101002:
                case 13101002:
                    ret.mobCount = 6;
                    break;
            }
        }

        /*	final MapleData randMorph = source.getChildByPath("morphRandom");
        if (randMorph != null) {
        for (MapleData data : randMorph.getChildren()) {
        ret.randomMorph.add(new Pair(
        MapleDataTool.getInt("morph", data, 0),
        MapleDataTool.getIntConvert("prop", data, 0)));
        }
        }*/

        ret.sourceid = sourceid;
        ret.skill = skill;

        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course
            ret.overTime = overTime;
        }
        final ArrayList<BuffStatValue> statups = new ArrayList<>();

        ret.mastery = (byte) WzDataTool.getInt("mastery", source, 0);
        ret.watk = (short) WzDataTool.getInt("pad", source, 0);
        ret.wdef = (short) WzDataTool.getInt("pdd", source, 0);
        ret.matk = (short) WzDataTool.getInt("mad", source, 0);
        ret.mdef = (short) WzDataTool.getInt("mdd", source, 0);
        ret.acc = (short) WzDataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) WzDataTool.getInt("eva", source, 0);
        ret.speed = (short) WzDataTool.getInt("speed", source, 0);
        ret.jump = (short) WzDataTool.getInt("jump", source, 0);

        List<Disease> cure = new ArrayList<>(5);
        if (WzDataTool.getInt("poison", source, 0) > 0) {
            cure.add(Disease.POISON);
        }
        if (WzDataTool.getInt("seal", source, 0) > 0) {
            cure.add(Disease.SEAL);
        }
        if (WzDataTool.getInt("darkness", source, 0) > 0) {
            cure.add(Disease.DARKNESS);
        }
        if (WzDataTool.getInt("weakness", source, 0) > 0) {
            cure.add(Disease.WEAKEN);
        }
        if (WzDataTool.getInt("curse", source, 0) > 0) {
            cure.add(Disease.CURSE);
        }
        ret.cureDebuffs = cure;

        if (ret.overTime && ret.getSummonMovementType() == null) {
            addBuffStatPairToListIfNotZero(statups, BuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(statups, BuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(statups, BuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(statups, BuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(statups, BuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(statups, BuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(statups, BuffStat.SPEED, Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(statups, BuffStat.JUMP, Integer.valueOf(ret.jump));
            addBuffStatPairToListIfNotZero(statups, BuffStat.MAXHP, (int) ret.mhpR);
            addBuffStatPairToListIfNotZero(statups, BuffStat.MAXMP, (int) ret.mmpR);
//	    addBuffStatPairToListIfNotZero(statups, BuffStat.EXPRATE, Integer.valueOf(2)); // EXP
        }

        final WzData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }

        ret.x = WzDataTool.getInt("x", source, 0);
        ret.y = WzDataTool.getInt("y", source, 0);
        ret.z = WzDataTool.getInt("z", source, 0);
        ret.damage = (short) WzDataTool.getIntConvert("damage", source, 100);
        ret.attackCount = (byte) WzDataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = (byte) WzDataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = WzDataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = WzDataTool.getIntConvert("moneyCon", source, 0);

        ret.itemCon = WzDataTool.getInt("itemCon", source, 0);
        ret.itemConNo = WzDataTool.getInt("itemConNo", source, 0);
        ret.moveTo = WzDataTool.getInt("moveTo", source, -1);

        Map<MonsterStatus, Integer> monsterStatus = Maps.newEnumMap(MonsterStatus.class);

        if (skill) { // hack because we can't get from the datafile...
            switch (sourceid) {
                case 2001002: // magic guard
                case 12001001:
                case 22111001:
                    statups.add(new BuffStatValue(BuffStat.MAGIC_GUARD, ret.x));
                    break;
                case 2301003: // invincible
                    statups.add(new BuffStatValue(BuffStat.INVINCIBLE, ret.x));
                    break;
                case 9001004: // hide
                    ret.duration = 60 * 120 * 1000;
                    ret.overTime = true;
                    statups.add(new BuffStatValue(BuffStat.DARKSIGHT, ret.x));
                    break;
                case 13101006: // Wind Walk
                case 4001003: // darksight
                case 14001003: // cygnus ds
                case 4330001:
                    statups.add(new BuffStatValue(BuffStat.DARKSIGHT, ret.x));
                    break;
                case 4211003: // pickpocket
                    statups.add(new BuffStatValue(BuffStat.PICKPOCKET, ret.x));
                    break;
                case 4211005: // mesoguard
                    statups.add(new BuffStatValue(BuffStat.MESOGUARD, ret.x));
                    break;
                case 4111001: // mesoup
                    statups.add(new BuffStatValue(BuffStat.MESOUP, ret.x));
                    break;
                case 4111002: // shadowpartner
                case 14111000: // cygnus
                    statups.add(new BuffStatValue(BuffStat.SHADOWPARTNER, ret.x));
                    break;
                case 11101002: // All Final attack
                case 13101002:
                    statups.add(new BuffStatValue(BuffStat.FINALATTACK, 1));
                    break;
                case 3101004: // soul arrow
                case 3201004:
                case 2311002: // mystic door - hacked buff icon
                case 13101003:
                    statups.add(new BuffStatValue(BuffStat.SOULARROW, ret.x));
                    break;
                case 1211006: // wk charges
                case 1211003:
                case 1211004:
                case 1211005:
                case 1211008:
                case 1211007:
                case 1221003:
                case 1221004:
                case 11111007:
                case 15101006:
                case 21111005:
                    statups.add(new BuffStatValue(BuffStat.WK_CHARGE, ret.x));
                    break;
                case 12101005:
                case 22121001: // Elemental Reset
                    statups.add(new BuffStatValue(BuffStat.ELEMENT_RESET, ret.x));
                    break;
                case 5110001: // Energy Charge
                case 15100004:
                    statups.add(new BuffStatValue(BuffStat.ENERGY_CHARGE, 1));
                    break;
                case 1101005: // booster
                case 1101004:
                case 1201005:
                case 1201004:
                case 1301005:
                case 1301004:
                case 3101002:
                case 3201002:
                case 4101003:
                case 4201002:
                case 2111005: // spell booster, do these work the same?
                case 2211005:
                case 5101006:
                case 5201003:
                case 11101001:
                case 12101004:
                case 13101001:
                case 14101002:
                case 15101002:
                case 21001003: // Aran - Pole Arm Booster
                case 22141002: // Magic Booster
                case 4301002:
                    statups.add(new BuffStatValue(BuffStat.BOOSTER, ret.x));
                    break;
                //case 5121009:
                //case 15111005:
                //    statups.add(new Pair<MapleBuffStat, Integer>(BuffStat.SPEED_INFUSION, ret.x));
                //    break;
                case 4321000: //tornado spin uses same buffstats
                    ret.duration = 1000;
                    statups.add(new BuffStatValue(BuffStat.DASH_SPEED, 100 +
                            ret.x));
                    statups.add(new BuffStatValue(BuffStat.DASH_JUMP, ret.y)); //always 0 but its there
                    break;
                case 5001005: // Dash
                case 15001003:
                    statups.add(new BuffStatValue(BuffStat.DASH_SPEED, ret.x));
                    statups.add(new BuffStatValue(BuffStat.DASH_JUMP, ret.y));
                    break;
                case 1101007: // pguard
                case 1201007:
                    statups.add(new BuffStatValue(BuffStat.POWERGUARD, ret.x));
                    break;
                case 1301007: // hyper body
                case 9001008:
                    statups.add(new BuffStatValue(BuffStat.MAXHP, ret.x));
                    statups.add(new BuffStatValue(BuffStat.MAXMP, ret.y));
                    break;
                case 1001: // recovery
                    statups.add(new BuffStatValue(BuffStat.RECOVERY, ret.x));
                    break;
                case 1111002: // combo
                case 11111001: // combo
                    statups.add(new BuffStatValue(BuffStat.COMBO, 1));
                    break;
                case 5211006: // Homing Beacon
                case 5220011: // Bullseye
                case 22151002: //killer wings

                    ret.duration = 60 * 120000;
                    statups.add(new BuffStatValue(BuffStat.HOMING_BEACON, ret.x));
                    break;
                case 1011: // Berserk fury
                case 10001011:
                case 20001011:
                case 20011011:
                    statups.add(new BuffStatValue(BuffStat.BERSERK_FURY, 1));
                    break;
                case 1010:
                case 10001010:// Invincible Barrier
                case 20001010:
                case 20011010:
                    statups.add(new BuffStatValue(BuffStat.DIVINE_BODY, 1));
                    break;
                case 1311006: //dragon roar
                    ret.hpR = -ret.x / 100.0;
                    statups.add(new BuffStatValue(BuffStat.DRAGON_ROAR, ret.y));
                    break;
                case 1311008: // dragon blood
                    statups.add(new BuffStatValue(BuffStat.DRAGONBLOOD, ret.x));
                    break;
                case 4341007:
                    statups.add(new BuffStatValue(BuffStat.THORNS, ret.x <<
                            8 | ret.y));
                    break;
                case 4341002:
                    ret.duration = 60 * 1000;
                    ret.overTime = true;
                    ret.hpR = -ret.x / 100.0;
                    statups.add(new BuffStatValue(BuffStat.FINAL_CUT, ret.y));
                    break;
                case 4331002:
                    statups.add(new BuffStatValue(BuffStat.MIRROR_IMAGE, ret.x));
                    break;
                case 4331003:
                    ret.duration = 60 * 1000;
                    ret.overTime = true;
                    statups.add(new BuffStatValue(BuffStat.OWL_SPIRIT, ret.y));
                    break;
                case 1121000: // maple warrior, all classes
                case 1221000:
                case 1321000:
                case 2121000:
                case 2221000:
                case 2321000:
                case 3121000:
                case 3221000:
                case 4121000:
                case 4221000:
                case 5121000:
                case 5221000:
                case 21121000: // Aran - Maple Warrior
                case 22171000:
                case 4341000:
                    statups.add(new BuffStatValue(BuffStat.MAPLE_WARRIOR, ret.x));
                    break;
                case 3121002: // sharp eyes bow master
                case 3221002: // sharp eyes marksmen
                    statups.add(new BuffStatValue(BuffStat.SHARP_EYES, ret.x <<
                            8 | ret.y));
                    break;
                case 21101003: // Body Pressure
                    statups.add(new BuffStatValue(BuffStat.BODY_PRESSURE, ret.x));
                    break;
                case 21000000: // Aran Combo
                    statups.add(new BuffStatValue(BuffStat.ARAN_COMBO, 100));
                    break;
                case 21100005: // Combo Drain
                    statups.add(new BuffStatValue(BuffStat.COMBO_DRAIN, ret.x));
                    break;
                case 21111001: // Smart Knockback
                    statups.add(new BuffStatValue(BuffStat.SMART_KNOCKBACK, ret.x));
                    break;
                case 4001002: // disorder
                case 14001002: // cygnus disorder
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case 5221009: // Mind Control
                    monsterStatus.put(MonsterStatus.HYPNOTIZE, 1);
                    break;
                case 1201006: // threaten
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case 1211002: // charged blow
                case 1111008: // shout
                case 4211002: // assaulter
                case 3101005: // arrow bomb
                case 1111005: // coma: sword
                case 1111006: // coma: axe
                case 4221007: // boomerang step
                case 5101002: // Backspin Blow
                case 5101003: // Double Uppercut
                case 5121004: // Demolition
                case 5121005: // Snatch
                case 5121007: // Barrage
                case 5201004: // pirate blank shot
                case 4121008: // Ninja Storm
                case 22151001:
                case 4201004: //steal, new
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 4321002:
                    monsterStatus.put(MonsterStatus.DARKNESS, 1);
                    break;
                case 4221003:
                case 4121003:
                    monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    break;
                case 2201004: // cold beam
                case 2211002: // ice strike
                case 3211003: // blizzard
                case 2211006: // il elemental compo
                case 2221007: // Blizzard
                case 5211005: // Ice Splitter
                case 2121006: // Paralyze
                case 21120006: // Tempest
                case 22121000:
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    ret.duration *= 2; // freezing skills are a little strange
                    break;
                case 2101003: // fp slow
                case 2201003: // il slow
                case 12101001:
                case 22141003: // Slow
                    monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 2101005: // poison breath
                case 2111006: // fp elemental compo
                case 2121003: // ice demon
                case 2221003: // fire demon
                case 3111003: //inferno, new
                case 22161002: //phantom imprint
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    break;
                case 4121004: // Ninja ambush
                case 4221004:
                    monsterStatus.put(MonsterStatus.NINJA_AMBUSH, (int) ret.damage);
                    break;
                case 2311005:
                    monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                /*case 4341006:
                statups.add(new Pair<MapleBuffStat, Integer>(BuffStat.MIRROR_TARGET, 1));
                break;*/
                case 3111002: // puppet ranger
                case 3211002: // puppet sniper
                case 13111004: // puppet cygnus
                case 5211001: // Pirate octopus summon
                case 5220002: // wrath of the octopi
                    statups.add(new BuffStatValue(BuffStat.PUPPET, 1));
                    break;
                case 3211005: // golden eagle
                case 3111005: // golden hawk
                    statups.add(new BuffStatValue(BuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3221005: // frostprey
                case 2121005: // elquines
                    statups.add(new BuffStatValue(BuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                    break;
                case 2311006: // summon dragon
                case 3121006: // phoenix
                case 2221005: // ifrit
                case 2321003: // bahamut
                case 1321007: // Beholder
                case 5211002: // Pirate bird summon
                case 11001004:
                case 12001004:
                case 12111004: // Itrit
                case 13001004:
                case 14001005:
                    statups.add(new BuffStatValue(BuffStat.SUMMON, 1));
                    break;
                case 2311003: // hs
                case 9001002: // GM hs
                    statups.add(new BuffStatValue(BuffStat.HOLY_SYMBOL, ret.x));
                    break;
                case 2211004: // il seal
                case 2111004: // fp seal
                case 12111002: // cygnus seal
                    monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 4111003: // shadow web
                case 14111001:
                    monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case 4121006: // spirit claw
                    statups.add(new BuffStatValue(BuffStat.SPIRIT_CLAW, 0));
                    break;
                case 2121004:
                case 2221004:
                case 2321004: // Infinity
                    statups.add(new BuffStatValue(BuffStat.INFINITY, ret.x));
                    break;
                case 1121002:
                case 1221002:
                case 1321002: // Stance
                case 21121003: // Aran - Freezing Posture
                    statups.add(new BuffStatValue(BuffStat.STANCE, ret.prop));
                    break;
                case 1005: // Echo of Hero
                case 10001005: // Cygnus Echo
                case 20001005: // Aran
                    statups.add(new BuffStatValue(BuffStat.ECHO_OF_HERO, ret.x));
                    break;
                case 1026: // Soaring
                case 10001026: // Soaring
                case 20001026: // Soaring
                case 20011026: // Soaring
                    ret.duration = 60 * 120 * 1000; //because it seems to dispel asap.

                    ret.overTime = true;
                    statups.add(new BuffStatValue(BuffStat.SOARING, 1));
                    break;
                case 2121002: // mana reflection
                case 2221002:
                case 2321002:
                    statups.add(new BuffStatValue(BuffStat.MANA_REFLECTION, 1));
                    break;
                case 2321005: // holy shield
                    statups.add(new BuffStatValue(BuffStat.HOLY_SHIELD, ret.x));
                    break;
                case 3121007: // Hamstring
                    statups.add(new BuffStatValue(BuffStat.HAMSTRING, ret.x));
                    monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 3221006: // Blind
                    statups.add(new BuffStatValue(BuffStat.BLIND, ret.x));
                    monsterStatus.put(MonsterStatus.ACC, ret.x);
                    break;
                default:
                    break;
            }
        }
        if (ret.morphId > 0 || ret.isPirateMorph()) {
            statups.add(new BuffStatValue(BuffStat.MORPH, ret.getMorph()));
        }
        if (ret.isMonsterRiding()) {
            statups.add(new BuffStatValue(BuffStat.MONSTER_RIDING, 1));
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;

        return ret;
    }

    /**
     * @param applyto
     * @param obj
     * @param attack damage done by the skill
     */
    public final void applyPassive(final ChannelCharacter applyto, final GameMapObject obj) {
        if (makeChanceResult()) {
            switch (sourceid) { // MP eater
                case 2100000:
                case 2200000:
                case 2300000:
                    if (obj == null || obj.getType() !=
                            GameMapObjectType.MONSTER) {
                        return;
                    }
                    final Monster mob = (Monster) obj; // x is absorb percentage
                    if (!mob.getStats().isBoss()) {
                        final int absorbMp = Math.min((int) (mob.getMobMaxMp() *
                                (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.getStats().setMp(applyto.getStats().getMp() +
                                    absorbMp);
                            applyto.getClient().write(ChannelPackets.showOwnBuffEffect(sourceid, 1));
                            applyto.getMap().broadcastMessage(applyto, ChannelPackets.showBuffeffect(applyto.getId(), sourceid, 1), false);
                        }
                    }
                    break;
            }
        }
    }

    public final boolean applyTo(ChannelCharacter chr) {
        return applyTo(chr, chr, true, null);
    }

    public final boolean applyTo(ChannelCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos);
    }

    private boolean applyTo(final ChannelCharacter applyfrom, final ChannelCharacter applyto, final boolean primary, final Point pos) {
        /*	if (sourceid == 4341006 && applyfrom.getBuffedValue(BuffStat.MIRROR_IMAGE) == null) {
        return false;
        } */
        int hpchange = calcHPChange(applyfrom, primary);
        int mpchange = calcMPChange(applyfrom, primary);

        final ActivePlayerStats stat = applyto.getStats();

        if (primary) {
            if (itemConNo != 0) {
                InventoryManipulator.removeById(applyto.getClient(), applyto.getInventoryForItem(itemCon), itemCon, itemConNo, false, true);
            }
        } else if (!primary && isResurrection()) {
            hpchange = stat.getMaxHp();
            applyto.setStance(0); //TODO fix death bug, player doesnt spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isHeroWill()) {
            applyto.dispelDebuff(Disease.SEDUCE);
        } else if (cureDebuffs.size() > 0) {
            for (final Disease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        } else if (isMPRecovery()) {
            final int toDecreaseHP = ((stat.getMaxHp() / 100) * 10);
            if (stat.getHp() > toDecreaseHP) {
                hpchange += -toDecreaseHP; // -10% of max HP
            } else {
                hpchange = stat.getHp() == 1 ? 0 : stat.getHp() - 1;
            }
            mpchange += ((toDecreaseHP / 100) * getY());
        }
        final List<StatValue> hpmpupdate = new ArrayList<>(2);
        if (hpchange != 0) {
            if (hpchange < 0 && (-hpchange) > stat.getHp() &&
                    !applyto.hasDisease(Disease.ZOMBIFY)) {
                return false;
            }
            stat.setHp(stat.getHp() + hpchange);
        }
        if (mpchange != 0) {
            if (mpchange < 0 && (-mpchange) > stat.getMp()) {
                return false;
            }
            stat.setMp(stat.getMp() + mpchange);

            hpmpupdate.add(new StatValue(Stat.MP, Integer.valueOf(stat.getMp())));
        }
        hpmpupdate.add(new StatValue(Stat.HP, Integer.valueOf(stat.getHp())));

        applyto.getClient().write(ChannelPackets.updatePlayerStats(hpmpupdate, true, applyto.getJobId()));

        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
            applyto.getClient().write(ChannelPackets.showSpecialEffect(19));
        } else if (GameConstants.isMonsterCard(sourceid)) {
            applyto.getMonsterBook().addCard(applyto.getClient(), sourceid);
        } else if (isSpiritClaw()) {
            Inventory use = applyto.getUseInventory();
            IItem item;
            for (int i = 0; i < use.getSlotLimit(); i++) { // impose order...
                item = use.getItem((byte) i);
                if (item != null) {
                    if (GameConstants.isThrowingStar(item.getItemId()) &&
                            item.getQuantity() >= 200) {
                        InventoryManipulator.removeById(applyto.getClient(), applyto.getUseInventory(), item.getItemId(), 200, false, true);
                        break;
                    }
                }
            }
        }
        if (overTime) {
            applyBuffEffect(applyfrom, applyto, primary);
        }
        if (primary) {
            if (overTime || isHeal()) {
                applyBuff(applyfrom);
            }
            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }
        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && pos != null) {
            final Summon tosummon = new Summon(applyfrom, sourceid, pos, summonMovementType);
            if (!tosummon.isPuppet()) {
                applyfrom.getCheatTracker().resetSummonAttack();
            }
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.getSummons().put(sourceid, tosummon);
            tosummon.addHP((short) x);
            if (isBeholder()) {
                tosummon.addHP((short) 1);
            }
            /*if (sourceid == 4341006) {
            applyfrom.cancelEffectFromBuffStat(BuffStat.MIRROR_IMAGE);
            }*/
        } else if (isMagicDoor()) { // Magic Door
            Door door = new Door(applyto, new Point(applyto.getPosition())); // Current Map door
            applyto.getMap().spawnDoor(door);
            applyto.addDoor(door);

            Door townDoor = new Door(door); // Town door
            applyto.addDoor(townDoor);
            door.getTown().spawnDoor(townDoor);

            if (applyto.hasParty()) { // update town doors
                applyto.silentPartyUpdate();
            }
            applyto.disableDoor();

        } else if (isMist()) {
            final Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            final Mist mist = new Mist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), isMistPoison(), false);

        } else if (isTimeLeap()) { // Time Leap
            for (PlayerCooldownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().write(ChannelPackets.skillCooldown(i.skillId, 0));
                }
            }
        }
        return true;
    }

    public final boolean applyReturnScroll(final ChannelCharacter applyto) {
        if (moveTo != -1) {
            if (applyto.getMap().getReturnMapId() != applyto.getMapId()) {
                GameMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
					target = ChannelServer.getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyto.getMapId() /
                            10000000 != 61) {
                        if (target.getId() / 10000000 != 21 &&
                                applyto.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != applyto.getMapId() /
                                    10000000) {
                                return false;
                            }
                        }
                    }
                }
                applyto.changeMap(target, target.getPortal(0));
                return true;
            }
        }
        return false;
    }

    private void applyBuff(final ChannelCharacter applyfrom) {
        if (isPartyBuff() && (applyfrom.hasParty() || isGmBuff())) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            final List<GameMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(GameMapObjectType.PLAYER));

            for (final GameMapObject affectedmo : affecteds) {
                final ChannelCharacter affected = (ChannelCharacter) affectedmo;

                if (affected != applyfrom && (isGmBuff() || 
                        applyfrom.getPartyMembership().getPartyId() == affected.getPartyMembership().getPartyId())) {
                    if ((isResurrection() && !affected.isAlive()) ||
                            (!isResurrection() && affected.isAlive())) {
                        applyTo(applyfrom, affected, false, null);
                        affected.getClient().write(ChannelPackets.showOwnBuffEffect(sourceid, 2));
                        affected.getMap().broadcastMessage(affected, ChannelPackets.showBuffeffect(affected.getId(), sourceid, 2), false);
                    }
                    if (isTimeLeap()) {
                        for (PlayerCooldownValueHolder i : affected.getAllCooldowns()) {
                            if (i.skillId != 5121010) {
                                affected.removeCooldown(i.skillId);
                                affected.getClient().write(ChannelPackets.skillCooldown(i.skillId, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyMonsterBuff(final ChannelCharacter applyfrom) {
        final Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        final List<GameMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(GameMapObjectType.MONSTER));
        int i = 0;

        for (final GameMapObject mo : affected) {
            if (makeChanceResult()) {
                ((Monster) mo).applyStatus(applyfrom, new MonsterStatusEffect(getMonsterStati(), SkillFactory.getSkill(sourceid), null, false), isPoison(), getDuration(), false);
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    private Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public final void silentApplyBuff(final ChannelCharacter chr, final long starttime) {
        final int localDuration = alchemistModifyVal(chr, duration, false);
        chr.registerEffect(this, starttime, TimerManager.getInstance().schedule(new CancelEffectAction(chr, this, starttime),
                                                                                ((starttime +
                localDuration) - System.currentTimeMillis())));

        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final Summon tosummon = new Summon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isPuppet()) {
                chr.getCheatTracker().resetSummonAttack();
                chr.getMap().spawnSummon(tosummon);
                chr.getSummons().put(sourceid, tosummon);
                tosummon.addHP((short) x);
            }
        }
    }

    public final void applyComboBuff(final ChannelCharacter applyto, short combo) {
        final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.ARAN_COMBO, combo));
        applyto.getClient().write(ChannelPackets.giveBuff(sourceid, 99999, stat, this)); // Hackish timing, todo find out

        final long starttime = System.currentTimeMillis();
//	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
//	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - System.currentTimeMillis()));
        applyto.registerEffect(this, starttime, null);
    }

    public final void applyEnergyBuff(final ChannelCharacter applyto, final boolean infinity) {
//	final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(BuffStat.ENERGY_CHARGE, (int) applyto.getEnergyCharge()));
        applyto.getClient().write(ChannelPackets.giveEnergyChargeTest(0));

        final long starttime = System.currentTimeMillis();
        if (infinity) {
            applyto.registerEffect(this, starttime, null);
        } else {
            final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime +
                    duration) - System.currentTimeMillis()));
            applyto.registerEffect(this, starttime, schedule);
        }
    }

    private void applyBuffEffect(final ChannelCharacter applyfrom, final ChannelCharacter applyto, final boolean primary) {
        if (!isMonsterRiding_()) {
            applyto.cancelEffect(this, true, -1);
        }
        int localDuration = duration;

        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
            applyto.getMap().broadcastMessage(applyto, ChannelPackets.showBuffeffect(applyto.getId(), sourceid, 1), false);
        }
        boolean normal = true;

        switch (sourceid) {
            case 5001005: // Dash
            case 4321000: //tornado spin
            case 15001003: {
                applyto.getClient().write(ChannelPackets.givePirate(statups, localDuration /
                        1000, sourceid));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignPirate(statups, localDuration /
                        1000, applyto.getId(), sourceid), false);
                normal = false;
                break;
            }
            case 5211006: // Homing Beacon
            case 22151002: //killer wings
            case 5220011: {// Bullseye
                if (applyto.getLinkedMonsterId() > 0) {
                    applyto.getClient().write(ChannelPackets.cancelHoming());
                    applyto.getClient().write(ChannelPackets.giveHoming(sourceid, applyto.getLinkedMonsterId()));
                } else {
                    return;
                }
                normal = false;
                break;
            }
            case 1004:
            case 10001004:
            case 5221006:
            case 20001004: {
                final int mountid = parseMountInfo(applyto, sourceid);
                if (mountid != 0) {
                    final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.MONSTER_RIDING, 0));
                    applyto.getClient().write(ChannelPackets.giveMount(mountid, sourceid, stat));
                    applyto.getMap().broadcastMessage(applyto, ChannelPackets.showMonsterRiding(applyto.getId(), stat, mountid, sourceid), false);
                    normal = false;
                }
                break;
            }
            case 15100004:
            case 5110001: { // Energy Charge
//		final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(BuffStat.ENERGY_CHARGE, applyto.getEnergyCharge()));
//		applyto.getClient().write(MaplePacketCreator.giveEnergyCharge(stat, (skill ? sourceid : -sourceid), localDuration));
                applyto.getClient().write(ChannelPackets.giveEnergyChargeTest(0));
                normal = false;
                break;
            }
            case 5121009: // Speed Infusion
            case 15111005:
                applyto.getClient().write(ChannelPackets.giveInfusion(statups, sourceid, localDuration));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignInfusion(applyto.getId(), x, localDuration), false);
                normal = false;
                break;
            case 13101006:
            case 4330001:
            case 4001003:
            case 14001003: { // Dark Sight
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.DARKSIGHT, 0));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4341002: { // Final Cut
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.FINAL_CUT, y));
                applyto.getClient().write(ChannelPackets.giveBuff(sourceid, localDuration, stat, this));
                normal = false;
                break;
            }
            case 4331003: { // Owl Spirit
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.OWL_SPIRIT, y));
                applyto.getClient().write(ChannelPackets.giveBuff(sourceid, localDuration, stat, this));
                normal = false;
                break;
            }
            case 4331002: { // Mirror Image
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.MIRROR_IMAGE, 0));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1111002:
            case 11111001: { // Combo
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.COMBO, 1));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 3101004:
            case 3201004:
            case 13101003: { // Soul Arrow
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.SOULARROW, 0));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4111002:
            case 14111000: { // Shadow Partne
                final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.SHADOWPARTNER, 0));
                applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1121010: // Enrage
                applyto.handleOrbconsume();
                break;
            default:
                if (isMorph() || isPirateMorph()) {
                    final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.MORPH, Integer.valueOf(getMorph(applyto))));
                    applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isMonsterRiding()) {
                    final int mountid = parseMountInfo(applyto, sourceid);
                    if (mountid != 0) {
                        final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.MONSTER_RIDING, 0));
                        applyto.getClient().write(ChannelPackets.cancelBuff(null));
                        applyto.getClient().write(ChannelPackets.giveMount(mountid, sourceid, stat));
                        applyto.getMap().broadcastMessage(applyto, ChannelPackets.showMonsterRiding(applyto.getId(), stat, mountid, sourceid), false);
                    } else {
                        return;
                    }
                    normal = false;
                } else if (isSoaring()) {
                    final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.SOARING, 1));
                    applyto.getMap().broadcastMessage(applyto, ChannelPackets.giveForeignBuff(applyto.getId(), stat, this), false);
                    applyto.getClient().write(ChannelPackets.giveBuff(sourceid, localDuration, stat, this));
                    normal = false;
                }
                break;
        }
        // Broadcast effect to self
        if (normal && statups.size() > 0) {
            applyto.getClient().write(ChannelPackets.giveBuff((skill ? sourceid : -sourceid), localDuration, statups, this));
        }
        final long starttime = System.currentTimeMillis();
        final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
        final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime +
                localDuration) - System.currentTimeMillis()));
        applyto.registerEffect(this, starttime, schedule);
    }

    public static int parseMountInfo(final ChannelCharacter player, final int skillid) {
        switch (skillid) {
            case 1004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
                final IItem item = player.getEquippedItemsInventory().getItem((byte) -22);
                if (item != null) {
                    return item.getItemId();
                }
                return 0;
            case 5221006: // Battleship
                return 1932000;
        }
        return 0;
    }

    private int calcHPChange(final ChannelCharacter applyfrom, final boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
                if (applyfrom.hasDisease(Disease.ZOMBIFY)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                hpchange += makeHealHP(hp / 100.0, applyfrom.getStats().getTotalMagic(), 3, 5);
                if (applyfrom.hasDisease(Disease.ZOMBIFY)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getStats().getCurrentMaxHp() * hpR);
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        switch (this.sourceid) {
            case 4211001: // Chakra
//		final PlayerStats stat = applyfrom.getStats();
//		int v42 = getY() + 100;
//		int v38 = Randomizer.rand(100, 200) % 0x64 + 100;
//		hpchange = (int) ((v38 * stat.getLuk() * 0.033 + stat.getDex()) * v42 * 0.002);
                hpchange += makeHealHP(getY() / 100.0, applyfrom.getStats().getTotalLuk(), 2.3, 3.5);
                break;
        }
        return hpchange;
    }

    private static int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) -
                (int) (stat * lowerfactor * rate) + 1)) + (int) (stat *
                lowerfactor * rate));
    }

    private static int getElementalAmp(final int job) {
        switch (job) {
            case 211:
            case 212:
                return 2110001;
            case 221:
            case 222:
                return 2210001;
            case 1211:
            case 1212:
                return 12110001;
            case 2215:
            case 2216:
            case 2217:
            case 2218:
                return 22150000;
        }
        return -1;
    }

    private int calcMPChange(final ChannelCharacter applyfrom, final boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getStats().getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;

                final int ElemSkillId = getElementalAmp(applyfrom.getJobId());
                if (ElemSkillId != -1) {
                    final ISkill amp = SkillFactory.getSkill(ElemSkillId);
                    final int ampLevel = applyfrom.getCurrentSkillLevel(amp);
                    if (ampLevel > 0) {
                        StatEffect ampStat = amp.getEffect(ampLevel);
                        mod = ampStat.getX() / 100.0;
                    }
                }
                if (applyfrom.getBuffedValue(BuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else {
                    mpchange -= mpCon * mod;
                }
            }
        }
        return mpchange;
    }

    private int alchemistModifyVal(final ChannelCharacter chr, final int val, final boolean withX) {
        if (!skill) {
            final StatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val *
                        ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) /
                        100.0));
            }
        }
        return val;
    }

    private StatEffect getAlchemistEffect(final ChannelCharacter chr) {
        ISkill al;
        switch (chr.getJobId()) {
            case 411:
            case 412:
                al = SkillFactory.getSkill(4110000);
                if (chr.getCurrentSkillLevel(al) == 0) {
                    return null;
                }
                return al.getEffect(chr.getCurrentSkillLevel(al));
            case 1411:
            case 1412:
                al = SkillFactory.getSkill(14110003);
                if (chr.getCurrentSkillLevel(al) == 0) {
                    return null;
                }
                return al.getEffect(chr.getCurrentSkillLevel(al));
        }
        return null;
    }

    public final void setSourceId(final int newid) {
        sourceid = newid;
    }

    private boolean isGmBuff() {
        switch (sourceid) {
            case 1005: // echo of hero acts like a gm buff
            case 10001005: // cygnus Echo
            case 20001005: // Echo
            case 20011005:
            case 9001000: // GM dispel
            case 9001001: // GM haste
            case 9001002: // GM Holy Symbol
            case 9001003: // GM Bless
            case 9001005: // GM resurrection
            case 9001008: // GM Hyper body
                return true;
            default:
                return false;
        }
    }

    private boolean isMonsterBuff() {
        switch (sourceid) {
            case 1201006: // threaten
            case 2101003: // fp slow
            case 2201003: // il slow
            case 12101001: // cygnus slow
            case 2211004: // il seal
            case 2111004: // fp seal
            case 12111002: // cygnus seal
            case 2311005: // doom
            case 4111003: // shadow web
            case 14111001: // cygnus web
            case 4121004: // Ninja ambush
            case 4221004: // Ninja ambush
            case 22151001:
            case 22141003:
            case 22121000:
            case 22161002:
            case 4321002:
                return skill;
        }
        return false;
    }

    public final boolean isMonsterRiding_() {
        return skill && (sourceid == 1004 || sourceid == 10001004 || sourceid ==
                20001004 || sourceid == 20011004);
    }

    public final boolean isMonsterRiding() {
        return skill && (isMonsterRiding_());
    }

    private boolean isPartyBuff() {
        if (lt == null || rb == null) {
            return false;
        }
        switch (sourceid) {
            case 1211003:
            case 1211004:
            case 1211005:
            case 1211006:
            case 1211007:
            case 1211008:
            case 1221003:
            case 1221004:
            case 11111007:
            case 12101005:
                return false;
        }
        return true;
    }

    public final boolean isHeal() {
        return sourceid == 2301002 || sourceid == 9101000;
    }

    public final boolean isResurrection() {
        return sourceid == 9001005 || sourceid == 2321006;
    }

    public final boolean isTimeLeap() {
        return sourceid == 5121010;
    }

    public final short getHp() {
        return hp;
    }

    public final short getMp() {
        return mp;
    }

    public final byte getMastery() {
        return mastery;
    }

    public final short getWatk() {
        return watk;
    }

    public final short getMatk() {
        return matk;
    }

    public final short getWdef() {
        return wdef;
    }

    public final short getMdef() {
        return mdef;
    }

    public final short getAcc() {
        return acc;
    }

    public final short getAvoid() {
        return avoid;
    }

    public final short getHands() {
        return hands;
    }

    public final short getSpeed() {
        return speed;
    }

    public final short getJump() {
        return jump;
    }

    public final int getDuration() {
        return duration;
    }

    public final boolean isOverTime() {
        return overTime;
    }

    public final List<BuffStatValue> getStatups() {
        return statups;
    }

    public final boolean sameSource(final StatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getZ() {
        return z;
    }

    public final short getDamage() {
        return damage;
    }

    public final byte getAttackCount() {
        return attackCount;
    }

    public final byte getBulletCount() {
        return bulletCount;
    }

    public final int getBulletConsume() {
        return bulletConsume;
    }

    public final byte getMobCount() {
        return mobCount;
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public final int getCooldown() {
        return cooldown;
    }

    public final Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public final boolean isHide() {
        return skill && sourceid == 9001004;
    }

    public final boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public final boolean isBerserk() {
        return skill && sourceid == 1320006;
    }

    public final boolean isBeholder() {
        return skill && sourceid == 1321007;
    }

    public final boolean isMPRecovery() {
        return skill && sourceid == 5101005;
    }

    public final boolean isMagicDoor() {
        return skill && sourceid == 2311002;
    }

    public final boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public final boolean isCharge() {
        switch (sourceid) {
            case 1211003:
            case 1211008:
            case 11111007:
            case 12101005:
            case 15101006:
            case 21111005:
                return skill;
        }
        return false;
    }

    public final boolean isMistPoison() {
        switch (sourceid) {
            case 2111003:
            case 12111005: // Flame gear
            case 14111006: // Poison bomb
                return true;
        }
        return false;
    }

    public final boolean isPoison() {
        switch (sourceid) {
            case 2111003:
            case 2101005:
            case 2111006:
            case 2121003:
            case 2221003:
            case 12111005: // Flame gear
            case 3111003: //inferno, new
            case 22161002: //phantom imprint
                return skill;
        }
        return false;
    }

    private boolean isMist() {
        return skill && (sourceid == 2111003 || sourceid == 4221006 || sourceid ==
                12111005 || sourceid == 14111006); // poison mist, smokescreen and flame gear
    }

    private boolean isSpiritClaw() {
        return skill && sourceid == 4121006;
    }

    private boolean isDispel() {
        return skill && (sourceid == 2311001 || sourceid == 9001000);
    }

    private boolean isHeroWill() {
        switch (sourceid) {
            case 1121011:
            case 1221012:
            case 1321010:
            case 2121008:
            case 2221008:
            case 2321009:
            case 3121009:
            case 3221008:
            case 4121009:
            case 4221008:
            case 5121008:
            case 5221010:
            case 21121008:
            case 22171004:
            case 4341008:
                return skill;
        }
        return false;
    }

    public final boolean isAranCombo() {
        return sourceid == 21000000;
    }

    public final boolean isPirateMorph() {
        switch (sourceid) {
            case 15111002:
            case 5111005:
            case 5121003:
                return skill;
        }
        return false;
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    public final int getMorph() {
        return morphId;
    }

    public final int getMorph(final ChannelCharacter chr) {
        final byte genderByte = chr.getGender().asNumber();
        switch (morphId) {
            case 1000:
            case 1100:
                return morphId + genderByte;
            case 1003:
                return morphId + (genderByte * 100);
        }
        return morphId;
    }

    public final SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case 3211002: // puppet sniper
            case 3111002: // puppet ranger
            case 13111004: // puppet cygnus
            case 5211001: // octopus - pirate
            case 5220002: // advanced octopus - pirate
            case 4341006:
                return SummonMovementType.STATIONARY;
            case 3211005: // golden eagle
            case 3111005: // golden hawk
            case 2311006: // summon dragon
            case 3221005: // frostprey
            case 3121006: // phoenix
            case 5211002: // bird - pirate
                return SummonMovementType.CIRCLE_FOLLOW;
            case 1321007: // beholder
            case 2121005: // elquines
            case 2221005: // ifrit
            case 2321003: // bahamut
            case 12111004: // Ifrit
            case 11001004: // soul
            case 12001004: // flame
            case 13001004: // storm
            case 14001005: // darkness
            case 15001004:
                return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public final boolean isSoaring() {

        switch (sourceid) {
            case 1026: // Soaring
            case 10001026: // Soaring
            case 20001026: // Soaring
            case 20011026: // Soaring
                return skill;
        }
        return false;
    }

    public final boolean isSkill() {
        return skill;
    }

    public final int getSourceId() {
        return sourceid;
    }

    /**
     *
     * @return true if the effect should happen based on it's probablity, false otherwise
     */
    public final boolean makeChanceResult() {
        return prop == 100 || Randomizer.nextInt(99) < prop;
    }

    public final short getProb() {
        return prop;
    }

    public static class CancelEffectAction implements Runnable {

        private final StatEffect effect;
        private final WeakReference<ChannelCharacter> target;
        private final long startTime;

        public CancelEffectAction(final ChannelCharacter target, final StatEffect effect, final long startTime) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
        }

        @Override
        public void run() {
            final ChannelCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime);
            }
        }
    }
}
