/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.life;

import javastory.game.SkillLevelEntry;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public final class MobSkillFactory {

    private static final Map<SkillLevelEntry, MobSkill> mobSkills = new HashMap<>();
    private static final WzDataProvider dataSource = WzDataProviderFactory.getDataProvider("Skill.wz");
    private static final WzData skillRoot = dataSource.getData("MobSkill.img");

    private MobSkillFactory() {
    }

    public static MobSkill getMobSkill(int skillId, int level) {
        final SkillLevelEntry entry = new SkillLevelEntry(skillId, level);
        MobSkill ret = mobSkills.get(entry);
        if (ret != null) {
            return ret;
        }

        final WzData skillData = skillRoot.getChildByPath(skillId + "/level/" +
                level);
        if (skillData != null) {
            List<Integer> toSummon = new ArrayList<>();
            for (int i = 0; i > -1; i++) {
                if (skillData.getChildByPath(String.valueOf(i)) == null) {
                    break;
                }
                toSummon.add(Integer.valueOf(WzDataTool.getInt(skillData.getChildByPath(String.valueOf(i)), 0)));
            }
            final WzData ltd = skillData.getChildByPath("lt");
            Point lt = null;
            Point rb = null;
            if (ltd != null) {
                lt = (Point) ltd.getData();
                rb = (Point) skillData.getChildByPath("rb").getData();
            }
            ret = new MobSkill(skillId, level);
            ret.addSummons(toSummon);
            ret.setCoolTime(WzDataTool.getInt("interval", skillData, 0) * 1000);
            ret.setDuration(WzDataTool.getInt("time", skillData, 0) * 1000);
            ret.setHp(WzDataTool.getInt("hp", skillData, 100));
            ret.setMpCon(WzDataTool.getInt(skillData.getChildByPath("mpCon"), 0));
            ret.setSpawnEffect(WzDataTool.getInt("summonEffect", skillData, 0));
            ret.setX(WzDataTool.getInt("x", skillData, 1));
            ret.setY(WzDataTool.getInt("y", skillData, 1));
            ret.setProp(WzDataTool.getInt("prop", skillData, 100) / 100);
            ret.setLimit((short) WzDataTool.getInt("limit", skillData, 0));
            ret.setLtRb(lt, rb);

            mobSkills.put(entry, ret);
        }
        return ret;
    }
}
