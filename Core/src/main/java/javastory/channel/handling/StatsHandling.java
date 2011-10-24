package javastory.channel.handling;

import java.util.ArrayList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.ISkill;
import javastory.channel.client.SkillFactory;
import javastory.channel.server.AutobanManager;
import javastory.game.Skills;
import javastory.game.Stat;
import javastory.game.StatValue;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public class StatsHandling {

	public static void handleDistributeAbilityPoints(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final List<StatValue> statupdate = new ArrayList<>(2);
		c.write(ChannelPackets.updatePlayerStats(statupdate, true, chr.getJobId()));
		reader.skip(4);

		final ActivePlayerStats stat = chr.getStats();

		if (chr.getRemainingAp() > 0) {
			final ChannelCharacter player = c.getPlayer();
			switch (reader.readInt()) {
			case 64: // Str
				if (stat.getStr() >= player.getMaxStats()) {
					return;
				}
				stat.setStr(stat.getStr() + 1);
				statupdate.add(new StatValue(Stat.STR, stat.getStr()));
				break;
			case 128: // Dex
				if (stat.getDex() >= player.getMaxStats()) {
					return;
				}
				stat.setDex(stat.getDex() + 1);
				statupdate.add(new StatValue(Stat.DEX, stat.getDex()));
				break;
			case 256: // Int
				if (stat.getInt() >= player.getMaxStats()) {
					return;
				}
				stat.setInt(stat.getInt() + 1);
				statupdate.add(new StatValue(Stat.INT, stat.getInt()));
				break;
			case 512: // Luk
				if (stat.getLuk() >= player.getMaxStats()) {
					return;
				}
				stat.setLuk(stat.getLuk() + 1);
				statupdate.add(new StatValue(Stat.LUK, stat.getLuk()));
				break;
			case 2048: // HP
				int MaxHP = stat.getMaxHp();
				if (chr.getHpApUsed() >= 10000 || MaxHP >= 30000) {
					return;
				}
				ISkill improvingMaxHP = null;
				int improvingMaxHPLevel = 0;
				if (chr.getJobId() == 0) { // Beginner
					MaxHP += Randomizer.rand(8, 12);
				} else if (chr.getJobId() >= 100 && chr.getJobId() <= 132) { // Warrior
					improvingMaxHP = SkillFactory.getSkill(1000001);
					improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
					MaxHP += Randomizer.rand(20, 24);
					if (improvingMaxHPLevel >= 1) {
						MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
					}
				} else if (chr.getJobId() >= 200 && chr.getJobId() <= 232) { // Magician
					MaxHP += Randomizer.rand(6, 10);
				} else if (chr.getJobId() >= 300 && chr.getJobId() <= 322) { // Bowman
					MaxHP += Randomizer.rand(16, 20);
				} else if (chr.getJobId() >= 400 && chr.getJobId() <= 422) { // Thief
					MaxHP += Randomizer.rand(20, 24);
				} else if (chr.getJobId() >= 500 && chr.getJobId() <= 522) { // Pirate
					improvingMaxHP = SkillFactory.getSkill(5100000);
					improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
					MaxHP += Randomizer.rand(16, 20);
					if (improvingMaxHPLevel >= 1) {
						MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
					}
				} else if (chr.getJobId() >= 1100 && chr.getJobId() <= 1111) { // Soul Master
					improvingMaxHP = SkillFactory.getSkill(11000000);
					improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
					MaxHP += Randomizer.rand(36, 42);
					if (improvingMaxHPLevel >= 1) {
						MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
					}
				} else if (chr.getJobId() >= 1200 && chr.getJobId() <= 1211) { // Flame Wizard
					MaxHP += Randomizer.rand(15, 21);
				} else if ((chr.getJobId() >= 1300 && chr.getJobId() <= 1311) || (chr.getJobId() >= 1400 && chr.getJobId() <= 1411)) { // Wind Breaker and Night Walker
					MaxHP += Randomizer.rand(30, 36);
				} else { // GameMaster
					MaxHP += Randomizer.rand(50, 100);
				}
				MaxHP = Math.min(30000, MaxHP);
				chr.setHpApUsed(chr.getHpApUsed() + 1);
				stat.setMaxHp(MaxHP);
				statupdate.add(new StatValue(Stat.MAX_HP, MaxHP));
				break;
			case 8192: // MP
				int MaxMP = stat.getMaxMp();
				if (chr.getMpApUsed() >= 10000 && stat.getMaxMp() >= 30000) {
					return;
				}
				if (chr.getJobId() == 0) { // Beginner
					MaxMP += Randomizer.rand(6, 8);
				} else if (chr.getJobId() >= 100 && chr.getJobId() <= 132) { // Warrior
					MaxMP += Randomizer.rand(2, 4);
				} else if (chr.getJobId() >= 200 && chr.getJobId() <= 232) { // Magician
					ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
					int improvingMaxMPLevel = chr.getCurrentSkillLevel(improvingMaxMP);
					if (improvingMaxMPLevel >= 1) {
						MaxMP += Randomizer.rand(18, 20) + improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
					} else {
						MaxMP += Randomizer.rand(18, 20);
					}
				} else if (chr.getJobId() >= 300 && chr.getJobId() <= 322) { // Bowman
					MaxMP += Randomizer.rand(10, 12);
				} else if (chr.getJobId() >= 400 && chr.getJobId() <= 422) { // Thief
					MaxMP += Randomizer.rand(10, 12);
				} else if (chr.getJobId() >= 500 && chr.getJobId() <= 522) { // Pirate
					MaxMP += Randomizer.rand(10, 12);
				} else if (chr.getJobId() >= 1100 && chr.getJobId() <= 1111) { // Soul Master
					MaxMP += Randomizer.rand(6, 9);
				} else if (chr.getJobId() >= 1200 && chr.getJobId() <= 1211) { // Flame Wizard
					ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
					int improvingMaxMPLevel = chr.getCurrentSkillLevel(improvingMaxMP);
					MaxMP += Randomizer.rand(33, 36);
					if (improvingMaxMPLevel >= 1) {
						MaxMP += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
					}
				} else if ((chr.getJobId() >= 1300 && chr.getJobId() <= 1311) || (chr.getJobId() >= 1400 && chr.getJobId() <= 1411)) { // Wind Breaker and Night Walker
					MaxMP += Randomizer.rand(21, 24);
				} else { // GameMaster
					MaxMP += Randomizer.rand(50, 100);
				}
				MaxMP = Math.min(30000, MaxMP);
				chr.setMpApUsed(chr.getMpApUsed() + 1);
				stat.setMaxMp(MaxMP);
				statupdate.add(new StatValue(Stat.MAX_MP, MaxMP));
				break;
			default:
				c.write(ChannelPackets.updatePlayerStats(ChannelPackets.EMPTY_STATUPDATE, true, chr.getJobId()));
				return;
			}
			chr.setRemainingAp(chr.getRemainingAp() - 1);
			statupdate.add(new StatValue(Stat.AVAILABLE_AP, chr.getRemainingAp()));
			c.write(ChannelPackets.updatePlayerStats(statupdate, true, chr.getJobId()));
		}
	}

	public static void handleDistributeSkillPoints(final int skillid, final ChannelClient c, final ChannelCharacter chr) {
		boolean isBeginnerSkill = false;
		int remainingSp = 0;

		switch (skillid) {
		case 1000:
		case 1001:
		case 1002: {
			final int snailsLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(1000));
			final int recoveryLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(1001));
			final int nimbleFeetLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(1002));
			remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
			isBeginnerSkill = true;
			break;
		}
		case 10001000:
		case 10001001:
		case 10001002: {
			final int snailsLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(10001000));
			final int recoveryLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(10001001));
			final int nimbleFeetLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(10001002));
			remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
			isBeginnerSkill = true;
			break;
		}
		case 20001000:
		case 20001001:
		case 20001002: {
			final int snailsLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20001000));
			final int recoveryLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20001001));
			final int nimbleFeetLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20001002));
			remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
			isBeginnerSkill = true;
			break;
		}
		case 20011000:
		case 20011001:
		case 20011002: {
			final int snailsLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20011000));
			final int recoveryLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20011001));
			final int nimbleFeetLevel = chr.getCurrentSkillLevel(SkillFactory.getSkill(20011002));
			remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
			isBeginnerSkill = true;
			break;
		}
		default: {
			remainingSp = chr.getRemainingSp(Skills.getSkillbookForSkill(skillid));
			break;
		}
		}
		final ISkill skill = SkillFactory.getSkill(skillid);

		if (skill.hasRequiredSkill()) {
			if (chr.getCurrentSkillLevel(SkillFactory.getSkill(skill.getRequiredSkillId())) < skill.getRequiredSkillLevel()) {
				AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill without the required skill (" + skillid + ")");
				return;
			}
		}
		final int maxlevel = skill.isFourthJob() ? chr.getMasterSkillLevel(skill) : skill.getMaxLevel();
		final int curLevel = chr.getCurrentSkillLevel(skill);

		if (skill.isInvisible() && chr.getCurrentSkillLevel(skill) == 0) {
			if ((skill.isFourthJob() && chr.getMasterSkillLevel(skill) == 0) || !skill.isFourthJob() && maxlevel < 10) {
				AutobanManager.getInstance().addPoints(c, 1000, 0, "Illegal distribution of SP to invisible skills (" + skillid + ")");
				return;
			}
		}

		if ((remainingSp > 0 && curLevel + 1 <= maxlevel) && skill.canBeLearnedBy(chr.getJobId())) {
			if (!isBeginnerSkill) {
				final int skillbook = Skills.getSkillbookForSkill(skillid);
				chr.setRemainingSp(chr.getRemainingSp(skillbook) - 1, skillbook);
			}
			chr.updateSingleStat(Stat.AVAILABLE_SP, chr.getRemainingSp());
			chr.changeSkillLevel(skill, (byte) (curLevel + 1), chr.getMasterSkillLevel(skill));
		} else if (!skill.canBeLearnedBy(chr.getJobId())) {
			AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill for a different job (" + skillid + ")");
		}
	}

	public static void handleAutoAssignAbilityPoints(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(8);
		final int PrimaryStat = reader.readInt();
		final int amount = reader.readInt();
		final int SecondaryStat = reader.readInt();
		final int amount2 = reader.readInt();
		final ActivePlayerStats playerst = chr.getStats();
		List<StatValue> statupdate = new ArrayList<>(2);
		c.write(ChannelPackets.updatePlayerStats(statupdate, true, chr.getJobId()));
		if (chr.getRemainingAp() == amount + amount2) {
			switch (PrimaryStat) {
			case 64: // Str
				if (playerst.getStr() + amount > 999) {
					return;
				}
				playerst.setStr(playerst.getStr() + amount);
				statupdate.add(new StatValue(Stat.STR, playerst.getStr()));
				break;
			case 128: // Dex
				if (playerst.getDex() + amount > 999) {
					return;
				}
				playerst.setDex(playerst.getDex() + amount);
				statupdate.add(new StatValue(Stat.DEX, playerst.getDex()));
				break;
			case 256: // Int
				if (playerst.getInt() + amount > 999) {
					return;
				}
				playerst.setInt(playerst.getInt() + amount);
				statupdate.add(new StatValue(Stat.INT, playerst.getInt()));
				break;
			case 512: // Luk
				if (playerst.getLuk() + amount > 999) {
					return;
				}
				playerst.setLuk(playerst.getLuk() + amount);
				statupdate.add(new StatValue(Stat.LUK, playerst.getLuk()));
				break;
			default:
				c.write(ChannelPackets.updatePlayerStats(ChannelPackets.EMPTY_STATUPDATE, true, chr.getJobId()));
				return;
			}
			switch (SecondaryStat) {
			case 64: // Str
				if (playerst.getStr() + amount2 > 999) {
					return;
				}
				playerst.setStr(playerst.getStr() + amount2);
				statupdate.add(new StatValue(Stat.STR, playerst.getStr()));
				break;
			case 128: // Dex
				if (playerst.getDex() + amount2 > 999) {
					return;
				}
				playerst.setDex(playerst.getDex() + amount2);
				statupdate.add(new StatValue(Stat.DEX, playerst.getDex()));
				break;
			case 256: // Int
				if (playerst.getInt() + amount2 > 999) {
					return;
				}
				playerst.setInt(playerst.getInt() + amount2);
				statupdate.add(new StatValue(Stat.INT, playerst.getInt()));
				break;
			case 512: // Luk
				if (playerst.getLuk() + amount2 > 999) {
					return;
				}
				playerst.setLuk(playerst.getLuk() + amount2);
				statupdate.add(new StatValue(Stat.LUK, playerst.getLuk()));
				break;
			default:
				c.write(ChannelPackets.updatePlayerStats(ChannelPackets.EMPTY_STATUPDATE, true, chr.getJobId()));
				return;
			}
			chr.setRemainingAp(chr.getRemainingAp() - (amount + amount2));
			statupdate.add(new StatValue(Stat.AVAILABLE_AP, chr.getRemainingAp()));
			c.write(ChannelPackets.updatePlayerStats(statupdate, true, chr.getJobId()));
		}
	}
}