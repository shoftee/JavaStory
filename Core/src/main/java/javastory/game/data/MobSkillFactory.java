/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.game.data;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import javastory.channel.life.MobSkill;
import javastory.game.SkillLevelEntry;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class MobSkillFactory {

	private static final WzDataProvider dataSource = WzDataProviderFactory.getDataProvider("Skill.wz");
	private static final WzData skillRoot = dataSource.getData("MobSkill.img");

	private static final Map<SkillLevelEntry, MobSkill> mobSkills = Maps.newHashMap();

	private MobSkillFactory() {
	}

	public static MobSkill getMobSkill(final int skillId, final int level) {
		final SkillLevelEntry entry = new SkillLevelEntry(skillId, level);
		MobSkill ret = mobSkills.get(entry);
		if (ret != null) {
			return ret;
		}

		final WzData data = skillRoot.getChildByPath(skillId + "/level/" + level);
		if (data != null) {
			ret = new MobSkill(skillId, level);

			final List<Integer> summons = Lists.newArrayList();
			int i = 0;
			WzData summonData = data.getChildByPath(String.valueOf(i));
			while (summonData != null) {
				final int id = WzDataTool.getInt(summonData);
				summons.add(Integer.valueOf(id));
				i++;
				summonData = data.getChildByPath(String.valueOf(i));
			}

			// TS NOTE: I'll preserve the old code, to enable making fun of the
			// one who wrote it:
//			for (int i = 0; i > -1; i++) {
//				if (data.getChildByPath(String.valueOf(i)) == null) {
//					break;
//				}
//				
//				toSummon.add(Integer.valueOf(WzDataTool.getInt(data.getChildByPath(String.valueOf(i)), 0)));
//			}

			final WzData ltd = data.getChildByPath("lt");
			Point lt = null;
			Point rb = null;
			if (ltd != null) {
				lt = (Point) ltd.getData();
				rb = (Point) data.getChildByPath("rb").getData();
			}

			ret.addSummons(summons);
			ret.setCoolTime(WzDataTool.getInt("interval", data, 0) * 1000);
			ret.setDuration(WzDataTool.getInt("time", data, 0) * 1000);
			ret.setHp(WzDataTool.getInt("hp", data, 100));
			ret.setMpCon(WzDataTool.getInt(data.getChildByPath("mpCon"), 0));
			ret.setSpawnEffect(WzDataTool.getInt("summonEffect", data, 0));
			ret.setX(WzDataTool.getInt("x", data, 1));
			ret.setY(WzDataTool.getInt("y", data, 1));
			ret.setProp(WzDataTool.getInt("prop", data, 100) / 100);
			ret.setLimit((short) WzDataTool.getInt("limit", data, 0));
			ret.setLtRb(lt, rb);

			mobSkills.put(entry, ret);
		}
		return ret;
	}
}
