package javastory.channel.client;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.client.IEquip;
import javastory.client.IItem;
import javastory.client.PlayerStats;
import javastory.game.GameConstants;
import javastory.game.Jobs;
import javastory.game.Skills;
import javastory.game.WeaponType;
import javastory.server.ItemInfoProvider;

public class ActivePlayerStats extends PlayerStats implements Serializable {

	private static final long serialVersionUID = -679541993413738569L;

	private transient WeakReference<ChannelCharacter> character;
	private transient float shouldHealHP, shouldHealMP;
	private transient short passive_sharpeye_percent, passive_sharpeye_rate;
	private transient int localmaxhp, localmaxmp, localSTR, localDEX, localLUK,
			localint_;
	private transient int magic, watk, hands, accuracy;
	private transient float speedMod, jumpMod, localmaxbasedamage;
	// Elemental properties
	public transient int element_amp_percent;
	public transient int def, element_ice, element_fire, element_light,
			element_psn;

	public ActivePlayerStats(final ChannelCharacter chr) {
		// TODO, move str/dex/int etc here -_-
		this.character = new WeakReference<>(chr);
	}

	public final void init() {
		relocHeal();
		recalcLocalStats();
	}

	public final void setStr(final int str) {
		this.STR = str;
		recalcLocalStats();
	}

	public final void setDex(final int dex) {
		this.DEX = dex;
		recalcLocalStats();
	}

	public final void setLuk(final int luk) {
		this.LUK = luk;
		recalcLocalStats();
	}

	public final void setInt(final int int_) {
		this.INT = int_;
		recalcLocalStats();
	}

	public final boolean setHp(final int newhp) {
		return setHp(newhp, false);
	}

	public final boolean setHp(int newhp, boolean silent) {
		final int oldHp = HP;
		int thp = newhp;
		if (thp < 0) {
			thp = 0;
		}
		if (thp > localmaxhp) {
			thp = localmaxhp;
		}
		this.HP = thp;

		final ChannelCharacter chra = character.get();
		if (chra != null) {
			if (!silent) {
				chra.updatePartyMemberHP();
			}
			if (oldHp > HP && !chra.isAlive()) {
				chra.playerDead();
			}
		}
		return HP != oldHp;
	}

	public final boolean setMp(final int newmp) {
		final int oldMp = MP;
		int tmp = newmp;
		if (tmp < 0) {
			tmp = 0;
		}
		if (tmp > localmaxmp) {
			tmp = localmaxmp;
		}
		this.MP = tmp;
		return MP != oldMp;
	}

	public final void setMaxHp(final int hp) {
		this.MaxHP = hp;
		recalcLocalStats();
	}

	public final void setMaxMp(final int mp) {
		this.MaxMP = mp;
		recalcLocalStats();
	}

	public final int getTotalDex() {
		return localDEX;
	}

	public final int getTotalInt() {
		return localint_;
	}

	public final int getTotalStr() {
		return localSTR;
	}

	public final int getTotalLuk() {
		return localLUK;
	}

	public final int getTotalMagic() {
		return magic;
	}

	public final double getSpeedMod() {
		return speedMod;
	}

	public final double getJumpMod() {
		return jumpMod;
	}

	public final int getTotalWatk() {
		return watk;
	}

	public final int getCurrentMaxHp() {
		return localmaxhp;
	}

	public final int getCurrentMaxMp() {
		return localmaxmp;
	}

	public final int getHands() {
		return hands;
	}

	public final float getCurrentMaxBaseDamage() {
		return localmaxbasedamage;
	}

