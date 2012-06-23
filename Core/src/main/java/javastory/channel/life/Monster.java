package javastory.channel.life;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import javastory.game.AttackNature;
import javastory.game.Effectiveness;
import javastory.game.SkillLevelEntry;
import javastory.game.data.MobInfo;
import javastory.scripting.EventInstanceManager;
import javastory.server.TimerManager;
import javastory.server.handling.ServerConstants;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Monster extends AbstractLoadedLife {

	private MobInfo stats;
	private OverrideMonsterStats statsOverride = null;
	private int hp, mp;
//	private short showdown;
	private byte venomCounter, carnivalTeam;
	private GameMap map;
	private Monster sponge;
	// Just a reference for monster EXP distribution after death
	private ChannelCharacter highestDamageChar;
	private WeakReference<ChannelCharacter> controller = new WeakReference<>(null);
	private boolean isFake, dropsDisabled, controllerHasAggro, controllerKnowsAboutAggro;
	private final Collection<AttackerEntry> attackers = Lists.newLinkedList();
	private EventInstanceManager eventInstance;
	private MonsterListener listener = null;
	private EnumMap<AttackNature, Effectiveness> effectiveness = Maps.newEnumMap(AttackNature.class);
	private final Map<MonsterStatus, MonsterStatusEffect> statuses = Maps.newEnumMap(MonsterStatus.class);
// 	private final List<MonsterStatusEffect> activeEffects = new ArrayList<MonsterStatusEffect>();
// 	private final List<MonsterStatus> monsterBuffs = new ArrayList<MonsterStatus>();
	private Map<Integer, Long> usedSkills;

	public Monster(final MobInfo stats) {
		super(stats.getMobId());
		this.initWithStats(stats);
	}

	public Monster(final Monster monster) {
		super(monster);
		this.initWithStats(monster.stats);
	}

	private void initWithStats(final MobInfo stats) {
		this.setStance(5);
		this.stats = stats;
		this.hp = stats.getHp();
		this.mp = stats.getMp();
		this.venomCounter = 0;
		// showdown = 100;
		this.carnivalTeam = -1;
		this.isFake = false;
		this.dropsDisabled = false;

		if (stats.getNoSkills() > 0) {
			this.usedSkills = Maps.newHashMap();
		}

		this.effectiveness = Maps.newEnumMap(stats.getEffectivenessMap());
	}

	public final MobInfo getStats() {
		return this.stats;
	}

	public final void disableDrops() {
		this.dropsDisabled = true;
	}

	public final boolean dropsDisabled() {
		return this.dropsDisabled;
	}

	public final void setSponge(final Monster mob) {
		this.sponge = mob;
	}

	public final void setMap(final GameMap map) {
		this.map = map;
	}

	public final int getHp() {
		return this.hp;
	}

	public final void setHp(final int hp) {
		this.hp = hp;
	}

	public final int getMobMaxHp() {
		if (this.statsOverride != null) {
			return this.statsOverride.getHp();
		}
		return this.stats.getHp();
	}

	public final int getMp() {
		return this.mp;
	}

	public final void setMp(int mp) {
		if (mp < 0) {
			mp = 0;
		}
		this.mp = mp;
	}

	public final int getMobMaxMp() {
		if (this.statsOverride != null) {
			return this.statsOverride.getMp();
		}
		return this.stats.getMp();
	}

	public final int getMobExp() {
		if (this.statsOverride != null) {
			return this.statsOverride.getExp();
		}
		return this.stats.getExp();
	}

	public final void setOverrideStats(final OverrideMonsterStats ostats) {
		this.statsOverride = ostats;
		this.hp = ostats.getHp();
		this.mp = ostats.getMp();
	}

	public final Monster getSponge() {
		return this.sponge;
	}

	public final byte getVenomMulti() {
		return this.venomCounter;
	}

	public final void setVenomMulti(final byte venom_counter) {
		this.venomCounter = venom_counter;
	}

	public final void damage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
		if (damage <= 0 || !this.isAlive()) {
			return;
		}
		AttackerEntry attacker = null;

		final PartyMember member = from.getPartyMembership();
		if (member != null) {
			member.getPartyId();
			attacker = new PartyAttackerEntry(member.getPartyId());
		} else {
			attacker = new SingleAttackerEntry(from);
		}
		boolean replaced = false;
		for (final AttackerEntry aentry : this.attackers) {
			if (aentry.equals(attacker)) {
				attacker = aentry;
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			this.attackers.add(attacker);
		}
		final int rDamage = Math.max(0, Math.min(damage, this.hp));
		attacker.addDamage(from, rDamage, updateAttackTime);

		if (this.stats.getSelfD() != -1) {
			this.hp -= rDamage;
			if (this.hp > 0) {
				if (this.hp < this.stats.getSelfDHp()) {
					// HP is below the self-destruction level
					this.map.killMonster(this, from, false, false, this.stats.getSelfD());
				} else { // Show HP
					for (final AttackerEntry mattacker : this.attackers) {
						for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
							if (cattacker.getAttacker().getMapId() == from.getMapId()) {
								// current attacker is on the map of the monster
								if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
									cattacker.getAttacker().getClient().write(
										MobPacket.showMonsterHP(this.getObjectId(), (int) Math.ceil(this.hp * 100.0 / this.getMobMaxHp())));
								}
							}
						}
					}
				}
			} else {
				// Character killed it without explosing :(
				this.map.killMonster(this, from, true, false, (byte) 1);
			}
		} else {
			if (this.sponge != null) {
				if (this.sponge.hp > 0) {
					// If it's still alive, don't want double/triple rewards
					// Sponge are always in the same map, so we can use this.map
					// The only mob that uses sponge are PB/HT
					this.sponge.hp -= rDamage;

					if (this.sponge.hp <= 0) {
						this.map.killMonster(this.sponge, from, true, false, (byte) 1);
					} else {
						this.map.broadcastMessage(MobPacket.showBossHP(this.sponge));
					}
				}
			}
			if (this.hp > 0) {
				this.hp -= rDamage;

				switch (this.stats.getHpDisplayType()) {
				case 0:
					this.map.broadcastMessage(MobPacket.showBossHP(this), this.getPosition());
					break;
				case 1:
					this.map.broadcastMessage(MobPacket.damageFriendlyMob(this, damage), this.getPosition());
					break;
				case 2:
					this.map.broadcastMessage(MobPacket.showMonsterHP(this.getObjectId(), (int) Math.ceil(this.hp * 100.0 / this.getMobMaxHp())));
					from.mulung_EnergyModify(true);
					break;
				case 3:
					for (final AttackerEntry mattacker : this.attackers) {
						for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
							if (cattacker.getAttacker().getMap() == from.getMap()) {
								// current attacker is on the map of the monster
								if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
									cattacker.getAttacker().getClient().write(
										MobPacket.showMonsterHP(this.getObjectId(), (int) Math.ceil(this.hp * 100.0 / this.getMobMaxHp())));
								}
							}
						}
					}
					break;
				}

				if (this.hp <= 0) {
					this.map.killMonster(this, from, true, false, (byte) 1);
				}
			}
		}
	}

	public final void heal(final int hp, final int mp, final boolean broadcast) {
		final int TotalHP = this.getHp() + hp;
		final int TotalMP = this.getMp() + mp;

		if (TotalHP >= this.getMobMaxHp()) {
			this.setHp(this.getMobMaxHp());
		} else {
			this.setHp(TotalHP);
		}
		if (TotalMP >= this.getMp()) {
			this.setMp(this.getMp());
		} else {
			this.setMp(TotalMP);
		}
		if (broadcast) {
			this.map.broadcastMessage(MobPacket.healMonster(this.getObjectId(), hp));
		} else if (this.sponge != null) {
			// else if, since only sponge doesn't broadcast
			this.sponge.hp += hp;
		}
	}

	private void giveExpToCharacter(final ChannelCharacter attacker, int exp, final boolean highestDamage, final int numExpSharers, final byte pty,
		final byte CLASS_EXP_PERCENT) {
		if (highestDamage) {
			if (this.eventInstance != null) {
				this.eventInstance.monsterKilled(attacker, this);
			} else {
				final EventInstanceManager em = attacker.getEventInstance();
				if (em != null) {
					em.monsterKilled(attacker, this);
				}
			}
			this.highestDamageChar = attacker;
		}
		if (exp > 0) {
			final Integer holySymbol = attacker.getBuffedValue(BuffStat.HOLY_SYMBOL);
			if (holySymbol != null) {
				if (numExpSharers == 1) {
					exp *= 1.0 + holySymbol.doubleValue() / 500.0;
				} else {
					exp *= 1.0 + holySymbol.doubleValue() / 100.0;
				}
			}
			int CLASS_EXP = 0;
			if (CLASS_EXP_PERCENT > 0) {
				CLASS_EXP = (int) ((exp / 100.0f) * CLASS_EXP_PERCENT);
			}
			if (attacker.hasDisease(Disease.CURSE)) {
				exp /= 2;
			}
			attacker.gainExpMonster(exp, true, highestDamage, pty, CLASS_EXP);
		}
		attacker.mobKilled(this.getId());
	}

	public final ChannelCharacter killBy(final ChannelCharacter killer) {
		final int totalBaseExp = Math.min(Integer.MAX_VALUE, (int) (this.getMobExp() * ChannelServer.getInstance().getExpRate()));
		AttackerEntry highest = null;
		int highdamage = 0;
		for (final AttackerEntry attackEntry : this.attackers) {
			if (attackEntry.getDamage() > highdamage) {
				highest = attackEntry;
				highdamage = attackEntry.getDamage();
			}
		}
		int baseExp;
		for (final AttackerEntry attackEntry : this.attackers) {
			baseExp = (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / this.getMobMaxHp()));
			attackEntry.killedMob(killer.getMap(), baseExp, attackEntry == highest);
		}
		final ChannelCharacter controll = this.controller.get();
		if (controll != null) { // this can/should only happen when a hidden gm
								// attacks the monster
			controll.getClient().write(MobPacket.stopControllingMonster(this.getObjectId()));
			controll.stopControllingMonster(this);
		}
		this.spawnRevives(killer.getMap());
		if (this.eventInstance != null) {
			this.eventInstance.unregisterMonster(this);
			this.eventInstance = null;
		}
		this.sponge = null;

		if (this.listener != null) {
			this.listener.monsterKilled();
		}
		final ChannelCharacter ret = this.highestDamageChar;
		this.highestDamageChar = null; // may not keep hard references to chars
									// outside of PlayerStorage or MapleMap
		return ret;
	}

	public final void spawnRevives(final GameMap map) {
		final List<Integer> toSpawn = this.stats.getRevives();

		if (toSpawn == null) {
			return;
		}
		switch (this.getId()) {
		case 8810026:
		case 8820009:
		case 8820010:
		case 8820011:
		case 8820012:
		case 8820013: {
			final List<Monster> mobs = Lists.newArrayList();
			Monster spongy = null;

			for (final int i : toSpawn) {
				final Monster mob = LifeFactory.getMonster(i);

				mob.setPosition(this.getPosition());
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

				if (this.eventInstance != null) {
					this.eventInstance.registerMonster(mob);
				}
				mob.setPosition(this.getPosition());
				if (this.dropsDisabled()) {
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
		return this.hp > 0;
	}

	public final void setCarnivalTeam(final byte team) {
		this.carnivalTeam = team;
	}

	public final byte getCarnivalTeam() {
		return this.carnivalTeam;
	}

	public final ChannelCharacter getController() {
		return this.controller.get();
	}

	public final void setController(final ChannelCharacter controller) {
		this.controller = new WeakReference<>(controller);
	}

	public final void switchController(final ChannelCharacter newController, final boolean immediateAggro) {
		final ChannelCharacter controllers = this.getController();
		if (controllers == newController) {
			return;
		} else if (controllers != null) {
			controllers.stopControllingMonster(this);
			controllers.getClient().write(MobPacket.stopControllingMonster(this.getObjectId()));
		}
		newController.controlMonster(this, immediateAggro);
		this.setController(newController);
		if (immediateAggro) {
			this.setControllerHasAggro(true);
		}
		this.setControllerKnowsAboutAggro(false);
	}

	public final void addListener(final MonsterListener listener) {
		this.listener = listener;
	}

	public final boolean isControllerHasAggro() {
		return this.controllerHasAggro;
	}

	public final void setControllerHasAggro(final boolean controllerHasAggro) {
		this.controllerHasAggro = controllerHasAggro;
	}

	public final boolean isControllerKnowsAboutAggro() {
		return this.controllerKnowsAboutAggro;
	}

	public final void setControllerKnowsAboutAggro(final boolean controllerKnowsAboutAggro) {
		this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
	}

	@Override
	public final void sendSpawnData(final ChannelClient client) {
		if (!this.isAlive()) {
			return;
		}
		client.write(MobPacket.spawnMonster(this, -1, this.isFake ? 0xfc : 0, 0));
		if (this.statuses.size() > 0) {
			for (final MonsterStatusEffect mse : this.statuses.values()) {
				client.write(MobPacket.applyMonsterStatus(this.getObjectId(), mse));
			}
		}
	}

	@Override
	public final void sendDestroyData(final ChannelClient client) {
		client.write(MobPacket.killMonster(this.getObjectId(), 0));
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append(this.stats.getName());
		sb.append("(");
		sb.append(this.getId());
		sb.append(") at X");
		sb.append(this.getPosition().x);
		sb.append("/ Y");
		sb.append(this.getPosition().y);
		sb.append(" with ");
		sb.append(this.getHp());
		sb.append("/ ");
		sb.append(this.getMobMaxHp());
		sb.append("hp, ");
		sb.append(this.getMp());
		sb.append("/ ");
		sb.append(this.getMobMaxMp());
		sb.append(" mp (alive: ");
		sb.append(this.isAlive());
		sb.append(" oid: ");
		sb.append(this.getObjectId());
		sb.append(") || Controller name : ");
		final ChannelCharacter chr = this.controller.get();
		sb.append(chr != null ? chr.getName() : "null");

		return sb.toString();
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.MONSTER;
	}

	public final EventInstanceManager getEventInstance() {
		return this.eventInstance;
	}

	public final void setEventInstance(final EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}

	public final int getStatusSourceID(final MonsterStatus status) {
		final MonsterStatusEffect effect = this.statuses.get(status);
		if (effect != null) {
			return effect.getSkill().getId();
		}
		return -1;
	}

	public final Effectiveness getEffectiveness(final AttackNature e) {
		if (this.statuses.size() > 0 && this.statuses.get(MonsterStatus.DOOM) != null) {
			return Effectiveness.NORMAL; // like blue snails
		}
		return this.effectiveness.get(e);
	}

	public final void applyStatus(final ChannelCharacter from, final MonsterStatusEffect status, final boolean poison, final long duration, final boolean venom) {
		if (!this.isAlive()) {
			return;
		}
		final AttackNature element = status.getSkill().getElement();
		switch (this.effectiveness.get(element)) {
		case IMMUNE:
		case STRONG:
			return;
		case NORMAL:
		case WEAK:
			break;
		default:
			return;
		}
		
		// compos don't have an elemental 
		// (they have 2 - so we have to hack here...)
		final int statusSkill = status.getSkill().getId();
		switch (statusSkill) {
		case 2111006: { // FP compo
			switch (this.effectiveness.get(AttackNature.POISON)) {
			case IMMUNE:
			case STRONG:
				return;
			}
			break;
		}
		case 2211006: { // IL compo
			switch (this.effectiveness.get(AttackNature.ICE)) {
			case IMMUNE:
			case STRONG:
				return;
			}
			break;
		}
		case 4120005:
		case 4220005:
		case 14110004: {
			switch (this.effectiveness.get(AttackNature.POISON)) {
			case WEAK:
				return;
			}
			break;
		}
		}
		final Map<MonsterStatus, Integer> statusEffects = status.getEffects();
		if (this.stats.isBoss()) {
			final boolean hasSpeed = statusEffects.containsKey(MonsterStatus.SPEED);
			final boolean hasNinjaAmbush = statusEffects.containsKey(MonsterStatus.NINJA_AMBUSH);
			final boolean hasWatk = statusEffects.containsKey(MonsterStatus.WATK);
			if (hasSpeed || hasNinjaAmbush || hasWatk) {
				return;
			}
		}
		for (final MonsterStatus stat : statusEffects.keySet()) {
			final MonsterStatusEffect oldEffect = this.statuses.get(stat);
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
				if (Monster.this.isAlive()) {
					Monster.this.map.broadcastMessage(MobPacket.cancelMonsterStatus(Monster.this.getObjectId(), statusEffects), Monster.this.getPosition());
					if (Monster.this.getController() != null && !Monster.this.getController().isMapObjectVisible(Monster.this)) {
						Monster.this.getController().getClient().write(MobPacket.cancelMonsterStatus(Monster.this.getObjectId(), statusEffects));
					}
					for (final MonsterStatus stat : statusEffects.keySet()) {
						Monster.this.statuses.remove(stat);
					}
					Monster.this.setVenomMulti((byte) 0);
				}
				status.cancelPoisonSchedule();
			}
		};
		if (poison && this.getHp() > 1) {
			final int poisonDamage = Math.min(Short.MAX_VALUE, (int) (this.getMobMaxHp() / (70.0 - from.getCurrentSkillLevel(status.getSkill())) + 0.999));
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
			for (int i = 0; i < this.getVenomMulti(); i++) {
				poisonDamage = poisonDamage + (int) (gap * Math.random()) + minDmg;
			}
			poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
			status.setEffect(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
			status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));

		} else if (statusSkill == 4111003 || statusSkill == 14111001) { // shadow
																		// web
			status.setPoisonSchedule(timerManager.schedule(new PoisonTask((int) (this.getMobMaxHp() / 50.0 + 0.999), from, status, cancelTask, true), 3500));

		} else if (statusSkill == 4121004 || statusSkill == 4221004) {
			final int damage = (from.getStats().getStr() + from.getStats().getLuk()) * 2 * (60 / 100);
			status.setPoisonSchedule(timerManager.register(new PoisonTask(damage, from, status, cancelTask, false), 1000, 1000));
		}

		for (final MonsterStatus stat : statusEffects.keySet()) {
			this.statuses.put(stat, status);
		}
		this.map.broadcastMessage(MobPacket.applyMonsterStatus(this.getObjectId(), status), this.getPosition());
		if (this.getController() != null && !this.getController().isMapObjectVisible(this)) {
			this.getController().getClient().write(MobPacket.applyMonsterStatus(this.getObjectId(), status));
		}
		final ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration + status.getSkill().getAnimationTime());
		status.setCancelTask(schedule);
	}

	public final void applyMonsterBuff(final Map<MonsterStatus, Integer> stats, final int x, final int skillId, final long duration, final MobSkill skill,
		final List<Integer> reflection) {
		final TimerManager timerManager = TimerManager.getInstance();
		final Runnable cancelTask = new Runnable() {

			@Override
			public final void run() {
				if (Monster.this.isAlive()) {
					Monster.this.map.broadcastMessage(MobPacket.cancelMonsterStatus(Monster.this.getObjectId(), stats), Monster.this.getPosition());
					if (Monster.this.getController() != null && !Monster.this.getController().isMapObjectVisible(Monster.this)) {
						Monster.this.getController().getClient().write(MobPacket.cancelMonsterStatus(Monster.this.getObjectId(), stats));
					}
					for (final MonsterStatus stat : stats.keySet()) {
						Monster.this.statuses.remove(stat);
					}
				}
			}
		};
		final MonsterStatusEffect effect = new MonsterStatusEffect(stats, null, skill, true);
		for (final MonsterStatus stat : stats.keySet()) {
			this.statuses.put(stat, effect);
		}
		if (reflection.size() > 0) {
			this.map.broadcastMessage(MobPacket.applyMonsterStatus(this.getObjectId(), effect, reflection), this.getPosition());
			if (this.getController() != null && !this.getController().isMapObjectVisible(this)) {
				this.getController().getClient().write(MobPacket.applyMonsterStatus(this.getObjectId(), effect, reflection));
			}
		} else {
			this.map.broadcastMessage(MobPacket.applyMonsterStatus(this.getObjectId(), effect), this.getPosition());
			if (this.getController() != null && !this.getController().isMapObjectVisible(this)) {
				this.getController().getClient().write(MobPacket.applyMonsterStatus(this.getObjectId(), effect));
			}
		}
		timerManager.schedule(cancelTask, duration);
	}

	public final void setTempEffectiveness(final AttackNature element, final long milli) {
		this.effectiveness.put(element, Effectiveness.WEAK);
		TimerManager.getInstance().schedule(new EffectivenessExpiration(element), milli);
	}

	public final boolean isBuffed(final MonsterStatus status) {
		return this.statuses.containsKey(status);
	}

	public final void setFake(final boolean fake) {
		this.isFake = fake;
	}

	public final boolean isFake() {
		return this.isFake;
	}

	public final GameMap getMap() {
		return this.map;
	}

	public final List<SkillLevelEntry> getSkills() {
		return this.stats.getSkills();
	}

	public final boolean hasSkill(final int skillId, final int level) {
		return this.stats.hasSkill(skillId, level);
	}

	public final long getLastSkillUsed(final int skillId) {
		if (this.usedSkills.containsKey(skillId)) {
			return this.usedSkills.get(skillId);
		}
		return 0;
	}

	public final void setLastSkillUsed(final int skillId, final long now, final long cooltime) {
		switch (skillId) {
		case 140:
			this.usedSkills.put(skillId, now + cooltime * 2);
			this.usedSkills.put(141, now);
			break;
		case 141:
			this.usedSkills.put(skillId, now + cooltime * 2);
			this.usedSkills.put(140, now + cooltime);
			break;
		default:
			this.usedSkills.put(skillId, now + cooltime);
			break;
		}
	}

	public final byte getNoSkills() {
		return this.stats.getNoSkills();
	}

	public final boolean isFirstAttack() {
		return this.stats.isFirstAttack();
	}

	public final int getBuffToGive() {
		return this.stats.getBuffToGive();
	}

	private final class EffectivenessExpiration implements Runnable {
		private final AttackNature e;

		private EffectivenessExpiration(final AttackNature e) {
			this.e = e;
		}

		@Override
		public void run() {
			Monster.this.effectiveness.remove(this.e);
		}
	}

	private final class PoisonTask implements Runnable {

		private final int poisonDamage;
		private final ChannelCharacter chr;
		private final MonsterStatusEffect status;
		private final Runnable cancelTask;
		private final boolean shadowWeb;
		private final GameMap map;

		private PoisonTask(final int poisonDamage, final ChannelCharacter chr, final MonsterStatusEffect status, final Runnable cancelTask,
			final boolean shadowWeb) {
			this.poisonDamage = poisonDamage;
			this.chr = chr;
			this.status = status;
			this.cancelTask = cancelTask;
			this.shadowWeb = shadowWeb;
			this.map = chr.getMap();
		}

		@Override
		public void run() {
			int damage = this.poisonDamage;
			if (damage >= Monster.this.hp) {
				damage = Monster.this.hp - 1;
				if (!this.shadowWeb) {
					this.cancelTask.run();
					this.status.cancelTask();
				}
			}
			if (Monster.this.hp > 1 && damage > 0) {
				Monster.this.damage(this.chr, damage, false);
				if (this.shadowWeb) {
					this.map.broadcastMessage(MobPacket.damageMonster(Monster.this.getObjectId(), damage), Monster.this.getPosition());
				}
			}
		}
	}

	private static class AttackingMapleCharacter {

		private final ChannelCharacter attacker;
		private final long lastAttackTime;

		public AttackingMapleCharacter(final ChannelCharacter attacker, final long lastAttackTime) {
			super();
			this.attacker = attacker;
			this.lastAttackTime = lastAttackTime;
		}

		public final long getLastAttackTime() {
			return this.lastAttackTime;
		}

		public final ChannelCharacter getAttacker() {
			return this.attacker;
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
		private final int chrid;
		private long lastAttackTime;

		public SingleAttackerEntry(final ChannelCharacter from) {
			this.chrid = from.getId();
		}

		@Override
		public void addDamage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
			if (this.chrid == from.getId()) {
				this.damage += damage;
				if (updateAttackTime) {
					this.lastAttackTime = System.currentTimeMillis();
				}
			}
		}

		@Override
		public final List<AttackingMapleCharacter> getAttackers() {
			final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterById(this.chrid);
			if (chr != null) {
				return Collections.singletonList(new AttackingMapleCharacter(chr, this.lastAttackTime));
			} else {
				return Collections.emptyList();
			}
		}

		@Override
		public boolean contains(final ChannelCharacter chr) {
			return this.chrid == chr.getId();
		}

		@Override
		public int getDamage() {
			return this.damage;
		}

		@Override
		public void killedMob(final GameMap map, final int baseExp, final boolean mostDamage) {
			final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterById(this.chrid);
			if (chr != null && chr.getMap() == map && chr.isAlive()) {
				Monster.this.giveExpToCharacter(chr, baseExp, mostDamage, 1, (byte) 0, (byte) 0);
			}
		}

		@Override
		public int hashCode() {
			return this.chrid;
		}

		@Override
		public final boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			final SingleAttackerEntry other = (SingleAttackerEntry) obj;
			return this.chrid == other.chrid;
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
		private final Map<Integer, OnePartyAttacker> attackers = Maps.newHashMapWithExpectedSize(6);
		private final int partyid;

		public PartyAttackerEntry(final int partyid) {
			this.partyid = partyid;
		}

		@Override
		public List<AttackingMapleCharacter> getAttackers() {
			final List<AttackingMapleCharacter> ret = Lists.newArrayListWithCapacity(this.attackers.size());
			for (final Entry<Integer, OnePartyAttacker> entry : this.attackers.entrySet()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterById(entry.getKey());
				if (chr != null) {
					ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
				}
			}
			return ret;
		}

		private Map<ChannelCharacter, OnePartyAttacker> resolveAttackers() {
			final Map<ChannelCharacter, OnePartyAttacker> ret = Maps.newHashMapWithExpectedSize(this.attackers.size());
			for (final Entry<Integer, OnePartyAttacker> aentry : this.attackers.entrySet()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterById(aentry.getKey());
				if (chr != null) {
					ret.put(chr, aentry.getValue());
				}
			}
			return ret;
		}

		@Override
		public final boolean contains(final ChannelCharacter chr) {
			return this.attackers.containsKey(chr.getId());
		}

		@Override
		public final int getDamage() {
			return this.totDamage;
		}

		@Override
		public void addDamage(final ChannelCharacter from, final int damage, final boolean updateAttackTime) {
			final OnePartyAttacker oldPartyAttacker = this.attackers.get(from.getId());
			if (oldPartyAttacker != null) {
				oldPartyAttacker.damage += damage;
				oldPartyAttacker.lastKnownParty = from.getParty();
				if (updateAttackTime) {
					oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
				}
			} else {
				// TODO actually this causes wrong behavior when the party
				// changes between attacks only the last setup will get exp -
				// but otherwise we'd have to store the full party constellation
				// for every attack or every time it changes, might be
				// wanted/needed in the future but not now
				final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
				this.attackers.put(from.getId(), onePartyAttacker);
				if (!updateAttackTime) {
					onePartyAttacker.lastAttackTime = 0;
				}
			}
			this.totDamage += damage;
		}

		@Override
		public final void killedMob(final GameMap map, final int baseExp, final boolean mostDamage) {
			ChannelCharacter pchr, highest = null;
			int iDamage, iexp, highestDamage = 0;
			Party party;
			double averagePartyLevel, expWeight, levelMod, innerBaseExp, expFraction;
			List<ChannelCharacter> expApplicable;
			final Map<ChannelCharacter, ExpMap> expMap = Maps.newHashMapWithExpectedSize(6);
			byte CLASS_EXP;

			for (final Entry<ChannelCharacter, OnePartyAttacker> attacker : this.resolveAttackers().entrySet()) {
				party = attacker.getValue().lastKnownParty;
				averagePartyLevel = 0;

				CLASS_EXP = 0;
				expApplicable = Lists.newArrayList();
				for (final PartyMember partychar : party.getMembers()) {
					if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || Monster.this.stats.getLevel() - partychar.getLevel() <= 5) {
						pchr = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
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
				innerBaseExp = baseExp * ((double) iDamage / this.totDamage);
				expFraction = innerBaseExp / (expApplicable.size() + 1);

				for (final ChannelCharacter expReceiver : expApplicable) {
					iexp = expMap.get(expReceiver) == null ? 0 : expMap.get(expReceiver).exp;
					expWeight = expReceiver == attacker.getKey() ? 2.0 : 0.7;
					levelMod = expReceiver.getLevel() / averagePartyLevel;
					if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
						levelMod = 1.0;
					}
					iexp += (int) Math.round(expFraction * expWeight * levelMod);
					expMap.put(expReceiver, new ExpMap(iexp, (byte) expApplicable.size(), CLASS_EXP));
				}
			}
			ExpMap expmap;
			for (final Entry<ChannelCharacter, ExpMap> expReceiver : expMap.entrySet()) {
				expmap = expReceiver.getValue();
				Monster.this.giveExpToCharacter(expReceiver.getKey(), expmap.exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expmap.ptysize,
					expmap.CLASS_EXP);
			}
		}

		@Override
		public final int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.partyid;
			return result;
		}

		@Override
		public final boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			final PartyAttackerEntry other = (PartyAttackerEntry) obj;
			if (this.partyid != other.partyid) {
				return false;
			}
			return true;
		}
	}
}
