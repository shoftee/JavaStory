package handling.channel.handler;

import client.GameConstants;
import java.util.ArrayList;
import java.util.List;

import client.ISkill;
import client.GameClient;
import client.GameCharacter;
import client.Stat;
import client.PlayerStats;
import client.SkillFactory;
import org.javastory.io.PacketFormatException;
import server.AutobanManager;
import org.javastory.tools.Randomizer;
import tools.MaplePacketCreator;
import tools.Pair;
import org.javastory.io.PacketReader;

public class StatsHandling {

    public static final void handleDistributeAbilityPoints(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        final List<Pair<Stat, Integer>> statupdate = new ArrayList<Pair<Stat, Integer>>(2);
        c.write(MaplePacketCreator.updatePlayerStats(statupdate, true, chr.getJob()));
        reader.skip(4);

        final PlayerStats stat = chr.getStat();

        if (chr.getRemainingAp() > 0) {
            switch (reader.readInt()) {
                case 64: // Str
                    if (stat.getStr() >= c.getPlayer().getMaxStats()) {
                        return;
                    }
                    stat.setStr(stat.getStr() + 1);
                    statupdate.add(new Pair<Stat, Integer>(Stat.STR, stat.getStr()));
                    break;
                case 128: // Dex
                    if (stat.getDex() >= c.getPlayer().getMaxStats()) {
                        return;
                    }
                    stat.setDex(stat.getDex() + 1);
                    statupdate.add(new Pair<Stat, Integer>(Stat.DEX, stat.getDex()));
                    break;
                case 256: // Int
                    if (stat.getInt() >= c.getPlayer().getMaxStats()) {
                        return;
                    }
                    stat.setInt(stat.getInt() + 1);
                    statupdate.add(new Pair<Stat, Integer>(Stat.INT, stat.getInt()));
                    break;
                case 512: // Luk
                    if (stat.getLuk() >= c.getPlayer().getMaxStats()) {
                        return;
                    }
                    stat.setLuk(stat.getLuk() + 1);
                    statupdate.add(new Pair<Stat, Integer>(Stat.LUK, stat.getLuk()));
                    break;
                case 2048: // HP
                    int MaxHP = stat.getMaxHp();
                    if (chr.getHpApUsed() >= 10000 || MaxHP >= 30000) {
                        return;
                    }
                    ISkill improvingMaxHP = null;
                    int improvingMaxHPLevel = 0;
                    if (chr.getJob() == 0) { // Beginner
                        MaxHP += Randomizer.rand(8, 12);
                    } else if (chr.getJob() >= 100 && chr.getJob() <= 132) { // Warrior
                        improvingMaxHP = SkillFactory.getSkill(1000001);
                        improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
                        MaxHP += Randomizer.rand(20, 24);
                        if (improvingMaxHPLevel >= 1) {
                            MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                        }
                    } else if (chr.getJob() >= 200 && chr.getJob() <= 232) { // Magician
                        MaxHP += Randomizer.rand(6, 10);
                    } else if (chr.getJob() >= 300 && chr.getJob() <= 322) { // Bowman
                        MaxHP += Randomizer.rand(16, 20);
                    } else if (chr.getJob() >= 400 && chr.getJob() <= 422) { // Thief
                        MaxHP += Randomizer.rand(20, 24);
                    } else if (chr.getJob() >= 500 && chr.getJob() <= 522) { // Pirate
                        improvingMaxHP = SkillFactory.getSkill(5100000);
                        improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
                        MaxHP += Randomizer.rand(16, 20);
                        if (improvingMaxHPLevel >= 1) {
                            MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                        }
                    } else if (chr.getJob() >= 1100 && chr.getJob() <= 1111) { // Soul Master
                        improvingMaxHP = SkillFactory.getSkill(11000000);
                        improvingMaxHPLevel = chr.getCurrentSkillLevel(improvingMaxHP);
                        MaxHP += Randomizer.rand(36, 42);
                        if (improvingMaxHPLevel >= 1) {
                            MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                        }
                    } else if (chr.getJob() >= 1200 && chr.getJob() <= 1211) { // Flame Wizard
                        MaxHP += Randomizer.rand(15, 21);
                    } else if ((chr.getJob() >= 1300 && chr.getJob() <= 1311) || (chr.getJob() >= 1400 && chr.getJob() <= 1411)) { // Wind Breaker and Night Walker
                        MaxHP += Randomizer.rand(30, 36);
                    } else { // GameMaster
                        MaxHP += Randomizer.rand(50, 100);
                    }
                    MaxHP = Math.min(30000, MaxHP);
                    chr.setHpApUsed(chr.getHpApUsed() + 1);
                    stat.setMaxHp(MaxHP);
                    statupdate.add(new Pair<Stat, Integer>(Stat.MAXHP, MaxHP));
                    break;
                case 8192: // MP
                    int MaxMP = stat.getMaxMp();
                    if (chr.getMpApUsed() >= 10000 && stat.getMaxMp() >= 30000) {
                        return;
                    }
                    if (chr.getJob() == 0) { // Beginner
                        MaxMP += Randomizer.rand(6, 8);
                    } else if (chr.getJob() >= 100 && chr.getJob() <= 132) { // Warrior
                        MaxMP += Randomizer.rand(2, 4);
                    } else if (chr.getJob() >= 200 && chr.getJob() <= 232) { // Magician
                        ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                        int improvingMaxMPLevel = chr.getCurrentSkillLevel(improvingMaxMP);
                        if (improvingMaxMPLevel >= 1) {
                            MaxMP += Randomizer.rand(18, 20) + improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                        } else {
                            MaxMP += Randomizer.rand(18, 20);
                        }
                    } else if (chr.getJob() >= 300 && chr.getJob() <= 322) { // Bowman
                        MaxMP += Randomizer.rand(10, 12);
                    } else if (chr.getJob() >= 400 && chr.getJob() <= 422) { // Thief
                        MaxMP += Randomizer.rand(10, 12);
                    } else if (chr.getJob() >= 500 && chr.getJob() <= 522) { // Pirate
                        MaxMP += Randomizer.rand(10, 12);
                    } else if (chr.getJob() >= 1100 && chr.getJob() <= 1111) { // Soul Master
                        MaxMP += Randomizer.rand(6, 9);
                    } else if (chr.getJob() >= 1200 && chr.getJob() <= 1211) { // Flame Wizard
                        ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
                        int improvingMaxMPLevel = chr.getCurrentSkillLevel(improvingMaxMP);
                        MaxMP += Randomizer.rand(33, 36);
                        if (improvingMaxMPLevel >= 1) {
                            MaxMP += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                        }
                    } else if ((chr.getJob() >= 1300 && chr.getJob() <= 1311) || (chr.getJob() >= 1400 && chr.getJob() <= 1411)) { // Wind Breaker and Night Walker
                        MaxMP += Randomizer.rand(21, 24);
                    } else { // GameMaster
                        MaxMP += Randomizer.rand(50, 100);
                    }
                    MaxMP = Math.min(30000, MaxMP);
                    chr.setMpApUsed(chr.getMpApUsed() + 1);
                    stat.setMaxMp(MaxMP);
                    statupdate.add(new Pair<Stat, Integer>(Stat.MAXMP, MaxMP));
                    break;
                default:
                    c.write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, chr.getJob()));
                    return;
            }
            chr.setRemainingAp(chr.getRemainingAp() - 1);
            statupdate.add(new Pair<Stat, Integer>(Stat.AVAILABLEAP, chr.getRemainingAp()));
            c.write(MaplePacketCreator.updatePlayerStats(statupdate, true, chr.getJob()));
        }
    }

    public static final void handleDistributeSkillPoints(final int skillid, final GameClient c, final GameCharacter chr) {
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
                remainingSp = chr.getRemainingSp(GameConstants.getSkillBookForSkill(skillid));
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

        if ((remainingSp > 0 && curLevel + 1 <= maxlevel) && skill.canBeLearnedBy(chr.getJob())) {
            if (!isBeginnerSkill) {
                final int skillbook = GameConstants.getSkillBookForSkill(skillid);
                chr.setRemainingSp(chr.getRemainingSp(skillbook) - 1, skillbook);
            }
            chr.updateSingleStat(Stat.AVAILABLESP, chr.getRemainingSp());
            chr.changeSkillLevel(skill, (byte) (curLevel + 1), chr.getMasterSkillLevel(skill));
        } else if (!skill.canBeLearnedBy(chr.getJob())) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill for a different job (" + skillid + ")");
        }
    }

    public static final void handleAutoAssignAbilityPoints(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        reader.skip(8);
        final int PrimaryStat = reader.readInt();
        final int amount = reader.readInt();
        final int SecondaryStat = reader.readInt();
        final int amount2 = reader.readInt();
        final PlayerStats playerst = chr.getStat();
        List<Pair<Stat, Integer>> statupdate = new ArrayList<Pair<Stat, Integer>>(2);
        c.write(MaplePacketCreator.updatePlayerStats(statupdate, true, chr.getJob()));
        if (chr.getRemainingAp() == amount + amount2) {
            switch (PrimaryStat) {
                case 64: // Str
                    if (playerst.getStr() + amount > 999) {
                        return;
                    }
                    playerst.setStr(playerst.getStr() + amount);
                    statupdate.add(new Pair<Stat, Integer>(Stat.STR, playerst.getStr()));
                    break;
                case 128: // Dex
                    if (playerst.getDex() + amount > 999) {
                        return;
                    }
                    playerst.setDex(playerst.getDex() + amount);
                    statupdate.add(new Pair<Stat, Integer>(Stat.DEX, playerst.getDex()));
                    break;
                case 256: // Int
                    if (playerst.getInt() + amount > 999) {
                        return;
                    }
                    playerst.setInt(playerst.getInt() + amount);
                    statupdate.add(new Pair<Stat, Integer>(Stat.INT, playerst.getInt()));
                    break;
                case 512: // Luk
                    if (playerst.getLuk() + amount > 999) {
                        return;
                    }
                    playerst.setLuk(playerst.getLuk() + amount);
                    statupdate.add(new Pair<Stat, Integer>(Stat.LUK, playerst.getLuk()));
                    break;
                default:
                    c.write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, chr.getJob()));
                    return;
            }
            switch (SecondaryStat) {
                case 64: // Str
                    if (playerst.getStr() + amount2 > 999) {
                        return;
                    }
                    playerst.setStr(playerst.getStr() + amount2);
                    statupdate.add(new Pair<Stat, Integer>(Stat.STR, playerst.getStr()));
                    break;
                case 128: // Dex
                    if (playerst.getDex() + amount2 > 999) {
                        return;
                    }
                    playerst.setDex(playerst.getDex() + amount2);
                    statupdate.add(new Pair<Stat, Integer>(Stat.DEX, playerst.getDex()));
                    break;
                case 256: // Int
                    if (playerst.getInt() + amount2 > 999) {
                        return;
                    }
                    playerst.setInt(playerst.getInt() + amount2);
                    statupdate.add(new Pair<Stat, Integer>(Stat.INT, playerst.getInt()));
                    break;
                case 512: // Luk
                    if (playerst.getLuk() + amount2 > 999) {
                        return;
                    }
                    playerst.setLuk(playerst.getLuk() + amount2);
                    statupdate.add(new Pair<Stat, Integer>(Stat.LUK, playerst.getLuk()));
                    break;
                default:
                    c.write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, chr.getJob()));
                    return;
            }
            chr.setRemainingAp(chr.getRemainingAp() - (amount + amount2));
            statupdate.add(new Pair<Stat, Integer>(Stat.AVAILABLEAP, chr.getRemainingAp()));
            c.write(MaplePacketCreator.updatePlayerStats(statupdate, true, chr.getJob()));
        }
    }
}