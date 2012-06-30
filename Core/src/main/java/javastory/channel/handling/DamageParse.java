package javastory.channel.handling;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.anticheat.CheatTracker;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.BuffStat;
import javastory.channel.client.ISkill;
import javastory.channel.client.MonsterStatus;
import javastory.channel.client.MonsterStatusEffect;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapItem;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.ItemDropType;
import javastory.channel.server.StatEffect;
import javastory.game.AttackType;
import javastory.game.AttackNature;
import javastory.game.Skills;
import javastory.game.data.MobInfo;
import javastory.game.data.SkillInfoProvider;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.server.TimerManager;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public final class DamageParse {

	private final static int[] charges = { 1211005, 1211006 };

	private DamageParse() {
	}

	public static void applyAttack(final AttackInfo attack, final ISkill theSkill, final ChannelCharacter player, int attackCount,
		final double maxDamagePerMonster, final StatEffect effect, final AttackType attack_type) {
		if (!player.isAlive()) {
			player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
			return;
		}
		player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);

		if (attack.skill != 0) {
			if (effect == null) {
				player.getClient().write(ChannelPackets.enableActions());
				return;
			}
			if (Skills.isMulungSkill(attack.skill)) {
				if (player.getMapId() / 10000 != 92502) {
					// AutobanManager.getInstance().autoban(player.getClient(),
					// "Using Mu Lung dojo skill out of dojo maps.");
				} else {
					player.mulung_EnergyModify(false);
				}
			}
			if (attack.targets > effect.getMobCount()) { // Must be done here,
															// since NPE with
															// normal atk
				player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
				return;
			}
		}
		if (attack.hits > attackCount) {
			if (attack.skill != 4211006) {
				player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
				return;
			}
		}
		int totDamage = 0;
		final GameMap map = player.getMap();

		if (attack.skill == 4211006) { // meso explosion
			for (final AttackPair oned : attack.allDamage) {
				if (oned.attack != null) {
					continue;
				}
				final GameMapObject mapobject = map.getMapObject(oned.objectid);

				if (mapobject != null && mapobject.getType() == GameMapObjectType.ITEM) {
					final GameMapItem mapitem = (GameMapItem) mapobject;

					if (mapitem.getMeso() > 0) {
						if (mapitem.isPickedUp()) {
							return;
						}
						map.removeMapObject(mapitem);
						map.broadcastMessage(ChannelPackets.explodeDrop(mapitem.getObjectId()));
						mapitem.setPickedUp(true);
					} else {
						player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
						return;
					}
				} else {
					player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
					return; // etc explosion, exploding nonexistant things, etc.
				}
			}
		}
		int fixeddmg, totDamageToOneMonster;
		final ActivePlayerStats stats = player.getStats();

		int CriticalDamage = stats.passive_sharpeye_percent();

		final Integer SharpEye = player.getBuffedSkill_Y(BuffStat.SHARP_EYES);
		if (SharpEye != null) {
			CriticalDamage += SharpEye - 100; // Additional damage in percentage
		}
		final Integer SharpEye_ = player.getBuffedSkill_Y(BuffStat.THORNS);
		if (SharpEye_ != null) {
			CriticalDamage += SharpEye_ - 100; // Additional damage in
												// percentage
		}
		byte ShdowPartnerAttackPercentage = 0;
		if (attack_type == AttackType.RANGED_WITH_SHADOWPARTNER || attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
			final ISkill SP;
			if (attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
				SP = SkillInfoProvider.getSkill(4331002);
			} else {
				switch (player.getJobId()) {
				case 1410: // NightWalker
				case 1411:
				case 1412:
					SP = SkillInfoProvider.getSkill(14111000);
					break;
				default:
					SP = SkillInfoProvider.getSkill(4111002);
					break;
				} // x = normal atk, y = skill
			}
			final StatEffect shadowPartnerEffect = SP.getEffect(player.getCurrentSkillLevel(SP));
			if (attack.skill != 0) {
				ShdowPartnerAttackPercentage = (byte) shadowPartnerEffect.getY();
			} else {
				ShdowPartnerAttackPercentage = (byte) shadowPartnerEffect.getX();
			}
			attackCount /= 2; // hack xD
		}

		byte overallAttackCount; // Tracking of Shadow Partner additional
									// damage.
		double maxDamagePerHit = 0;
		Monster monster;
		MobInfo monsterstats;
		boolean Tempest;

		for (final AttackPair oned : attack.allDamage) {
			monster = map.getMonsterByOid(oned.objectid);

			if (monster != null) {
				totDamageToOneMonster = 0;
				monsterstats = monster.getStats();
				fixeddmg = monsterstats.getFixedDamage();
				Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006;

				if (!Tempest) {
					if (!monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
						maxDamagePerHit = CalculateMaxWeaponDamagePerHit(player, monster, attack, theSkill, effect, maxDamagePerMonster, CriticalDamage);
					} else {
						maxDamagePerHit = 1;
					}
				}
				overallAttackCount = 0; // Tracking of Shadow Partner additional
										// damage.

				for (Integer eachd : oned.attack) {
					overallAttackCount++;

					if (overallAttackCount - 1 == attackCount) { // Is a Shadow
																	// partner
																	// hit so
																	// let's
																	// divide it
																	// once
						maxDamagePerHit = maxDamagePerHit / 100 * ShdowPartnerAttackPercentage;
					}
					// System.out.println("Client damage : " + eachd +
					// " Server : " + maxDamagePerHit);
					if (fixeddmg != -1) {
						if (monsterstats.getOnlyNoramlAttack()) {
							eachd = attack.skill != 0 ? 0 : fixeddmg;
						} else {
							eachd = fixeddmg;
						}
					} else {
						if (monsterstats.getOnlyNoramlAttack()) {
							eachd = attack.skill != 0 ? 0 : Math.min(eachd, (int) maxDamagePerHit); // Convert
																									// to
																									// server
																									// calculated
																									// damage
						} else {
							if (Tempest) { // Monster buffed with Tempest
								if (eachd > monster.getMobMaxHp()) {
									eachd = monster.getMobMaxHp();
									player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
								}
							} else {
								if (eachd > maxDamagePerHit) {
									player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
									if (eachd > maxDamagePerHit * 2) {
										eachd = (int) maxDamagePerHit; // Convert
																		// to
																		// server
																		// calculated
																		// damage
										player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_3);
									}
								}
							}
						}
					}
					totDamageToOneMonster += eachd;
				}
				totDamage += totDamageToOneMonster;
				player.checkMonsterAggro(monster);

				if (player.getPosition().distanceSq(monster.getPosition()) > 400000.0) { // 600^2,
																							// 550
																							// is
																							// approximatly
																							// the
																							// range
																							// of
																							// ultis
					player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER); // ,
																										// Double.toString(Math.sqrt(distance))
				}
				// pickpocket
				if (player.getBuffedValue(BuffStat.PICKPOCKET) != null) {
					switch (attack.skill) {
					case 0:
					case 4001334:
					case 4201005:
					case 4211002:
					case 4211004:
					case 4221003:
					case 4221007:
						handlePickPocket(player, monster, oned);
						break;
					}
				}
				if (totDamageToOneMonster > 0) {
					if (attack.skill == 3221007) {
						if (player.isOnDMG()) {
							player.sendNotice(5, "Damage: " + player.getSnipeDamage());
						}
						monster.damage(player, player.getSnipeDamage(), true);
					} else {
						if (totDamageToOneMonster >= 199999) {
							// HACK
							// Damage formula
							totDamageToOneMonster = Math.min(ChannelCharacter.damageCap, Math.max(totDamageToOneMonster, totDamageToOneMonster
								* (player.getStats().getTotalWatk() / 50)));
							if (player.isOnDMG()) {
								player.sendNotice(5, "Damage: " + totDamageToOneMonster);
							}
						}
						monster.damage(player, totDamageToOneMonster, true);
					}

					// effects
					switch (attack.skill) {
					case 4101005: // drain
					case 5111004: { // Energy Drain
						stats.setHp(stats.getHp()
							+ Math.min(monster.getMobMaxHp(), Math.min((int) ((double) totDamage
								* (double) theSkill.getEffect(player.getCurrentSkillLevel(theSkill)).getX() / 100.0), stats.getMaxHp() / 2)), true);
						break;
					}
					case 1311005: { // Sacrifice
						final int remainingHP = stats.getHp() - totDamage * effect.getX() / 100;
						stats.setHp(remainingHP > 1 ? (int) 1 : remainingHP);
						break;
					}
					case 5211006:
					case 22151002: // killer wing
					case 5220011: {// homing
						player.setLinkedMonsterId(monster.getObjectId());
						break;
					}
					case 4301001:
					case 4311002:
					case 4311003:
					case 4331000:
					case 4331004:
					case 4331005:
					case 4341005:
					case 4221007: // Boomerang Stab
					case 4221001: // Assasinate
					case 4211002: // Assulter
					case 4201005: // Savage Blow
					case 4001002: // Disorder
					case 4001334: // Double Stab
					case 4121007: // Triple Throw
					case 4111005: // Avenger
					case 4001344: { // Lucky Seven
						// Venom
						final ISkill skill = SkillInfoProvider.getSkill(4120005);
						final ISkill skill2 = SkillInfoProvider.getSkill(4220005);
						final ISkill skill3 = SkillInfoProvider.getSkill(4340001);
						if (player.getCurrentSkillLevel(skill) > 0) {
							final StatEffect venomEffect = skill.getEffect(player.getCurrentSkillLevel(skill));
							MonsterStatusEffect monsterStatusEffect;

							for (int i = 0; i < attackCount; i++) {
								if (venomEffect.makeChanceResult()) {
									if (monster.getVenomMulti() < 3) {
										monster.setVenomMulti((byte) (monster.getVenomMulti() + 1));
										monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), skill, null, false);
										monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
									}
								}
							}
						} else if (player.getCurrentSkillLevel(skill2) > 0) {
							final StatEffect venomEffect = skill2.getEffect(player.getCurrentSkillLevel(skill2));
							MonsterStatusEffect monsterStatusEffect;

							for (int i = 0; i < attackCount; i++) {
								if (venomEffect.makeChanceResult()) {
									if (monster.getVenomMulti() < 3) {
										monster.setVenomMulti((byte) (monster.getVenomMulti() + 1));
										monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), skill2, null, false);
										monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
									}
								}
							}
						} else if (player.getCurrentSkillLevel(skill3) > 0) {
							final StatEffect venomEffect = skill3.getEffect(player.getCurrentSkillLevel(skill3));
							MonsterStatusEffect monsterStatusEffect;

							for (int i = 0; i < attackCount; i++) {
								if (venomEffect.makeChanceResult()) {
									if (monster.getVenomMulti() < 3) {
										monster.setVenomMulti((byte) (monster.getVenomMulti() + 1));
										monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), skill3, null, false);
										monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
									}
								}
							}
						}
						break;
					}
					case 21000002: // Double attack
					case 21100001: // Triple Attack
					case 21100002: // Pole Arm Push
					case 21100004: // Pole Arm Smash
					case 21110002: // Full Swing
					case 21110003: // Pole Arm Toss
					case 21110004: // Fenrir Phantom
					case 21110006: // Whirlwind
					case 21110007: // (hidden) Full Swing - Double Attack
					case 21110008: // (hidden) Full Swing - Triple Attack
					case 21120002: // Overswing
					case 21120005: // Pole Arm finale
					case 21120006: // Tempest
					case 21120009: // (hidden) Overswing - Double Attack
					case 21120010: { // (hidden) Overswing - Triple Attack
						if (player.getBuffedValue(BuffStat.WK_CHARGE) != null) {
							final ISkill skill = SkillInfoProvider.getSkill(21111005);
							final StatEffect eff = skill.getEffect(player.getCurrentSkillLevel(skill));
							monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, eff.getX()), skill, null, false),
								false, eff.getY() * 1000, false);
						}
						if (player.getBuffedValue(BuffStat.BODY_PRESSURE) != null) {
							final ISkill skill = SkillInfoProvider.getSkill(21101003);
							final StatEffect eff = skill.getEffect(player.getCurrentSkillLevel(skill));

							if (eff.makeChanceResult()) {
								monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.NEUTRALISE, 1), skill, null, false),
									false, eff.getX() * 1000, false);
							}
						}
						if (player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
							final ISkill skill = SkillInfoProvider.getSkill(21100005);
							final ActivePlayerStats stat = player.getStats();
							stat.setHp(stat.getHp() + totDamage * skill.getEffect(player.getCurrentSkillLevel(skill)).getX() / 100, true);
						}
						break;
					}
					default: // passives attack bonuses
						if (totDamageToOneMonster > 0) {
							if (player.getBuffedValue(BuffStat.BLIND) != null) {
								final ISkill skill = SkillInfoProvider.getSkill(3221006);
								final StatEffect eff = skill.getEffect(player.getCurrentSkillLevel(skill));

								if (eff.makeChanceResult()) {
									final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, eff
										.getX()), skill, null, false);
									monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, false);
								}

							} else if (player.getBuffedValue(BuffStat.HAMSTRING) != null) {
								final ISkill skill = SkillInfoProvider.getSkill(3121007);
								final StatEffect eff = skill.getEffect(player.getCurrentSkillLevel(skill));

								if (eff.makeChanceResult()) {
									final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, eff
										.getX()), skill, null, false);
									monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, false);
								}
							} else if (player.getJobId() == 121) { // WHITEKNIGHT
								for (final int charge : charges) {
									final ISkill skill = SkillInfoProvider.getSkill(charge);
									if (player.isBuffFrom(BuffStat.WK_CHARGE, skill)) {
										final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE,
											1), skill, null, false);
										monster.applyStatus(player, monsterStatusEffect, false,
											skill.getEffect(player.getCurrentSkillLevel(skill)).getY() * 2000, false);
										break;
									}
								}
							}
						}
						break;
					}
					if (effect != null && effect.getMonsterStati().size() > 0) {
						if (effect.makeChanceResult()) {
							monster.applyStatus(player, new MonsterStatusEffect(effect.getMonsterStati(), theSkill, null, false), effect.isPoison(), effect
								.getDuration(), false);
						}
					}
				}
			}
		}
		if (attack.skill != 0 && (attack.targets > 0 || attack.skill != 4331003 && attack.skill != 4341002)) {
			effect.applyTo(player, attack.position);
		}
		if (totDamage > 1) {
			final CheatTracker tracker = player.getCheatTracker();

			tracker.setAttacksWithoutHit(true);
			final int offenseLimit;
			switch (attack.skill) {
			case 3121004:
			case 5221004:
				offenseLimit = 100;
				break;
			default:
				offenseLimit = 500;
				break;
			}
			if (tracker.getAttacksWithoutHit() > offenseLimit) {
				tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
			}
		}
	}

	public static void applyAttackMagic(final AttackInfo attack, final ISkill theSkill, final ChannelCharacter player, final StatEffect effect) {
		player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);

		if (effect == null) {
			player.getClient().write(ChannelPackets.enableActions());
			return;
		}
		//	if (attack.skill != 2301002) { // heal is both an attack and a special move (healing) so we'll let the whole applying magic live in the special move part
		//	    effect.applyTo(player);
		//	}
		if (attack.hits > effect.getAttackCount() || attack.targets > effect.getMobCount()) {
			player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
			return;
		}
		final ActivePlayerStats stats = player.getStats();
		//	double minDamagePerHit;
		double maxDamagePerHit;
		if (attack.skill == 2301002) {
			maxDamagePerHit = 30000;
		} else if (attack.skill == 1000 || attack.skill == 10001000 || attack.skill == 20001000 || attack.skill == 20011000) {
			maxDamagePerHit = 40;
		} else {
			// Minimum Damage = BA * (INT * 0.5 + (MATK*0.058)Â² + MATK * 3.3)
			// /100
			// Maximum Damage = BA * (INT * 0.5 + (MATK*0.058)Â² + (Mastery *
			// 0.9 * MATK) * 3.3) /100
			final double v75 = effect.getMatk() * 0.058;
			//	    minDamagePerHit = stats.getTotalMagic() * (stats.getInt() * 0.5 + (v75 * v75) + effect.getMatk() * 3.3) / 100;
			maxDamagePerHit = stats.getTotalMagic() * (stats.getInt() * 0.5 + v75 * v75 + effect.getMastery() * 0.9 * effect.getMatk() * 3.3) / 100;
		}
		maxDamagePerHit *= 1.04; // Avoid any errors for now

		final AttackNature element = player.getBuffedValue(BuffStat.ELEMENT_RESET) != null ? AttackNature.NEUTRAL : theSkill.getElement();

		double MaxDamagePerHit = 0;
		int totDamageToOneMonster, totDamage = 0, fixeddmg;
		byte overallAttackCount;
		boolean Tempest;
		MobInfo monsterstats;

		int CriticalDamage = stats.passive_sharpeye_percent();
		final Integer SharpEye = player.getBuffedSkill_Y(BuffStat.SHARP_EYES);
		if (SharpEye != null) {
			CriticalDamage += SharpEye - 100; // Additional damage in percentage
		}
		final Integer SharpEye_ = player.getBuffedSkill_Y(BuffStat.THORNS);
		if (SharpEye_ != null) {
			CriticalDamage += SharpEye_ - 100; // Additional damage in
												// percentage
		}
		final ISkill eaterSkill = SkillInfoProvider.getSkill(Skills.getMpEaterForJob(player.getJobId()));
		final int eaterLevel = player.getCurrentSkillLevel(eaterSkill);

		final GameMap map = player.getMap();

		for (final AttackPair oned : attack.allDamage) {
			final Monster monster = map.getMonsterByOid(oned.objectid);

			if (monster != null) {
				Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006;
				totDamageToOneMonster = 0;
				monsterstats = monster.getStats();
				fixeddmg = monsterstats.getFixedDamage();
				if (!Tempest) {
					if (!monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
						MaxDamagePerHit = CalculateMaxMagicDamagePerHit(player, theSkill, monster, monsterstats, stats, element, CriticalDamage,
							maxDamagePerHit);
					} else {
						maxDamagePerHit = 1;
					}
				}
				overallAttackCount = 0;

				for (Integer eachd : oned.attack) {
					overallAttackCount++;

					/*
					 * if (attack.skill == 2221006) { // Chain Lightning
					 * maxDamagePerMob *= (byte) 0.7 ^ (byte)
					 * (overallAttackCount - 1); maxDamagePerMob *= 3.333 *
					 * ((byte) (1 - 0.7) ^ (byte) (attack.targets)); }
					 */
					if (fixeddmg != -1) {
						eachd = monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg; // Magic
																					// is
																					// always
																					// not
																					// a
																					// normal
																					// attack
					} else {
						if (monsterstats.getOnlyNoramlAttack()) {
							eachd = 0; // Magic is always not a normal attack
						} else {
							//			    System.out.println("Client damage : " + eachd + " Server : " + MaxDamagePerHit);

							if (Tempest) { // Buffed with Tempest
								// In special case such as Chain lightning, the
								// damage will be reduced from the maxMP.
								if (eachd > monster.getMobMaxHp()) {
									eachd = monster.getMobMaxHp();
									player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC);
								}
							} else {
								if (eachd > MaxDamagePerHit) {
									//				    System.out.println("EXCEED!!! Client damage : " + eachd + " Server : " + MaxDamagePerHit);
									eachd = (int) MaxDamagePerHit; // Convert to
																	// server
																	// calculated
																	// damage
									player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC);
								}
							}
						}
					}
					totDamageToOneMonster += eachd;
				}
				totDamage += totDamageToOneMonster;
				player.checkMonsterAggro(monster);

				if (player.getPosition().distanceSq(monster.getPosition()) > 400000.0) { // 600^2,
																							// 550
																							// is
																							// approximatly
																							// the
																							// range
																							// of
																							// ultis
					player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER);
				}
				if (attack.skill == 2301002 && !monsterstats.getUndead()) {
					player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
					return;
				}

				if (totDamageToOneMonster > 0) {
					if (totDamageToOneMonster >= 199999) {
						// HACK
						// Damage Formula
						totDamageToOneMonster = Math.min(ChannelCharacter.damageCap, Math.max(totDamageToOneMonster, totDamageToOneMonster
							* (player.getStats().getTotalMagic() / 50)));
						if (player.isOnDMG()) {
							player.sendNotice(5, "Damage: " + totDamageToOneMonster);
						}
					}
					monster.damage(player, totDamageToOneMonster, true);
					// effects
					switch (attack.skill) {
					case 2221003:
						monster.setTempEffectiveness(AttackNature.FIRE, theSkill.getEffect(player.getCurrentSkillLevel(theSkill)).getDuration());
						break;
					case 2121003:
						monster.setTempEffectiveness(AttackNature.ICE, theSkill.getEffect(player.getCurrentSkillLevel(theSkill)).getDuration());
						break;
					}
					if (effect != null && effect.getMonsterStati().size() > 0) {
						if (effect.makeChanceResult()) {
							monster.applyStatus(player, new MonsterStatusEffect(effect.getMonsterStati(), theSkill, null, false), effect.isPoison(), effect
								.getDuration(), false);
						}
					}
					if (eaterLevel > 0) {
						eaterSkill.getEffect(eaterLevel).applyPassive(player, monster);
					}
				}
			}
		}
		if (attack.skill != 2301002) {
			effect.applyTo(player);
		}

		if (totDamage > 1) {
			final CheatTracker tracker = player.getCheatTracker();
			tracker.setAttacksWithoutHit(true);

			if (tracker.getAttacksWithoutHit() > 500) {
				tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
			}
		}
	}

	private static double CalculateMaxMagicDamagePerHit(final ChannelCharacter chr, final ISkill skill, final Monster monster, final MobInfo mobstats,
		final ActivePlayerStats stats, final AttackNature elem, final Integer sharpEye, final double maxDamagePerMonster) {
		final int dLevel = Math.max(mobstats.getLevel() - chr.getLevel(), 0);
		final int Accuracy = stats.getTotalInt() / 10 + stats.getTotalLuk() / 10;
		final int MinAccuracy = mobstats.getEvasion() * (dLevel * 2 + 51) / 120;
		// FullAccuracy = Avoid * (dLevel * 2 + 51) / 50

		// miss :P or HACK :O
		if (MinAccuracy > Accuracy && skill.getId() != 1000 && skill.getId() != 10001000 && skill.getId() != 20001000 && skill.getId() != 20011000) {
			return 0;
		}
		double elemMaxDamagePerMob;

		switch (monster.getEffectiveness(elem)) {
		case IMMUNE:
			elemMaxDamagePerMob = 1;
			break;
		case NORMAL:
			elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster / 100 * stats.element_amp_percent, stats);
			break;
		case WEAK:
			elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * 1.5 / 100 * stats.element_amp_percent, stats);
			break;
		case STRONG:
			elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * 0.5 / 100 * stats.element_amp_percent, stats);
			break;
		default:
			throw new RuntimeException("Unknown enum constant");
		}
		// Calculate monster magic def
		// Min damage = (MIN before defense) - MDEF*.6
		// Max damage = (MAX before defense) - MDEF*.5
		elemMaxDamagePerMob -= mobstats.getMagicDefense() * 0.5;

		// Calculate Sharp eye bonus
		if (sharpEye != null) {
			elemMaxDamagePerMob += elemMaxDamagePerMob / 100 * sharpEye;
		}

		//	if (skill.isChargeSkill()) {
		//	    elemMaxDamagePerMob = (float) ((90 * ((System.currentTimeMillis() - chr.getKeyDownSkill_Time()) / 1000) + 10) * elemMaxDamagePerMob * 0.01);
		//	}
		if (skill.isChargeSkill() && chr.getKeyDownSkill_Time() == 0) {
			return 0;
		}
		switch (skill.getId()) {
		case 1000:
		case 10001000:
		case 20001000:
		case 20011000:
			elemMaxDamagePerMob = 40;
			break;
		}
		if (elemMaxDamagePerMob > ChannelCharacter.getDamageCap()) {
			elemMaxDamagePerMob = ChannelCharacter.getDamageCap();
		} else if (elemMaxDamagePerMob < 0) {
			elemMaxDamagePerMob = 1;
		}
		return elemMaxDamagePerMob;
	}

	private static double ElementalStaffAttackBonus(final AttackNature elem, final double elemMaxDamagePerMob, final ActivePlayerStats stats) {
		switch (elem) {
		case FIRE:
			return elemMaxDamagePerMob / 100 * stats.element_fire;
		case ICE:
			return elemMaxDamagePerMob / 100 * stats.element_ice;
		case LIGHTING:
			return elemMaxDamagePerMob / 100 * stats.element_light;
		case POISON:
			return elemMaxDamagePerMob / 100 * stats.element_psn;
		default:
			return elemMaxDamagePerMob / 100 * stats.def;
		}
	}

	private static void handlePickPocket(final ChannelCharacter player, final Monster mob, final AttackPair oned) {
		final int maxmeso = player.getBuffedValue(BuffStat.PICKPOCKET).intValue();
		final ISkill skill = SkillInfoProvider.getSkill(4211003);
		final StatEffect s = skill.getEffect(player.getCurrentSkillLevel(skill));

		for (final Integer eachd : oned.attack) {
			if (s.makeChanceResult()) {

				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						final int amount = Math.min((int) Math.max((double) eachd / (double) 20000 * maxmeso, 1), maxmeso);
						final int x = (int) (mob.getPosition().getX() + Randomizer.nextInt(100) - 50);
						final int y = (int) mob.getPosition().getY();
						final Point position = new Point(x, y);
						player.getMap().spawnMesoDrop(amount, position, mob, player, true, ItemDropType.DEFAULT);
					}
				}, 100);
			}
		}
	}

	private static double CalculateMaxWeaponDamagePerHit(final ChannelCharacter player, final Monster monster, final AttackInfo attack, final ISkill theSkill,
		final StatEffect attackEffect, double maximumDamageToMonster, final Integer CriticalDamagePercent) {
		if (player.getMapId() / 1000000 == 914) { // aran
			return 199999;
		}
		AttackNature element = AttackNature.NEUTRAL;
		if (theSkill != null) {
			element = theSkill.getElement();

			switch (theSkill.getId()) {
			case 1000:
			case 10001000:
			case 20001000:
			case 20011000:
				maximumDamageToMonster = 40;
				break;
			case 3221007: // Sniping
				maximumDamageToMonster = ChannelCharacter.getDamageCap();
				break;
			case 4211006: // Meso Explosion
				maximumDamageToMonster = ChannelCharacter.getDamageCap();
				break;
			/*
			 * case 4221001: // Assasinate maximumDamageToMonster = 400000;
			 * break;
			 */
			case 1009: // Bamboo Trust
			case 10001009:
			case 20001009:
			case 20011009:
				maximumDamageToMonster = monster.getStats().isBoss() ? monster.getMobMaxHp() / 30 * 100 : monster.getMobMaxHp();
				break;
			case 3211006: // Sniper Strafe
				if (monster.getStatusSourceID(MonsterStatus.FREEZE) == 3211003) { // blizzard
																					// in
																					// effect
					maximumDamageToMonster = monster.getHp();
				}
				break;
			}
		}
		if (player.getBuffedValue(BuffStat.WK_CHARGE) != null) {
			final int chargeSkillId = player.getBuffSource(BuffStat.WK_CHARGE);

			switch (chargeSkillId) {
			case 1211003:
			case 1211004:
				element = AttackNature.FIRE;
				break;
			case 1211005:
			case 1211006:
			case 21111005:
				element = AttackNature.ICE;
				break;
			case 1211007:
			case 1211008:
			case 15101006:
				element = AttackNature.LIGHTING;
				break;
			case 1221003:
			case 1221004:
			case 11111007:
				element = AttackNature.HOLY;
				break;
			case 12101005:
				element = AttackNature.NEUTRAL;
				break;
			default:
				throw new RuntimeException("Unknown enum constant");
			}
			final ISkill skill = SkillInfoProvider.getSkill(chargeSkillId);
			maximumDamageToMonster *= skill.getEffect(player.getCurrentSkillLevel(skill)).getDamage() / 100.0;
		}
		double elementalMaxDamagePerMonster;
		if (element != AttackNature.NEUTRAL) {
			double elementalEffect;

			switch (attack.skill) {
			case 3211003:
			case 3111003: // inferno and blizzard
				elementalEffect = attackEffect.getX() / 200.0;
				break;
			default:
				elementalEffect = 0.5;
				break;
			}
			switch (monster.getEffectiveness(element)) {
			case IMMUNE:
				elementalMaxDamagePerMonster = 1;
				break;
			case NORMAL:
				elementalMaxDamagePerMonster = maximumDamageToMonster;
				break;
			case WEAK:
				elementalMaxDamagePerMonster = maximumDamageToMonster * (1.0 + elementalEffect);
				break;
			case STRONG:
				elementalMaxDamagePerMonster = maximumDamageToMonster * (1.0 - elementalEffect);
				break;
			default:
				throw new RuntimeException("Unknown enum constant");
			}
		} else {
			elementalMaxDamagePerMonster = maximumDamageToMonster;
		}
		// Calculate mob def
		final short moblevel = monster.getStats().getLevel();
		final short d = moblevel > player.getLevel() ? (short) (moblevel - player.getLevel()) : 0;
		elementalMaxDamagePerMonster = elementalMaxDamagePerMonster * (1 - 0.01 * d) - monster.getStats().getPhysicalDefense() * 0.5;

		// Calculate passive bonuses + Sharp Eye
		elementalMaxDamagePerMonster += elementalMaxDamagePerMonster / 100 * CriticalDamagePercent;

		//	if (theSkill.isChargeSkill()) {
		//	    elementalMaxDamagePerMonster = (double) (90 * (System.currentTimeMillis() - player.getKeyDownSkill_Time()) / 2000 + 10) * elementalMaxDamagePerMonster * 0.01;
		//	}
		if (theSkill != null && theSkill.isChargeSkill() && player.getKeyDownSkill_Time() == 0) {
			return 0;
		}

		if (elementalMaxDamagePerMonster > ChannelCharacter.getDamageCap()) {
			elementalMaxDamagePerMonster = ChannelCharacter.getDamageCap();
		} else if (elementalMaxDamagePerMonster < 0) {
			elementalMaxDamagePerMonster = 1;
		}
		return elementalMaxDamagePerMonster;
	}

	public static AttackInfo parseDmgMa(final PacketReader lea) throws PacketFormatException {
		final AttackInfo ret = new AttackInfo();

		lea.skip(1);
		final boolean unkk = lea.readByte() == -1;
		lea.skip(unkk ? 7 : 8);
		ret.tbyte = lea.readByte();
		ret.targets = (byte) (ret.tbyte >>> 4 & 0xF);
		ret.hits = (byte) (ret.tbyte & 0xF);
		lea.skip(8); // ?
		ret.skill = lea.readInt();

		lea.skip(16); // ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on
						// v.82

		switch (ret.skill) {
		case 2121001: // Big Bang
		case 2221001:
		case 2321001:
		case 22121000: // breath
		case 22151001:
			ret.charge = lea.readInt();
			break;
		default:
			ret.charge = -1;
			break;
		}

		lea.skip(1);
		ret.display = lea.readByte(); // Always zero?
		ret.animation = lea.readByte();
		ret.speed = lea.readByte(); // Confirmed
		lea.skip(1); // Weapon subclass
		ret.lastAttackTickCount = lea.readInt(); // Ticks
		lea.skip(4); // 0

		int oid, damage;
		List<Integer> allDamageNumbers;
		ret.allDamage = Lists.newArrayList();

		for (int i = 0; i < ret.targets; i++) {
			oid = lea.readInt();
			lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2]
							// seems to change randomly for some attack

			allDamageNumbers = Lists.newArrayList();

			for (int j = 0; j < ret.hits; j++) {
				damage = lea.readInt();
				allDamageNumbers.add(Integer.valueOf(damage));
			}
			lea.skip(4); // CRC of monster [Wz Editing]
			ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
		}

		return ret;
	}

	public static AttackInfo parseDmgM(final PacketReader lea) throws PacketFormatException {
		final AttackInfo ret = new AttackInfo();

		lea.skip(1);
		final boolean unkk = lea.readByte() == -1;
		lea.skip(unkk ? 7 : 8);
		ret.tbyte = lea.readByte();
		ret.targets = (byte) (ret.tbyte >>> 4 & 0xF);
		ret.hits = (byte) (ret.tbyte & 0xF);
		lea.skip(8);
		ret.skill = lea.readInt();

		// ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82
		lea.skip(16);

		switch (ret.skill) {
		case 4341002: // Final cut
		case 5101004: // Corkscrew
		case 5201002: // Gernard
		case 14111006: // Poison bomb
		case 15101003: // Cygnus corkscrew
			ret.charge = lea.readInt();
			break;
		default:
			ret.charge = 0;
			break;
		}
		// ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82
		lea.skip(1);
		ret.display = lea.readByte(); // Always zero?
		ret.animation = lea.readByte();
		lea.skip(1); // Weapon class
		ret.speed = lea.readByte(); // Confirmed
		ret.lastAttackTickCount = lea.readInt(); // Ticks
		lea.skip(4);

		ret.allDamage = Lists.newArrayList();

		if (ret.skill == 4211006) {
			// Meso Explosion
			return parseMesoExplosion(lea, ret);
		}
		int oid, damage;
		List<Integer> allDamageNumbers;

		for (int i = 0; i < ret.targets; i++) {
			oid = lea.readInt();
			//	    	System.out.println(tools.HexTool.toString(lea.readBytes(14)));
			// [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change
			// randomly for some attack
			lea.skip(14);

			allDamageNumbers = Lists.newArrayList();

			for (int j = 0; j < ret.hits; j++) {
				damage = lea.readInt();
				//				System.out.println("Damage: " + damage);
				allDamageNumbers.add(Integer.valueOf(damage));
			}
			// CRC of monster [Wz Editing]
			lea.skip(4);
			ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
		}
		ret.position = lea.readVector();
		return ret;
	}

	public static AttackInfo parseDmgR(final PacketReader lea) throws PacketFormatException {
		final AttackInfo ret = new AttackInfo();

		lea.skip(1);
		final boolean unkk = lea.readByte() == -1;
		lea.skip(unkk ? 7 : 8);
		ret.tbyte = lea.readByte();
		ret.targets = (byte) (ret.tbyte >>> 4 & 0xF);
		ret.hits = (byte) (ret.tbyte & 0xF);
		lea.skip(8);
		ret.skill = lea.readInt();
		// ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82
		lea.skip(16);

		switch (ret.skill) {
		case 3121004: // Hurricane
		case 3221001: // Pierce
		case 5221004: // Rapidfire
		case 13111002: // Cygnus Hurricane
			lea.skip(4); // extra 4 bytes
			break;
		}
		// ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82
		lea.skip(1);

		ret.display = lea.readByte(); // Always zero?
		ret.animation = lea.readByte();
		lea.skip(1); // Weapon class
		ret.speed = lea.readByte(); // Confirmed
		ret.lastAttackTickCount = lea.readInt(); // Ticks
		lea.skip(4); // 0
		ret.slot = (byte) lea.readShort();
		ret.csstar = (byte) lea.readShort();
		ret.AOE = lea.readByte(); // is AOE or not, TT/ Avenger = 41, Showdown =
									// 0

		int damage, oid;
		List<Integer> allDamageNumbers;
		ret.allDamage = Lists.newArrayList();

		for (int i = 0; i < ret.targets; i++) {
			oid = lea.readInt();
			//	    	System.out.println(tools.HexTool.toString(lea.readBytes(14)));
			// [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change
			// randomly for some attack
			lea.skip(14);
			allDamageNumbers = Lists.newArrayList();
			for (int j = 0; j < ret.hits; j++) {
				damage = lea.readInt();
				allDamageNumbers.add(Integer.valueOf(damage));
			}
			// CRC of monster [Wz Editing]
			lea.skip(4);
			//	    	System.out.println(tools.HexTool.toString(lea.readBytes(4)));

			ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
		}
		lea.skip(4);
		ret.position = lea.readVector();

		return ret;
	}

	public static AttackInfo parseMesoExplosion(final PacketReader lea, final AttackInfo ret) throws PacketFormatException {
		byte bullets;
		if (ret.hits == 0) {
			lea.skip(4);
			bullets = lea.readByte();
			for (int j = 0; j < bullets; j++) {
				ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
				lea.skip(1);
			}
			lea.skip(2); // 8F 02
			return ret;
		}

		int oid;
		List<Integer> allDamageNumbers;

		for (int i = 0; i < ret.targets; i++) {
			oid = lea.readInt();
			lea.skip(12);
			bullets = lea.readByte();
			allDamageNumbers = Lists.newArrayList();
			for (int j = 0; j < bullets; j++) {
				allDamageNumbers.add(Integer.valueOf(lea.readInt()));
			}
			ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
			lea.skip(4); // C3 8F 41 94, 51 04 5B 01
		}
		lea.skip(4);
		bullets = lea.readByte();

		for (int j = 0; j < bullets; j++) {
			ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
			lea.skip(1);
		}
		lea.skip(2); // 8F 02/ 63 02

		return ret;
	}
}
