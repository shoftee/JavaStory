package javastory.channel.life;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.client.BuffStat;
import javastory.channel.client.Disease;
import javastory.channel.client.MonsterStatus;
import javastory.channel.client.MonsterStatusEffect;
import javastory.channel.client.SkillFactory;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.packet.MobPacket;
import javastory.game.SkillLevelEntry;
import javastory.scripting.EventInstanceManager;
import javastory.server.TimerManager;
import javastory.server.handling.ServerConstants;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Monster extends AbstractLoadedGameLife {

    private MonsterStats stats;
    private OverrideMonsterStats statsOverride = null;
    private int hp, mp;
    //   private short showdown;
    private byte venom_counter, carnivalTeam;
    private GameMap map;
    private Monster sponge;
    // Just a reference for monster EXP distribution after death
    private ChannelCharacter highestDamageChar;
    private WeakReference<ChannelCharacter> controller = new WeakReference<>(null);
    private boolean isFake, dropsDisabled, controllerHasAggro, controllerKnowsAboutAggro;
    private final Collection<AttackerEntry> attackers = Lists.newLinkedList();
    private EventInstanceManager eventInstance;
    private MonsterListener listener = null;
    private final Map<MonsterStatus, MonsterStatusEffect> statuses = Maps.newEnumMap(MonsterStatus.class);
//    private final List<MonsterStatusEffect> activeEffects = new ArrayList<MonsterStatusEffect>();
//    private final List<MonsterStatus> monsterBuffs = new ArrayList<MonsterStatus>();
    private Map<Integer, Long> usedSkills;

    public Monster(final int id, final MonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public Monster(final Monster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(final MonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
        venom_counter = 0;
//	showdown = 100;
        carnivalTeam = -1;
        isFake = false;
        dropsDisabled = false;

        if (stats.getNoSkills() > 0) {
            usedSkills = new HashMap<>();
        }
    }

    public final MonsterStats getStats() {
        return stats;
    }

    public final void disableDrops() {
        this.dropsDisabled = true;
    }

    public final boolean dropsDisabled() {
        return dropsDisabled;
    }

    public final void setSponge(final Monster mob) {
        sponge = mob;
    }

    public final void setMap(final GameMap map) {
        this.map = map;
    }

    public final int getHp() {
        return hp;
    }

    public final void setHp(int hp) {
        this.hp = hp;
    }

    public final int getMobMaxHp() {
        if (statsOverride != null) {
            return statsOverride.getHp();
        }
        return stats.getHp();
    }

    public final int getMp() {
        return mp;
    }

    public final void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public final int getMobMaxMp() {
        if (statsOverride != null) {
            return statsOverride.getMp();
        }
        return stats.getMp();
    }

    public final int getMobExp() {
        if (statsOverride != null) {
            return statsOverride.getExp();
        }
        return stats.getExp();
    }

    public final void setOverrideStats(final OverrideMonsterStats ostats) {
        this.statsOverride = ostats;
        this.hp = ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final Monster getSponge() {
        return sponge;
    }

    public final byte getVenomMulti() {
        return venom_counter;
    }

    public final void setVenomMulti(final byte venom_counter) {
        this.venom_counter = venom_counter;
    }

    public final void damage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
        if (damage <= 0 || !isAlive()) {
            return;
        }
        AttackerEntry attacker = null;

        final PartyMember member = from.getPartyMembership();
        if (member != null) {
            member.getPartyId();
            attacker = new PartyAttackerEntry(member.getPartyId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        final int rDamage = Math.max(0, Math.min(damage, hp));
        attacker.addDamage(from, rDamage, updateAttackTime);

        if (stats.getSelfD() != -1) {
            hp -= rDamage;
            if (hp > 0) {
                if (hp < stats.getSelfDHp()) {
                    // HP is below the selfd level
                    map.killMonster(this, from, false, false, stats.getSelfD());
                } else { // Show HP
                    for (final AttackerEntry mattacker : attackers) {
                        for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                            if (cattacker.getAttacker().getMapId() == from.getMapId()) {
                                // current attacker is on the map of the monster
                                if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                    cattacker.getAttacker().getClient().write(MobPacket.showMonsterHP(getObjectId(), (int) Math.ceil((hp * 100.0) / getMobMaxHp())));
                                }
                            }
                        }
                    }
                }
            } else {
                // Character killed it without explosing :(
                map.killMonster(this, from, true, false, (byte) 1);
            }
        } else {
            if (sponge != null) {
                if (sponge.hp > 0) { // If it's still alive, dont want double/triple rewards
                    // Sponge are always in the same map, so we can use this.map
                    // The only mob that uses sponge are PB/HT
                    sponge.hp -= rDamage;

                    if (sponge.hp <= 0) {
                        map.killMonster(sponge, from, true, false, (byte) 1);
                    } else {
                        map.broadcastMessage(MobPacket.showBossHP(sponge));
                    }
                }
            }
            if (hp > 0) {
                hp -= rDamage;

                switch (stats.getHPDisplayType()) {
                    case 0:
                        map.broadcastMessage(MobPacket.showBossHP(this), this.getPosition());
                        break;
                    case 1:
                        map.broadcastMessage(MobPacket.damageFriendlyMob(this, damage), this.getPosition());
                        break;
                    case 2:
                        map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), (int) Math.ceil((hp * 100.0) / getMobMaxHp())));
                        from.mulung_EnergyModify(true);
                        break;
                    case 3:
                        for (final AttackerEntry mattacker : attackers) {
                            for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                                if (cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                    if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                        cattacker.getAttacker().getClient().write(MobPacket.showMonsterHP(getObjectId(), (int) Math.ceil((hp * 100.0) / getMobMaxHp())));
                                    }
                                }
                            }
                        }
                        break;
                }

                if (hp <= 0) {
                    map.killMonster(this, from, true, false, (byte) 1);
                }
            }
        }
    }

    public final void heal(final int hp, final int mp, final boolean broadcast) {
        final int TotalHP = getHp() + hp;
        final int TotalMP = getMp() + mp;

        if (TotalHP >= getMobMaxHp()) {
            setHp(getMobMaxHp());
        } else {
            setHp(TotalHP);
        }
        if (TotalMP >= getMp()) {
            setMp(getMp());
        } else {
            setMp(TotalMP);
        }
        if (broadcast) {
            map.broadcastMessage(MobPacket.healMonster(getObjectId(), hp));
        } else if (sponge != null) { // else if, since only sponge doesn't broadcast
            sponge.hp += hp;
        }
    }

    private void giveExpToCharacter(final ChannelCharacter attacker, int exp, final boolean highestDamage, final int numExpSharers, final byte pty, final byte CLASS_EXP_PERCENT) {
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            } else {
                final EventInstanceManager em = attacker.getEventInstance();
                if (em != null) {
                    em.monsterKilled(attacker, this);
                }
            }
            highestDamageChar = attacker;
        }
        if (exp > 0) {
            final Integer holySymbol = attacker.getBuffedValue(BuffStat.HOLY_SYMBOL);
            if (holySymbol != null) {
                if (numExpSharers == 1) {
                    exp *= 1.0 + (holySymbol.doubleValue() / 500.0);
                } else {
                    exp *= 1.0 + (holySymbol.doubleValue() / 100.0);
                }
            }
            int CLASS_EXP = 0;
            if (CLASS_EXP_PERCENT > 0) {
                CLASS_EXP = (int) ((float) (exp / 100) * CLASS_EXP_PERCENT);
            }
            if (attacker.hasDisease(Disease.CURSE)) {
                exp /= 2;
            }
            attacker.gainExpMonster(exp, true, highestDamage, pty, CLASS_EXP);
        }
        attacker.mobKilled(getId());
    }

    public final ChannelCharacter killBy(final ChannelCharacter killer) {
        int totalBaseExp = (Math.min(Integer.MAX_VALUE, (getMobExp() * killer.getClient().getChannelServer().getExpRate())));
        AttackerEntry highest = null;
        int highdamage = 0;
        for (final AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        int baseExp;
        for (final AttackerEntry attackEntry : attackers) {
            baseExp = (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMobMaxHp()));
            attackEntry.killedMob(killer.getMap(), baseExp, attackEntry == highest);
        }
        final ChannelCharacter controll = controller.get();
        if (controll != null) { // this can/should only happen when a hidden gm attacks the monster
            controll.getClient().write(MobPacket.stopControllingMonster(getObjectId()));
            controll.stopControllingMonster(this);
        }
        spawnRevives(killer.getMap());
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
            eventInstance = null;
        }
        sponge = null;

        if (listener != null) {
            listener.monsterKilled();
        }
        final ChannelCharacter ret = highestDamageChar;
        highestDamageChar = null; // may not keep hard references to chars outside of PlayerStorage or MapleMap
        return ret;
    }

    public final void spawnRevives(final GameMap map) {
        final List<Integer> toSpawn = stats.getRevives();

        if (toSpawn == null) {
            return;
        }
        switch (getId()) {
            case 8810026:
            case 8820009:
            case 8820010:
            case 8820011:
            case 8820012:
            case 8820013: {
                final List<Monster> mobs = new ArrayList<>();
                Monster spongy = null;

                for (final int i : toSpawn) {
                    final Monster mob = LifeFactory.getMonster(i);

                    mob.setPosition(getPosition());
                    switch (mob.getId()) {
                        case 8810018: // Horntail Sponge
                        case 8820010: // PinkBeanSponge1
                        case 8820011: // PinkBeanSponge2
                        case 8820012: // PinkBeanSponge3
                        case 8820013: // PinkBeanSponge4
                        case 8820014: // PinkBeanSponge5
                            spongy = mob;
                            break;
                        default:
                            mobs.add(mob);
                            break;
                    }
                }
                if (spongy != null) {
                    map.spawnRevives(spongy, this.getObjectId());

                    for (final Monster i : mobs) {
                        i.setSponge(spongy);
                        map.spawnRevives(i, this.getObjectId());
                    }
                }
                break;
            }
            default: {
                for (final int i : toSpawn) {
                    final Monster mob = LifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getPosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnRevives(mob, this.getObjectId());

                    if (mob.getId() == 9300216) {
                        map.broadcastMessage(ChannelPackets.environmentChange("Dojang/clear", 4));
                        map.broadcastMessage(ChannelPackets.environmentChange("dojang/end/clear", 3));
                    }
                }
                break;
            }
        }
    }

    public final boolean isAlive() {
        return hp > 0;
    }

    public final void setCarnivalTeam(final byte team) {
        carnivalTeam = team;
    }

    public final byte getCarnivalTeam() {
        return carnivalTeam;
    }

    public final ChannelCharacter getController() {
        return controller.get();
    }

    public final void setController(final ChannelCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public final void switchController(final ChannelCharacter newController, final boolean immediateAggro) {
        final ChannelCharacter controllers = getController();
        if (controllers == newController) {
            return;
        } else if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().write(MobPacket.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public final void addListener(final MonsterListener listener) {
        this.listener = listener;
    }

    public final boolean isControllerHasAggro() {
        return controllerHasAggro;
    }

    public final void setControllerHasAggro(final boolean controllerHasAggro) {
        this.controllerHasAggro = controllerHasAggro;
    }

    public final boolean isControllerKnowsAboutAggro() {
        return controllerKnowsAboutAggro;
    }

    public final void setControllerKnowsAboutAggro(final boolean controllerKnowsAboutAggro) {
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    @Override
    public final void sendSpawnData(final ChannelClient client) {
        if (!isAlive()) {
            return;
        }
        client.write(MobPacket.spawnMonster(this, -1, isFake ? 0xfc : 0, 0));
        if (statuses.size() > 0) {
            for (final MonsterStatusEffect mse : this.statuses.values()) {
                client.write(MobPacket.applyMonsterStatus(getObjectId(), mse));
            }
        }
    }

    @Override
    public final void sendDestroyData(final ChannelClient client) {
        client.write(MobPacket.killMonster(getObjectId(), 0));
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(stats.getName());
        sb.append("(");
        sb.append(getId());
        sb.append(") at X");
        sb.append(getPosition().x);
        sb.append("/ Y");
        sb.append(getPosition().y);
        sb.append(" with ");
        sb.append(getHp());
        sb.append("/ ");
        sb.append(getMobMaxHp());
        sb.append("hp, ");
        sb.append(getMp());
        sb.append("/ ");
        sb.append(getMobMaxMp());
        sb.append(" mp (alive: ");
        sb.append(isAlive());
        sb.append(" oid: ");
        sb.append(getObjectId());
        sb.append(") || Controller name : ");
        final ChannelCharacter chr = controller.get();
        sb.append(chr != null ? chr.getName() : "null");

        return sb.toString();
    }

    @Override
    public final GameMapObjectType getType() {
        return GameMapObjectType.MONSTER;
    }

    public final EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public final void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public final int getStatusSourceID(final MonsterStatus status) {
        final MonsterStatusEffect effect = statuses.get(status);
        if (effect != null) {
            return effect.getSkill().getId();
        }
        return -1;
    }

    public final ElementalEffectiveness getEffectiveness(final Element e) {
        if (statuses.size() > 0 && statuses.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL; // like blue snails
        }
        return stats.getEffectiveness(e);
    }

    public final void applyStatus(final ChannelCharacter from, final MonsterStatusEffect status, final boolean poison, final long duration, final boolean venom) {
        if (!isAlive()) {
            return;
        }
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
                return;
            case NORMAL:
            case WEAK:
                break;
            default:
                return;
        }
        // compos don't have an elemental (they have 2 - so we have to hack here...)
        final int statusSkill = status.getSkill().getId();
        switch (statusSkill) {
            case 2111006: { // FP compo
                switch (stats.getEffectiveness(Element.POISON)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
            case 2211006: { // IL compo
                switch (stats.getEffectiveness(Element.ICE)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
            case 4120005:
            case 4220005:
            case 14110004: {
                switch (stats.getEffectiveness(Element.POISON)) {
                    case WEAK:
                        return;
                }
                break;
            }
        }
        final Map<MonsterStatus, Integer> statis = status.getEffects();
        if (stats.isBoss()) {
            if (!(statis.containsKey(MonsterStatus.SPEED)
                    && statis.containsKey(MonsterStatus.NINJA_AMBUSH)
                    && statis.containsKey(MonsterStatus.WATK))) {
                return;
            }
        }
        for (MonsterStatus stat : statis.keySet()) {
            final MonsterStatusEffect oldEffect = statuses.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getEffects().isEmpty()) {
                    oldEffect.cancelTask();
                    oldEffect.cancelPoisonSchedule();
                }
            }
        }
        final TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public final void run() {
                if (isAlive()) {
                    map.broadcastMessage(MobPacket.cancelMonsterStatus(getObjectId(), statis), getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(Monster.this)) {
                        getController().getClient().write(MobPacket.cancelMonsterStatus(getObjectId(), statis));
                    }
                    for (final MonsterStatus stat : statis.keySet()) {
                        statuses.remove(stat);
                    }
                    setVenomMulti((byte) 0);
                }
                status.cancelPoisonSchedule();
            }
        };
        if (poison && getHp() > 1) {
            final int poisonDamage = Math.min(Short.MAX_VALUE, (int) (getMobMaxHp() / (70.0 - from.getCurrentSkillLevel(status.getSkill())) + 0.999));
            status.setEffect(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
            status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));
        } else if (venom) {
            int poisonLevel = 0;
            int matk = 0;

            switch (from.getJobId()) {
                case 412:
                    poisonLevel = from.getCurrentSkillLevel(SkillFactory.getSkill(4120005));
                    if (poisonLevel <= 0) {
                        return;
                    }
                    matk = SkillFactory.getSkill(4120005).getEffect(poisonLevel).getMatk();
                    break;
                case 422:
                    poisonLevel = from.getCurrentSkillLevel(SkillFactory.getSkill(4220005));
                    if (poisonLevel <= 0) {
                        return;
                    }
                    matk = SkillFactory.getSkill(4220005).getEffect(poisonLevel).getMatk();
                    break;
                case 1411:
                case 1412:
                    poisonLevel = from.getCurrentSkillLevel(SkillFactory.getSkill(14110004));
                    if (poisonLevel <= 0) {
                        return;
                    }
                    matk = SkillFactory.getSkill(14110004).getEffect(poisonLevel).getMatk();
                    break;
                default:
                    return; // Hack, using venom without the job required
            }
            final int luk = from.getStats().getLuk();
            final int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
            final int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
            int gap = maxDmg - minDmg;
            if (gap == 0) {
                gap = 1;
            }
            int poisonDamage = 0;
            for (int i = 0; i < getVenomMulti(); i++) {
                poisonDamage = poisonDamage + ((int) (gap * Math.random()) + minDmg);
            }
            poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
            status.setEffect(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
            status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));

        } else if (statusSkill == 4111003 || statusSkill == 14111001) { // shadow web
            status.setPoisonSchedule(timerManager.schedule(new PoisonTask((int) (getMobMaxHp() / 50.0 + 0.999), from, status, cancelTask, true), 3500));

        } else if (statusSkill == 4121004 || statusSkill == 4221004) {
            final int damage = (from.getStats().getStr() + from.getStats().getLuk()) * 2 * (60 / 100);
            status.setPoisonSchedule(timerManager.register(new PoisonTask(damage, from, status, cancelTask, false), 1000, 1000));
        }

        for (final MonsterStatus stat : statis.keySet()) {
            statuses.put(stat, status);
        }
        map.broadcastMessage(MobPacket.applyMonsterStatus(getObjectId(), status), getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().write(MobPacket.applyMonsterStatus(getObjectId(), status));
        }
        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration + status.getSkill().getAnimationTime());
        status.setCancelTask(schedule);
    }

    public final void applyMonsterBuff(final Map<MonsterStatus, Integer> stats, final int x, final int skillId, final long duration, final MobSkill skill, final List<Integer> reflection) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public final void run() {
                if (isAlive()) {
                    map.broadcastMessage(MobPacket.cancelMonsterStatus(getObjectId(), stats), getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(Monster.this)) {
                        getController().getClient().write(MobPacket.cancelMonsterStatus(getObjectId(), stats));
                    }
                    for (final MonsterStatus stat : stats.keySet()) {
                        statuses.remove(stat);
                    }
                }
            }
        };
        final MonsterStatusEffect effect = new MonsterStatusEffect(stats, null, skill, true);
        for (final MonsterStatus stat : stats.keySet()) {
            statuses.put(stat, effect);
        }
        if (reflection.size() > 0) {
            map.broadcastMessage(MobPacket.applyMonsterStatus(getObjectId(), effect, reflection), getPosition());
            if (getController() != null && !getController().isMapObjectVisible(this)) {
                getController().getClient().write(MobPacket.applyMonsterStatus(getObjectId(), effect, reflection));
            }
        } else {
            map.broadcastMessage(MobPacket.applyMonsterStatus(getObjectId(), effect), getPosition());
            if (getController() != null && !getController().isMapObjectVisible(this)) {
                getController().getClient().write(MobPacket.applyMonsterStatus(getObjectId(), effect));
            }
        }
        timerManager.schedule(cancelTask, duration);
    }

    public final void setTempEffectiveness(final Element e, final long milli) {
        stats.setEffectiveness(e, ElementalEffectiveness.WEAK);
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
			public void run() {
                stats.removeEffectiveness(e);
            }
        }, milli);
    }

    public final boolean isBuffed(final MonsterStatus status) {
        return statuses.containsKey(status);
    }

    public final void setFake(final boolean fake) {
        this.isFake = fake;
    }

    public final boolean isFake() {
        return isFake;
    }

    public final GameMap getMap() {
        return map;
    }

    public final List<SkillLevelEntry> getSkills() {
        return stats.getSkills();
    }

    public final boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public final long getLastSkillUsed(final int skillId) {
        if (usedSkills.containsKey(skillId)) {
            return usedSkills.get(skillId);
        }
        return 0;
    }

    public final void setLastSkillUsed(final int skillId, final long now, final long cooltime) {
        switch (skillId) {
            case 140:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(141, now);
                break;
            case 141:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(140, now + cooltime);
                break;
            default:
                usedSkills.put(skillId, now + cooltime);
                break;
        }
    }

    public final byte getNoSkills() {
        return stats.getNoSkills();
    }

    public final boolean isFirstAttack() {
        return stats.isFirstAttack();
    }

    public final int getBuffToGive() {
        return stats.getBuffToGive();
    }

    private final class PoisonTask implements Runnable {

        private final int poisonDamage;
        private final ChannelCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final GameMap map;

        private PoisonTask(final int poisonDamage, final ChannelCharacter chr, final MonsterStatusEffect status, final Runnable cancelTask, final boolean shadowWeb) {
            this.poisonDamage = poisonDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage = poisonDamage;
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    cancelTask.run();
                    status.cancelTask();
                }
            }
            if (hp > 1 && damage > 0) {
                damage(chr, damage, false);
                if (shadowWeb) {
                    map.broadcastMessage(MobPacket.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }

    private class AttackingMapleCharacter {

        private ChannelCharacter attacker;
        private long lastAttackTime;

        public AttackingMapleCharacter(final ChannelCharacter attacker, final long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public final long getLastAttackTime() {
            return lastAttackTime;
        }

        public final ChannelCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(ChannelCharacter from, int damage, boolean updateAttackTime);

        public int getDamage();

        public boolean contains(ChannelCharacter chr);

        public void killedMob(GameMap map, int baseExp, boolean mostDamage);
    }

    private final class SingleAttackerEntry implements AttackerEntry {

        private int damage;
        private int chrid;
        private long lastAttackTime;
        private ChannelServer cserv;

        public SingleAttackerEntry(final ChannelCharacter from, final ChannelServer cserv) {
            this.chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
                if (updateAttackTime) {
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public final List<AttackingMapleCharacter> getAttackers() {
            final ChannelCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final ChannelCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final GameMap map, final int baseExp, final boolean mostDamage) {
            final ChannelCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null && chr.getMap() == map && chr.isAlive()) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1, (byte) 0, (byte) 0);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static final class ExpMap {

        public final int exp;
        public final byte ptysize;
        public final byte CLASS_EXP;

        public ExpMap(final int exp, final byte ptysize, final byte CLASS_EXP) {
            super();
            this.exp = exp;
            this.ptysize = ptysize;
            this.CLASS_EXP = CLASS_EXP;
        }
    }

    private static final class OnePartyAttacker {

        public Party lastKnownParty;
        public int damage;
        public long lastAttackTime;

        public OnePartyAttacker(final Party lastKnownParty, final int damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private int totDamage;
        private final Map<Integer, OnePartyAttacker> attackers = new HashMap<>(6);
        private int partyid;
        private ChannelServer cserv;

        public PartyAttackerEntry(final int partyid, final ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
        }

        @Override
		public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final ChannelCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private Map<ChannelCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<ChannelCharacter, OnePartyAttacker> ret = new HashMap<>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final ChannelCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public final boolean contains(final ChannelCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public final int getDamage() {
            return totDamage;
        }

        @Override
		public void addDamage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public final void killedMob(final GameMap map, final int baseExp, final boolean mostDamage) {
            ChannelCharacter pchr, highest = null;
            int iDamage, iexp, highestDamage = 0;
            Party party;
            double averagePartyLevel, expWeight, levelMod, innerBaseExp, expFraction;
            List<ChannelCharacter> expApplicable;
            final Map<ChannelCharacter, ExpMap> expMap = new HashMap<>(6);
            byte CLASS_EXP;

            for (final Entry<ChannelCharacter, OnePartyAttacker> attacker : resolveAttackers().entrySet()) {
                party = attacker.getValue().lastKnownParty;
                averagePartyLevel = 0;

                CLASS_EXP = 0;
                expApplicable = new ArrayList<>();
                for (final PartyMember partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || stats.getLevel() - partychar.getLevel() <= 5) {
                        pchr = cserv.getPlayerStorage().getCharacterByName(partychar.getName());
                        if (pchr != null) {
                            if (pchr.isAlive() && pchr.getMap() == map) {
                                expApplicable.add(pchr);
                                averagePartyLevel += pchr.getLevel();

                                if (CLASS_EXP == 0) {
                                    CLASS_EXP = ServerConstants.CLASS_EXP(pchr.getJobId());
                                }
                            }
                        }
                    }
                }
                if (expApplicable.size() > 1) {
                    averagePartyLevel /= expApplicable.size();
                }
                iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                innerBaseExp = baseExp * ((double) iDamage / totDamage);
                expFraction = innerBaseExp / (expApplicable.size() + 1);

                for (final ChannelCharacter expReceiver : expApplicable) {
                    iexp = expMap.get(expReceiver) == null ? 0 : expMap.get(expReceiver).exp;
                    expWeight = (expReceiver == attacker.getKey() ? 2.0 : 0.7);
                    levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, new ExpMap(iexp, (byte) expApplicable.size(), CLASS_EXP));
                }
            }
            ExpMap expmap;
            for (final Entry<ChannelCharacter, ExpMap> expReceiver : expMap.entrySet()) {
                expmap = expReceiver.getValue();
                giveExpToCharacter(expReceiver.getKey(), expmap.exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expmap.ptysize, expmap.CLASS_EXP);
            }
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            if (partyid != other.partyid) {
                return false;
            }
            return true;
        }
    }
}
