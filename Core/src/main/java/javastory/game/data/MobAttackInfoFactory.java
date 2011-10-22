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

import java.util.Map;

import javastory.tools.StringUtil;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public final class MobAttackInfoFactory {

	private static final WzDataProvider dataRoot = WzDataProviderFactory.getDataProvider("Mob.wz");

	private static final MobAttackInfoFactory instance = new MobAttackInfoFactory();

	public static MobAttackInfoFactory getInstance() {
		return instance;
	}

	private final Map<MobAttackId, MobAttackInfo> mobAttacks;

	private MobAttackInfoFactory() {
		this.mobAttacks = Maps.newHashMap();
	}

	public MobAttackInfo getMobAttackInfo(int mobId, int attackId) {
		final MobAttackId mobAttackId = new MobAttackId(mobId, attackId);
		MobAttackInfo ret = mobAttacks.get(mobAttackId);
		if (ret != null) {
			return ret;
		}

		WzData mobData = dataRoot.getData(StringUtil.getLeftPaddedStr(Integer.toString(mobId) + ".img", '0', 11));
		if (mobData == null) {
			return null;
		}

		WzData infoData = mobData.getChildByPath("info/link");

		if (infoData != null) {
			String linkedId = WzDataTool.getString("info/link", mobData);
			String paddedId = StringUtil.getLeftPaddedStr(linkedId, '0', 7);
			mobData = dataRoot.getData(paddedId);
		}

		final WzData attackData = mobData.getChildByPath("attack" + (attackId + 1) + "/info");
		if (attackData == null) {
			return null;
		}

		ret = new MobAttackInfo(attackData);
		mobAttacks.put(mobAttackId, ret);

		return ret;
	}
}
