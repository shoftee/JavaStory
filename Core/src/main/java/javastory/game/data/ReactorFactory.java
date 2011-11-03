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

import javastory.tools.Pair;
import javastory.tools.StringUtil;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public class ReactorFactory {

	private static final WzDataProvider reactorDataProvider = WzDataProviderFactory.getDataProvider("Reactor.wz");
	private static Map<Integer, ReactorInfo> reactorInfoCache = Maps.newHashMap();

	public static ReactorInfo getReactor(final int reactorId) {
		ReactorInfo reactorInfo = reactorInfoCache.get(Integer.valueOf(reactorId));
		if (reactorInfo == null) {
			int infoId = reactorId;
			WzData data = reactorDataProvider.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
			final WzData link = data.getChildByPath("info/link");
			
			if (link != null) {
				infoId = WzDataTool.getIntConvert("info/link", data);
				reactorInfo = reactorInfoCache.get(Integer.valueOf(infoId));
			}
			
			if (reactorInfo == null) {
				data = reactorDataProvider.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
				reactorInfo = new ReactorInfo(reactorId, data);
				reactorInfoCache.put(Integer.valueOf(infoId), reactorInfo);
				if (reactorId != infoId) {
					reactorInfoCache.put(Integer.valueOf(reactorId), reactorInfo);
				}
			} else { 
				// stats exist at infoId but not rid; add to map
				reactorInfoCache.put(Integer.valueOf(reactorId), reactorInfo);
			}
		}
		return reactorInfo;
	}
}
