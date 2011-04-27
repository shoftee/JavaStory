package client;

import java.util.HashMap;
import java.util.Map;

import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataFileEntry;
import provider.WzDataProviderFactory;
import provider.WzDataDirectoryEntry;
import provider.WzDataTool;
import tools.StringUtil;

public final class SkillFactory {

    private static final Map<Integer, ISkill> skills = new HashMap<>();
    private static final Map<Integer, SummonSkillEntry> summonSkills = new HashMap<>();
    private final static WzData stringData = WzDataProviderFactory.getDataProvider("String.wz").getData("Skill.img");

    private SkillFactory() {
    }

    public static ISkill getSkill(final int id) {
        if (!skills.isEmpty()) {
            return skills.get(Integer.valueOf(id));
        }
        System.out.println(":: Loading SkillFactory ::");
        final WzDataProvider datasource = WzDataProviderFactory.getDataProvider("Skill.wz");
        final WzDataDirectoryEntry root = datasource.getRoot();
        int skillId;
        WzData summonData;
        SummonSkillEntry summonSkillEntry;
        for (WzDataFileEntry topDir : root.getFiles()) {
            // Loop thru jobs
            if (topDir.getName().length() > 8) {
                continue;
            }
            for (WzData job : datasource.getData(topDir.getName())) {
                // Loop thru each jobs
                if (!job.getName().equals("skill")) {
                    continue;
                }
                for (WzData skill : job) {
                    // Loop thru each jobs
                    if (skill == null) {
                        continue;
                    }
                    skillId = Integer.parseInt(skill.getName());
                    skills.put(skillId, Skill.loadFromData(skillId, skill));
                    summonData = skill.getChildByPath("summon/attack1/info");
                    if (summonData == null) {
                        continue;
                    }
                    summonSkillEntry = new SummonSkillEntry();
                    summonSkillEntry.attackAfter = (short) WzDataTool.getInt("attackAfter", summonData, 999999);
                    summonSkillEntry.type = (byte) WzDataTool.getInt("type", summonData, 0);
                    summonSkillEntry.mobCount = (byte) WzDataTool.getInt("mobCount", summonData, 1);
                    summonSkills.put(skillId, summonSkillEntry);
                }
            }
        }
        return null;
    }

    public static String getSkillName(final int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        WzData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return WzDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return null;
    }

    public static SummonSkillEntry getSummonData(final int skillid) {
        return summonSkills.get(skillid);
    }
}