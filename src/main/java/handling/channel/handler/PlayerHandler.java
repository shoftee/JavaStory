package handling.channel.handler;

import java.awt.Point;
import java.util.concurrent.ScheduledFuture;
import java.util.List;

import client.IItem;
import client.ISkill;
import client.SkillFactory;
import client.SkillMacro;
import client.GameConstants;
import client.CancelCooldownAction;
import client.MapleInventoryType;
import client.MapleBuffStat;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleKeyBinding;
import client.PlayerStats;
import client.anticheat.CheatingOffense;
import org.javastory.server.channel.ChannelManager;
import server.AutobanManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MaplePortal;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobAttackInfoFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.FieldLimitType;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.packet.MobPacket;
import tools.packet.MTSCSPacket;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.UIPacket;

public class PlayerHandler {

    private static final boolean isFinisher(final int skillid) {
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

    public static final void ChangeMonsterBookCover(final int bookid, final MapleClient c, final MapleCharacter chr) {
        if (bookid == 0 || GameConstants.isMonsterCard(bookid)) {
            chr.setMonsterBookCover(bookid);
            chr.getMonsterBook().updateCard(c, bookid);
        }
    }

    public static final void ChangeSkillMacro(final SeekableLittleEndianAccessor slea, final MapleCharacter chr) {
        final int num = slea.readByte();
        String name;
        int shout, skill1, skill2, skill3;
        SkillMacro macro;

        for (int i = 0; i < num; i++) {
            name = slea.readMapleAsciiString();
            shout = slea.readByte();
            skill1 = slea.readInt();
            skill2 = slea.readInt();
            skill3 = slea.readInt();

            macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }

    public static final void ChangeKeymap(final SeekableLittleEndianAccessor slea, final MapleCharacter chr) {
        if (slea.available() != 8) { // else = pet auto pot
            slea.skip(4);
            final int numChanges = slea.readInt();

            int key, type, action;
            MapleKeyBinding newbinding;

            for (int i = 0; i < numChanges; i++) {
                key = slea.readInt();
                type = slea.readByte();
                action = slea.readInt();
                newbinding = new MapleKeyBinding(type, action);
                chr.changeKeybinding(key, newbinding);
            }
        }
    }

    public static final void UseChair(final int itemId, final MapleClient c, final MapleCharacter chr) {
        final IItem toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);

        if (toUse == null || toUse.getItemId() != itemId) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }
        if (itemId == 3011000) {
            for (IItem item : c.getPlayer().getInventory(MapleInventoryType.CASH).list()) {
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
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void CancelChair(final short id, final MapleClient c, final MapleCharacter chr) {
        if (id == -1) { // Cancel Chair
            if (chr.getChair() == 3011000) {
                chr.cancelFishingTask();
            }
            chr.setChair(0);
            c.getSession().write(MaplePacketCreator.cancelChair(-1));
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), 0), false);
        } else { // Use In-Map Chair
            chr.setChair(id);
            c.getSession().write(MaplePacketCreator.cancelChair(id));
        }
    }

    public static final void TrockAddMap(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte addrem = slea.readByte();
        final byte vip = slea.readByte();

        if (addrem == 0) {
            chr.deleteFromRocks(slea.readInt());
        } else if (addrem == 1) {
            if (chr.getMap().getForcedReturnId() == 999999999) {
                chr.addRockMap();
            }
        }
        c.getSession().write(MTSCSPacket.getTrockRefresh(chr, vip == 1, addrem == 3));
    }

