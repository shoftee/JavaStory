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
package server.maps;

import java.util.HashMap;
import java.util.Map;

import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;
import tools.Pair;
import tools.StringUtil;

public class ReactorFactory {

    private static final WzDataProvider data = WzDataProviderFactory.getDataProvider("Reactor.wz");
    private static Map<Integer, ReactorStats> reactorStats = new HashMap<>();

    public static ReactorStats getReactor(int rid) {
        ReactorStats stats = reactorStats.get(Integer.valueOf(rid));
        if (stats == null) {
            int infoId = rid;
            WzData reactorData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) +
                    ".img", '0', 11));
            WzData link = reactorData.getChildByPath("info/link");
            if (link != null) {
                infoId = WzDataTool.getIntConvert("info/link", reactorData);
                stats = reactorStats.get(Integer.valueOf(infoId));
            }
            if (stats == null) {
                reactorData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) +
                        ".img", '0', 11));
                WzData reactorInfoData = reactorData.getChildByPath("0/event/0");
                stats = new ReactorStats();

                if (reactorInfoData != null) {
                    boolean areaSet = false;
                    int i = 0;
                    while (reactorInfoData != null) {
                        Pair<Integer, Integer> reactItem = null;
                        int type = WzDataTool.getIntConvert("type", reactorInfoData);
                        if (type == 100) { //reactor waits for item
                            reactItem = new Pair<Integer, Integer>(
                                    WzDataTool.getIntConvert("0", reactorInfoData),
                                    WzDataTool.getIntConvert("1", reactorInfoData));
                            if (!areaSet) { //only set area of effect for item-triggered reactors once
                                stats.setTL(WzDataTool.getPoint("lt", reactorInfoData));
                                stats.setBR(WzDataTool.getPoint("rb", reactorInfoData));
                                areaSet = true;
                            }
                        }
                        stats.addState((byte) i, type, reactItem, (byte) WzDataTool.getIntConvert("state", reactorInfoData));
                        i++;
                        reactorInfoData = reactorData.getChildByPath(i +
                                "/event/0");
                    }
                } else { //sit there and look pretty; likely a reactor such as Zakum/Papulatus doors that shows if player can enter
                    stats.addState((byte) 0, 999, null, (byte) 0);
                }
                reactorStats.put(Integer.valueOf(infoId), stats);
                if (rid != infoId) {
                    reactorStats.put(Integer.valueOf(rid), stats);
                }
            } else { // stats exist at infoId but not rid; add to map
                reactorStats.put(Integer.valueOf(rid), stats);
            }
        }
        return stats;
    }
}
