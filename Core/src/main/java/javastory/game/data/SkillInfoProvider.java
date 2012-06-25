package javastory.game.data;

import java.util.Map;

import javastory.channel.client.ISkill;
import javastory.channel.client.Skill;
import javastory.tools.StringUtil;
import javastory.wz.WzData;
import javastory.wz.WzDataDirectoryEntry;
import javastory.wz.WzDataFileEntry;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public final class SkillInfoProvider {

	private static final Map<Integer, ISkill> skills = Maps.newHashMap();
	private static final Map<Integer, SummonSkillEntry> summonSkills = Maps.newHashMap();
	private final static WzData stringData = WzDataProviderFactory.getDataProvider("String.wz").getData("Skill.img");

	private SkillInfoProvider() {
	}

	public static ISkill getSkill(final int id) {
		if (!skills.isEmpty()) {
			return skills.get(Integer.valueOf(id));
		}
		System.out.println(":: Loading SkillInfoProvider ::");
		final WzDataProvider datasource = WzDataProviderFactory.getDataProvider("Skill.wz");
		final WzDataDirectoryEntry root = datasource.getRoot();
		for (final WzDataFileEntry topDir : root.getFiles()) {
			// Loop thru jobs
			if (topDir.getName().length() > 8) {
				continue;
			}
			for (final WzData job : datasource.getData(topDir.getName())) {
				// Loop thru each jobs
				if (!job.getName().equals("skill")) {
					continue;
				}
				for (final WzData skill : job) {
					// Loop thru each jobs
					if (skill == null) {
						continue;
					}
					final int skillId = Integer.parseInt(skill.getName());
					skills.put(skillId, Skill.loadFromData(skillId, skill));
					final WzData summonData = skill.getChildByPath("summon/attack1/info");
					if (summonData == null) {
						continue;
					}
					
					final SummonSkillEntry summonSkillEntry = new SummonSkillEntry(summonData);
					summonSkills.put(skillId, summonSkillEntry);
				}
			}
		}
		return null;
	}

	public static String getSkillName(final int id) {
		String strId = Integer.toString(id);
		strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
		final WzData skillroot = stringData.getChildByPath(strId);
		if (skillroot != null) {
			return WzDataTool.getString(skillroot.getChildByPath("name"), "");
		}
		return null;
	}

	public static SummonSkillEntry getSummonData(final int skillid) {
		return summonSkills.get(skillid);
	}
}