    public static final void CharInfoRequest(final int objectid, final MapleClient c, final MapleCharacter chr) {
        final MapleCharacter player = (MapleCharacter) c.getPlayer().getMap().getMapObject(objectid);

        if (player != null) {
            if (!player.isGM() || (c.getPlayer().isGM() && player.isGM())) {
                c.getSession().write(MaplePacketCreator.charInfo(player, c.getPlayer().equals(player)));
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }

    public static final void TakeDamage(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(4); // Ticks
        final byte type = slea.readByte();
        slea.skip(1); // Element - 0x00 = elementless, 0x01 = ice, 0x02 = fire, 0x03 = lightning
        int damage = slea.readInt();

        int oid = 0;
        int monsteridfrom = 0;
        int reflect = 0;
        byte direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        int mpattack = 0;
        boolean is_pg = false;
        boolean isDeadlyAttack = false;
        MapleMonster attacker = null;
        final PlayerStats stats = chr.getStat();

        if (type != -2 && type != -3 && type != -4) { // Not map damage
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = (MapleMonster) chr.getMap().getMonsterByOid(oid);
            direction = slea.readByte();

            if (attacker == null) {
                return;
            }
            if (type != -1) { // Bump damage
                final MobAttackInfo attackInfo = MobAttackInfoFactory.getInstance().getMobAttackInfo(attacker, type);
                if (attackInfo.isDeadlyAttack()) {
                    isDeadlyAttack = true;
                    mpattack = stats.getMp() - 1;
                } else {
                    mpattack += attackInfo.getMpBurn();
                }
                final MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                if (skill != null && (damage == -1 || damage > 0)) {
                    skill.applyEffect(chr, attacker, false);
                }
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
            }
        }

        if (damage == -1) {
            fake = 4020002 + ((chr.getJob() / 10 - 40) * 100000);
        } else if (damage < -1 || damage > 60000) {
            AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
            return;
        }
        chr.getCheatTracker().checkTakeDamage(damage);

        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);

            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                chr.cancelMorphs();
            }

            if (type == -1) {
                if (chr.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                    attacker = (MapleMonster) chr.getMap().getMapObject(oid);
                    if (attacker != null) {
                        int bouncedamage = (int) (damage * (chr.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / 100));
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(chr, bouncedamage, true);
                        damage -= bouncedamage;
                        chr.getMap().broadcastMessage(chr, MobPacket.damageMonster(oid, bouncedamage), chr.getPosition());
                        is_pg = true;
                    }
                }
            } else if (type != -2 && type != -3 && type != -4) {
                switch (chr.getJob()) {
                    case 112: {
                        final ISkill skill = SkillFactory.getSkill(1120004);
                        if (chr.getSkillLevel(skill) > 0) {
                            damage = (int) ((skill.getEffect(chr.getSkillLevel(skill)).getX() / 1000.0) * damage);
                        }
                        break;
                    }
                    case 122: {
                        final ISkill skill = SkillFactory.getSkill(1220005);
                        if (chr.getSkillLevel(skill) > 0) {
                            damage = (int) ((skill.getEffect(chr.getSkillLevel(skill)).getX() / 1000.0) * damage);
                        }
                        break;
                    }
                    case 132: {
                        final ISkill skill = SkillFactory.getSkill(1320005);
                        if (chr.getSkillLevel(skill) > 0) {
                            damage = (int) ((skill.getEffect(chr.getSkillLevel(skill)).getX() / 1000.0) * damage);
                        }
                        break;
                    }
                }
            }

            if (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
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
                    mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                    hploss = damage - mploss;

                    if (mploss > stats.getMp()) {
                        hploss += mploss - stats.getMp();
                        mploss = stats.getMp();
                    }
                    chr.addMPHP(-hploss, -mploss);
                }

            } else if (chr.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1;

                final int mesoloss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0));
                if (chr.getMeso() < mesoloss) {
                    chr.gainMeso(-chr.getMeso(), false);
                    chr.cancelBuffStats(MapleBuffStat.MESOGUARD);
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
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.damagePlayer(type, monsteridfrom, chr.getId(), damage, fake, direction, reflect, is_pg, oid, pos_x, pos_y), false);
        }
    }

    public static final void AranCombo(final MapleClient c, final MapleCharacter chr) {
        if (chr.getJob() >= 2000 && chr.getJob() <= 2112) {
            short combo = chr.getCombo();
            final long curr = System.currentTimeMillis();
            if (combo > 0 && (curr - chr.getLastCombo()) > 5000) {
                // Official MS timing is 2.5 seconds, so 5 seconds should be safe.
                chr.getCheatTracker().registerOffense(CheatingOffense.ARAN_COMBO_HACK);
            }
            if (combo < 30000) {
                combo++;
            }
            chr.setLastCombo(curr);
            chr.setCombo(combo);
            c.getSession().write(MaplePacketCreator.testCombo(combo));
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
                    SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
                    break;
            }
        }
    }

    public static final void UseItemEffect(final int itemId, final MapleClient c, final MapleCharacter chr) {
        final IItem toUse = chr.getInventory(MapleInventoryType.CASH).findById(itemId);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.setItemEffect(itemId);
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(final int id, final MapleCharacter chr) {
        chr.cancelEffect(
                MapleItemInformationProvider.getInstance().getItemEffect(-id), false, -1);
    }

    public static final void CancelBuffHandler(final int sourceid, final MapleCharacter chr) {
        final ISkill skill = SkillFactory.getSkill(sourceid);

        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);
        } else {
            chr.cancelEffect(SkillFactory.getSkill(sourceid).getEffect(1), false, -1);
        }
    }

    public static final void SkillEffect(final SeekableLittleEndianAccessor slea, final MapleCharacter chr) {
        final int skillId = slea.readInt();
        final byte level = slea.readByte();
        final byte flags = slea.readByte();
        final byte speed = slea.readByte();
        final byte unk = slea.readByte(); // Added on v.82

        final ISkill skill = SkillFactory.getSkill(skillId);
        final int skilllevel_serv = chr.getSkillLevel(skill);

        if (skilllevel_serv > 0 && skilllevel_serv == level && skill.isChargeSkill() && level > 0) {
            chr.setKeyDownSkill_Time(System.currentTimeMillis());
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
        }
    }

    public static final void SpecialMove(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        slea.skip(4); // Old X and Y
        final int skillid = slea.readInt();
        final int skillLevel = slea.readByte();
        final ISkill skill = SkillFactory.getSkill(skillid);

        if (chr.getSkillLevel(skill) == 0 || chr.getSkillLevel(skill) != skillLevel) {
            if (!GameConstants.isMulungSkill(skillid)) {
                c.getSession().close();
                return;
            }
            if (chr.getMapId() / 10000 != 92502) {
                //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
            } else {
                chr.mulung_EnergyModify(false);
            }
        }
        final MapleStatEffect effect = skill.getEffect(chr.getSkillLevel(skill));

        if (effect.getCooldown() > 0) {
            if (chr.skillisCooling(skillid)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (skillid != 5221006) { // Battleship
                c.getSession().write(MaplePacketCreator.skillCooldown(skillid, effect.getCooldown()));
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(chr, skillid), effect.getCooldown() * 1000);
                chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
            }
        }

        switch (skillid) {
            case 1121001:
            case 1221001:
            case 1321001:
            case 9001020: // GM magnet
                final byte number_of_mobs = slea.readByte();
                slea.skip(3);
                for (int i = 0; i < number_of_mobs; i++) {
                    int mobId = slea.readInt();

                    final MapleMonster mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob != null) {
//			chr.getMap().broadcastMessage(chr, MaplePacketCreator.showMagnet(mobId, slea.readByte()), chr.getPosition());
                        mob.switchController(chr, mob.isControllerHasAggro());
                    }
                }
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, slea.readByte()), chr.getPosition());
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            default:
                Point pos = null;
                if (slea.available() == 7) {
                    pos = slea.readPos();
                }
                if (skill.getId() == 2311002) { // Mystic Door
                    if (chr.canDoor()) {
                        if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                            effect.applyTo(c.getPlayer(), pos);
                        } else {
                            c.getSession().write(MaplePacketCreator.enableActions());
                        }
                    } else {
                        chr.dropMessage(5, "Please wait 5 seconds before casting Mystic Door again.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                } else {
                    if (effect.parseMountInfo(c.getPlayer(), skill.getId()) != 0 && c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null && c.getPlayer().getDragon() != null) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeDragon(c.getPlayer().getId()));
                        c.getPlayer().getMap().removeMapObject(c.getPlayer().getDragon());
                        c.getPlayer().setDragon(null);
                    }
                    effect.applyTo(c.getPlayer(), pos);
                }
                break;
        }
    }

    public static final void closeRangeAttack(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        final AttackInfo attack = DamageParse.parseDmgM(slea);

        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final boolean mirror = chr.getBuffedValue(MapleBuffStat.MIRROR_IMAGE) != null;
        int attackCount = (chr.getJob() >= 430 && chr.getJob() <= 434 ? 2 : 1), skillLevel = 0;
        MapleStatEffect effect = null;
        ISkill skill = null;

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            skillLevel = chr.getSkillLevel(skill);
            if (skillLevel == 0) {
                c.getSession().close();
                return;
            }
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            maxdamage *= effect.getDamage() / 100.0;
            attackCount = effect.getAttackCount();

            if (effect.getCooldown() > 0) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
            }
        }
        attackCount *= mirror ? 2 : 1;
        // handle combo orbconsume
        int numFinisherOrbs = 0;
        final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);

        if (isFinisher(attack.skill)) { // finisher
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff.intValue() - 1;
            }
            chr.handleOrbconsume();

        } else if (attack.targets > 0 && comboBuff != null) {
            // handle combo orbgain
            switch (chr.getJob()) {
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
        switch (chr.getJob()) {
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
        if (attack.targets > 0 && attack.skill == 1211002) { // handle charged blow
            final int advcharge_level = chr.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                if (!SkillFactory.getSkill(1220010).getEffect(advcharge_level).makeChanceResult()) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
                }
            } else {
                chr.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }

        if (numFinisherOrbs > 0) {
            maxdamage *= numFinisherOrbs;
        } else if (comboBuff != null) {
            ISkill combo;
            if (c.getPlayer().getJob() == 1110 || c.getPlayer().getJob() == 1111) {
                combo = SkillFactory.getSkill(11111001);
            } else {
                combo = SkillFactory.getSkill(1111002);
            }
            maxdamage *= 1.0 + (combo.getEffect(c.getPlayer().getSkillLevel(combo)).getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
        }

        if (isFinisher(attack.skill)) {
            if (numFinisherOrbs == 0) {
                return;
            }
            maxdamage = MapleCharacter.getDamageCap(); // FIXME reenable damage calculation for finishers
        }
        DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);

        chr.getMap().broadcastMessage(chr, MaplePacketCreator.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed, attack.allDamage, chr.getLevel()), chr.getPosition());
    }

    public static final void rangedAttack(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        final AttackInfo attack = DamageParse.parseDmgR(slea);

        int bulletCount = 1, skillLevel = 0;
        MapleStatEffect effect = null;
        ISkill skill = null;

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(attack.skill);
            skillLevel = chr.getSkillLevel(skill);
            if (skillLevel == 0) {
                c.getSession().close();
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
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
            }
        }
        final Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0, visProjectile = 0;
        if (attack.AOE != 0 && chr.getBuffedValue(MapleBuffStat.SOULARROW) == null && attack.skill != 4111004) {
            projectile = chr.getInventory(MapleInventoryType.USE).getItem(attack.slot).getItemId();

            if (attack.csstar > 0) {
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }
            // Handle bulletcount
            if (chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) == null) {
                int bulletConsume = bulletCount;
                if (effect != null && effect.getBulletConsume() != 0) {
                    bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                }
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true);
            }
        }

        double basedamage;
        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }
        final PlayerStats statst = chr.getStat();
        switch (attack.skill) {
            case 4001344: // Lucky Seven
            case 4121007: // Triple Throw
            case 14001004: // Lucky seven
            case 14111005: // Triple Throw
                basedamage = (float) ((float) ((statst.getTotalLuk() * 5.0f) * (statst.getTotalWatk() + projectileWatk)) / 100);
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
        DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);

        chr.getMap().broadcastMessage(chr, MaplePacketCreator.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel()), chr.getPosition());
    }

    public static final void MagicDamage(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        final AttackInfo attack = DamageParse.parseDmgMa(slea);
        final ISkill skill = SkillFactory.getSkill(attack.skill);
        final int skillLevel = chr.getSkillLevel(skill);
        if (skillLevel == 0) {
            c.getSession().close();
            return;
        }
        final MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);

        if (effect.getCooldown() > 0) {
            if (chr.skillisCooling(attack.skill)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
            chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(chr, attack.skill), effect.getCooldown() * 1000));
        }
        DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect);

        chr.getMap().broadcastMessage(chr, MaplePacketCreator.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.animation, attack.speed, attack.allDamage, attack.charge, chr.getLevel()), chr.getPosition());
    }

    public static final void WheelOfFortuneEffect(final int itemId, final MapleCharacter chr) {
        // BA 03 00 00   72 01 00 00 << extra int
        switch (itemId) {
            case 5510000: {
                if (!chr.isAlive()) {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showSpecialEffect(chr.getId(), itemId), false);
                }
                break;
            }
            default:
                System.out.println("Unhandled item effect [WheelOfFortuneHandler] id : " + itemId);
                break;
        }
    }

    public static final void DropMeso(final int meso, final MapleCharacter chr) {
        if (!chr.isAlive() || (meso < 10 || meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getPosition(), chr, chr, true, (byte) 0);
    }

    public static final void ChangeEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 7) {
            final int emoteid = 5159992 + emote;
            final MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(emoteid));
                return;
            }
        }
        if (emote > 0) {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.facialExpression(chr, emote), false);
        }
    }

    public static final void Heal(final SeekableLittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(4);
        final int healHP = slea.readShort();
        final int healMP = slea.readShort();
        final PlayerStats stats = chr.getStat();
        if (stats.getHp() <= 0) {
            return;
        }
        if (healHP != 0) {
            if (healHP > stats.getHealHP()) {
                //chr.getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_HP, String.valueOf(healHP));
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

    public static final void MovePlayer(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
//	slea.skip(5); // unknown
        final Point Original_Pos = chr.getPosition(); // 4 bytes Added on v.80 MSEA
        slea.skip(37);

        // log.trace("Movement command received: unk1 {} unk2 {}", new Object[] { unk1, unk2 });
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea);

        if (res != null) { // TODO more validation of input data
            if (slea.available() != 18) {
                //System.out.println("slea.available != 18 (movement parsing error)");
                return;
            }
            final MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
            } else {
                speedCheck(res, c);
                map.broadcastMessage(chr, MaplePacketCreator.movePlayer(chr.getId(), res, Original_Pos), false);
            }
            MovementParse.updatePosition(res, chr, 0);
            map.movePlayer(chr, chr.getPosition());

            /*	    int count = c.getPlayer().getFallCounter();
            if (map.getFootholds().findBelow(c.getPlayer().getPosition()) == null) {
            if (count > 3) {
            c.getPlayer().changeMap(map, map.getPortal(0));
            } else {
            c.getPlayer().setFallCounter(++count);
            }
            } else if (count > 0) {
            c.getPlayer().setFallCounter(0);
            }*/
        }
    }

    private static final void speedCheck(final List<LifeMovementFragment> res, final MapleClient c) {
        double speedMod, playerSpeedMod = c.getPlayer().getStat().getSpeedMod() + 0.005;
        for (LifeMovementFragment lmf : res) {
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

    public static final void ChangeMapSpecial(final String portal_name, final MapleClient c, final MapleCharacter chr) {
        final MaplePortal portal = chr.getMap().getPortal(portal_name);
//	slea.skip(2);

        if (portal != null) {
            portal.enterPortal(c);
        }
    }

    public static final void ChangeMap(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (slea.available() != 0) {
            slea.skip(7); // 1 = from dying 2 = regular portals
            final int targetid = slea.readInt(); // FF FF FF FF
            final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
            if (slea.available() >= 7) {

                slea.skip(4); //2F 03 04 00

            }
            slea.skip(1);
            final boolean wheel = slea.readShort() > 0;

            if (targetid != -1 && !chr.isAlive()) {
                if (chr.getEventInstance() != null) {
                    chr.getEventInstance().revivePlayer(chr);
                }
                chr.setStance(0);

                if (!wheel) {
                    chr.getStat().setHp(50);

                    final MapleMap to = chr.getMap().getReturnMap();
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    if (chr.haveItem(5510000, 1, false, true)) { // Wheel of Fortune
                        chr.getStat().setHp((chr.getStat().getMaxHp() / 100) * 40);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);

                        final MapleMap to = chr.getMap();
                        chr.changeMap(to, to.getPortal(0));
                    } else {
                        c.disconnect(true, false);
                    }
                }
            } else if (targetid != -1 && chr.isGM()) {
                final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                chr.changeMap(to, to.getPortal(0));

            } else if (targetid != -1 && !chr.isGM()) {
                final int divi = chr.getMapId() / 100;
                if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack

                    if (targetid == 130000000 || targetid / 100 == 9130401) { // Cygnus introduction
                        final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                        chr.changeMap(to, to.getPortal(0));
                    }
                } else if (divi == 9140900) { // Aran Introduction
                    if (targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000) {
                        final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                        chr.changeMap(to, to.getPortal(0));
                    }
                } else if (divi == 9140901 && targetid == 140000000) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (divi == 9140902 && (targetid == 140030000 || targetid == 140000000)) { //thing is. dont really know which one!
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (divi == 9000900 && targetid / 100 == 9000900 && targetid > chr.getMapId()) {
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (divi / 1000 == 9000 && targetid / 100000 == 9000) {
                    if (targetid < 900090000 || targetid > 900090004) { //1 movie
                        c.getSession().write(UIPacket.IntroDisableUI(false));
                        c.getSession().write(UIPacket.IntroLock(false));
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (divi / 10 == 1020 && targetid == 1020000) { // Adventurer movie clip Intro
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));

                } else if (chr.getMapId() == 900090101 && targetid == 100030100) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (chr.getMapId() == 2010000 && targetid == 104000000) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (chr.getMapId() == 106020001 || chr.getMapId() == 106020502) {
                    if (targetid == (chr.getMapId() - 1)) {
                        c.getSession().write(UIPacket.IntroDisableUI(false));
                        c.getSession().write(UIPacket.IntroLock(false));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                        chr.changeMap(to, to.getPortal(0));
                    }
                } else if (chr.getMapId() == 0 && targetid == 10000) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    final MapleMap to = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                }
            } else {
                if (portal != null) {
                    portal.enterPortal(c);
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
    }

    public static final void InnerPortal(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final MaplePortal portal = c.getPlayer().getMap().getPortal(slea.readMapleAsciiString());
        final int toX = slea.readShort();
        final int toY = slea.readShort();
//	slea.readShort(); // Original X pos
//	slea.readShort(); // Original Y pos

        if (portal == null) {
            c.disconnect(true, false);
            return;
        } else if (portal.getPosition().distanceSq(chr.getPosition()) > 22500) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));
    }
}