	public void recalcLocalStats() {
		final ChannelCharacter chra = character.get();
		if (chra == null) {
			return;
		}
		int oldmaxhp = localmaxhp;
		localmaxhp = getMaxHp();
		localmaxmp = getMaxMp();
		localDEX = getDex();
		localint_ = getInt();
		localSTR = getStr();
		localLUK = getLuk();
		int speed = 100;
		int jump = 100;
		magic = localint_;
		watk = 0;
		for (IItem item : chra.getEquippedItemsInventory()) {
			final IEquip equip = (IEquip) item;

			if (equip.getPosition() == -11) {
				if (GameConstants.isMagicWeapon(equip.getItemId())) {
					final Map<String, Integer> eqstat = ItemInfoProvider
							.getInstance().getEquipStats(equip.getItemId());

					element_fire = eqstat.get("incRMAF");
					element_ice = eqstat.get("incRMAI");
					element_light = eqstat.get("incRMAL");
					element_psn = eqstat.get("incRMAS");
					def = eqstat.get("elemDefault");
				} else {
					element_fire = 100;
					element_ice = 100;
					element_light = 100;
					element_psn = 100;
					def = 100;
				}
			}
			accuracy += equip.getAcc();
			localmaxhp += equip.getHp();
			localmaxmp += equip.getMp();
			localDEX += equip.getDex();
			localint_ += equip.getInt();
			localSTR += equip.getStr();
			localLUK += equip.getLuk();
			magic += equip.getMatk() + equip.getInt();
			watk += equip.getWatk();
			speed += equip.getSpeed();
			jump += equip.getJump();
		}
		Integer buff = chra.getBuffedValue(BuffStat.MAPLE_WARRIOR);
		if (buff != null) {
			final double d = buff.doubleValue() / 100;
			localSTR += d * localSTR;
			localDEX += d * localDEX;
			localLUK += d * localLUK;

			final int before = localint_;
			localint_ += d * localint_;
			magic += localint_ - before;
		}
		buff = chra.getBuffedValue(BuffStat.ECHO_OF_HERO);
		if (buff != null) {
			final double d = buff.doubleValue() / 100;
			watk += watk / 100 * d;
			magic += magic / 100 * d;
		}
		buff = chra.getBuffedValue(BuffStat.ARAN_COMBO);
		if (buff != null) {
			watk += buff / 10;
		}
		buff = chra.getBuffedValue(BuffStat.MAXHP);
		if (buff != null) {
			localmaxhp += (buff.doubleValue() / 100) * localmaxhp;
		}
		buff = chra.getBuffedValue(BuffStat.MAXMP);
		if (buff != null) {
			localmaxmp += (buff.doubleValue() / 100) * localmaxmp;
		}
		element_amp_percent = 100;

		switch (chra.getJobId()) {
		case 322: { // Crossbowman
			final ISkill expert = SkillFactory.getSkill(3220004);
			final int boostLevel = chra.getCurrentSkillLevel(expert);
			if (boostLevel > 0) {
				watk += expert.getEffect(boostLevel).getX();
			}
			break;
		}
		case 312: { // Bowmaster
			final ISkill expert = SkillFactory.getSkill(3120005);
			final int boostLevel = chra.getCurrentSkillLevel(expert);
			if (boostLevel > 0) {
				watk += expert.getEffect(boostLevel).getX();
			}
			break;
		}
		case 211:
		case 212: { // IL
			final ISkill amp = SkillFactory.getSkill(2110001);
			final int level = chra.getCurrentSkillLevel(amp);
			if (level > 0) {
				element_amp_percent = amp.getEffect(level).getY();
			}
			break;
		}
		case 221:
		case 222: { // IL
			final ISkill amp = SkillFactory.getSkill(2210001);
			final int level = chra.getCurrentSkillLevel(amp);
			if (level > 0) {
				element_amp_percent = amp.getEffect(level).getY();
			}
			break;
		}
		case 1211:
		case 1212: { // flame
			final ISkill amp = SkillFactory.getSkill(12110001);
			final int level = chra.getCurrentSkillLevel(amp);
			if (level > 0) {
				element_amp_percent = amp.getEffect(level).getY();
			}
			break;
		}
		case 2215:
		case 2216:
		case 2217:
		case 2218: {
			final ISkill amp = SkillFactory.getSkill(22150000);
			final int level = chra.getCurrentSkillLevel(amp);
			if (level > 0) {
				element_amp_percent = amp.getEffect(level).getY();
			}
			break;
		}
		case 2112: { // Aran
			final ISkill expert = SkillFactory.getSkill(21120001);
			final int boostLevel = chra.getCurrentSkillLevel(expert);
			if (boostLevel > 0) {
				watk += expert.getEffect(boostLevel).getX();
			}
			break;
		}
		}
		final ISkill blessoffairy = SkillFactory.getSkill(Skills
				.getBlessOfFairyForJob(chra.getJobId()));
		final int boflevel = chra.getCurrentSkillLevel(blessoffairy);
		if (boflevel > 0) {
			watk += blessoffairy.getEffect(boflevel).getX();
			magic += blessoffairy.getEffect(boflevel).getY();
		}

		// switch (chra.getJob() / 100) {
		// case 1:
		// break;
		// }

		buff = chra.getBuffedValue(BuffStat.ACC);
		if (buff != null) {
			accuracy += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.WATK);
		if (buff != null) {
			watk += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.MATK);
		if (buff != null) {
			magic += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.SPEED);
		if (buff != null) {
			speed += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.JUMP);
		if (buff != null) {
			jump += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.DASH_SPEED);
		if (buff != null) {
			speed += buff.intValue();
		}
		buff = chra.getBuffedValue(BuffStat.DASH_JUMP);
		if (buff != null) {
			jump += buff.intValue();
		}
		if (speed > 140) {
			speed = 140;
		}
		if (jump > 123) {
			jump = 123;
		}
		speedMod = speed / 100.0f;
		jumpMod = jump / 100.0f;
		Integer mount = chra.getBuffedValue(BuffStat.MONSTER_RIDING);
		if (mount != null) {
			jumpMod = 1.23f;
			switch (mount.intValue()) {
			case 1:
				speedMod = 1.5f;
				break;
			case 2:
				speedMod = 1.7f;
				break;
			case 3:
				speedMod = 1.8f;
				break;
			default:
				System.err.println(
						"Unhandeled monster riding level, Speedmod = "
								+ speedMod + "");
			}
		}
		hands = this.localDEX + this.localint_ + this.localLUK;

		magic = Math.min(magic, ChannelCharacter.magicCap);
		localmaxhp = Math.min(30000, localmaxhp);
		localmaxmp = Math.min(30000, localmaxmp);

		CalcPassive_SharpEye(chra);

		localmaxbasedamage = calculateMaxBaseDamage(watk);
		if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
			chra.updatePartyMemberHP();
		}
	}

	private void CalcPassive_SharpEye(final ChannelCharacter player) {
		switch (player.getJobId()) { // Apply passive Critical bonus
		case 410:
		case 411:
		case 412: { // Assasin/ Hermit / NL
			final ISkill critSkill = SkillFactory.getSkill(4100001);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 1410:
		case 1411:
		case 1412: { // Night Walker
			final ISkill critSkill = SkillFactory.getSkill(14100001);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 511:
		case 512: { // Buccaner, Viper
			final ISkill critSkill = SkillFactory.getSkill(5110000);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 1511:
		case 1512: {
			final ISkill critSkill = SkillFactory.getSkill(15110000);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 2111:
		case 2112: {
			// Aran, TODO : only applies when there's > 10 combo
			final ISkill critSkill = SkillFactory.getSkill(21110000);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(critSkill.getEffect(critlevel).getDamage());
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 300:
		case 310:
		case 311:
		case 312:
		case 320:
		case 321:
		case 322: { // Bowman
			final ISkill critSkill = SkillFactory.getSkill(3000001);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 1300:
		case 1310:
		case 1311:
		case 1312: { // Bowman
			final ISkill critSkill = SkillFactory.getSkill(13000000);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		case 2214:
		case 2215:
		case 2216:
		case 2217:
		case 2218: { // Evan
			final ISkill critSkill = SkillFactory.getSkill(22140000);
			final int critlevel = player.getCurrentSkillLevel(critSkill);
			if (critlevel > 0) {
				this.passive_sharpeye_percent =
						(short) (critSkill.getEffect(critlevel).getDamage() - 100);
				this.passive_sharpeye_rate =
						critSkill.getEffect(critlevel).getProb();
				return;
			}
			break;
		}
		}
		this.passive_sharpeye_percent = 0;
		this.passive_sharpeye_rate = 0;
	}

	public final short passive_sharpeye_percent() {
		return passive_sharpeye_percent;
	}

	public final short passive_sharpeye_rate() {
		return passive_sharpeye_rate;
	}

	public final float calculateMaxBaseDamage(final int watk) {
		final ChannelCharacter chra = character.get();
		if (chra == null) {
			return 0;
		}
		float maxBaseDamage;
		if (watk == 0) {
			maxBaseDamage = 1;
		} else {
			final IItem weapon_item =
					chra.getEquippedItemsInventory().getItem((byte) -11);

			if (weapon_item != null) {
				final int job = chra.getJobId();
				final WeaponType weapon =
						GameConstants.getWeaponType(weapon_item.getItemId());
				int primary, secondary;

				switch (weapon) {
				case BOW:
				case CROSSBOW:
					primary = localDEX;
					secondary = localSTR;
					break;
				case CLAW:
				case DAGGER:
					if ((job >= 400 && job <= 422)
							|| (job >= 1400 && job <= 1412)) {
						primary = localLUK;
						secondary = localDEX + localSTR;
					} else { // Non Thieves
						primary = localSTR;
						secondary = localDEX;
					}
					break;
				case KNUCKLE:
					primary = localSTR;
					secondary = localDEX;
					break;
				case GUN:
					primary = localDEX;
					secondary = localSTR;
					break;
				case NOT_A_WEAPON:
					if ((job >= 500 && job <= 522)
							|| (job >= 1500 && job <= 1512)) {
						primary = localSTR;
						secondary = localDEX;
					} else {
						primary = 0;
						secondary = 0;
					}
					break;
				default:
					primary = localSTR;
					secondary = localDEX;
					break;
				}
				maxBaseDamage = ((weapon.getMaxDamageMultiplier() * primary) + secondary)
						* watk / 100;
			} else {
				maxBaseDamage = 0;
			}
		}
		return maxBaseDamage;
	}

	public final float getHealHP() {
		return shouldHealHP;
	}

	public final float getHealMP() {
		return shouldHealMP;
	}

	public final void relocHeal() {
		final ChannelCharacter chra = character.get();
		if (chra == null) {
			return;
		}
		final int jobId = chra.getJobId();

		shouldHealHP = 10; // Reset
		shouldHealMP = 3;

		if (Jobs.isJobFamily(200, jobId)) {
			// Improving MP recovery
			shouldHealMP += (((float) chra
					.getCurrentSkillLevel(SkillFactory.getSkill(2000000)) / 10) * chra
					.getLevel());
		} else if (Jobs.isJobFamily(111, jobId)) {
			final ISkill effect = SkillFactory.getSkill(1110000);
			// Improving MP Recovery
			final int lvl = chra.getCurrentSkillLevel(effect);
			if (lvl > 0) {
				shouldHealMP += effect.getEffect(lvl).getMp();
			}

		} else if (Jobs.isJobFamily(121, jobId)) {
			// Improving MP Recovery
			final ISkill effect = SkillFactory.getSkill(1210000);
			final int lvl = chra.getCurrentSkillLevel(effect);
			if (lvl > 0) {
				shouldHealMP += effect.getEffect(lvl).getMp();
			}

		} else if (Jobs.isJobFamily(1111, jobId)) {
			// Improving MP Recovery
			final ISkill effect = SkillFactory.getSkill(11110000);
			final int lvl = chra.getCurrentSkillLevel(effect);
			if (lvl > 0) {
				shouldHealMP += effect.getEffect(lvl).getMp();
			}

		} else if (Jobs.isJobFamily(410, jobId)) {
			// Endure
			final ISkill effect = SkillFactory.getSkill(4100002);
			final int lvl = chra.getCurrentSkillLevel(effect);
			if (lvl > 0) {
				shouldHealHP += effect.getEffect(lvl).getHp();
				shouldHealMP += effect.getEffect(lvl).getMp();
			}

		} else if (Jobs.isJobFamily(420, jobId)) {
			// Endure
			final ISkill effect = SkillFactory.getSkill(4200001);
			final int lvl = chra.getCurrentSkillLevel(effect);
			if (lvl > 0) {
				shouldHealHP += effect.getEffect(lvl).getHp();
				shouldHealMP += effect.getEffect(lvl).getMp();
			}
		}
		if (chra.isGM()) {
			shouldHealHP += 1000;
			shouldHealMP += 1000;
		}
		if (chra.getChair() != 0) {
			// Is sitting on a chair.
			// Until the values of Chair heal has been fixed,
			// MP is different here, if chair data MP = 0, heal + 1.5
			shouldHealHP += 99;
			shouldHealMP += 99;
		} else {
			// Because Heal isn't multipled when there's a chair :)
			final float recvRate = chra.getMap().getRecoveryRate();
			shouldHealHP *= recvRate;
			shouldHealMP *= recvRate;
		}
		// To avoid any problem with bathrobe / Sauna >.<
		// 1.5
		shouldHealHP *= 2;
		shouldHealMP *= 2;
	}
}