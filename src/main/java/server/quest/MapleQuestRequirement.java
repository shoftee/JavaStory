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
package server.quest;

import java.util.Calendar;
import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;

import client.IItem;
import client.SkillFactory;
import client.GameConstants;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MapleQuestStatus;
import provider.MapleData;
import provider.MapleDataTool;

import tools.Pair;

public class MapleQuestRequirement implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    
    private MapleQuest quest;
    private MapleQuestRequirementType type;
    private int intStore;
    private String stringStore;
    private List<Pair> dataStore;

    /** Creates a new instance of MapleQuestRequirement */
    public MapleQuestRequirement(MapleQuest quest, MapleQuestRequirementType type, MapleData data) {
	this.type = type;
	this.quest = quest;

	switch (type) {
	    case job: {
		final List<MapleData> child = data.getChildren();
		dataStore = new LinkedList<Pair>();

		for (int i = 0; i < child.size(); i++) {
		    dataStore.add(new Pair(i, MapleDataTool.getInt(child.get(i), -1)));
		}
		break;
	    }
	    case skill: {
		final List<MapleData> child = data.getChildren();
		dataStore = new LinkedList<Pair>();

		for (int i = 0; i < child.size(); i++) {
		    final MapleData childdata = child.get(i);
		    dataStore.add(new Pair(MapleDataTool.getInt(childdata.getChildByPath("id"), 0),
			    MapleDataTool.getInt(childdata.getChildByPath("acquire"), 0)));
		}
		break;
	    }
	    case quest: {
		final List<MapleData> child = data.getChildren();
		dataStore = new LinkedList<Pair>();

		for (int i = 0; i < child.size(); i++) {
		    final MapleData childdata = child.get(i);
		    dataStore.add(new Pair(MapleDataTool.getInt(childdata.getChildByPath("id")),
			    MapleDataTool.getInt(childdata.getChildByPath("state"), 0)));
		}
		break;
	    }
	    case item: {
		final List<MapleData> child = data.getChildren();
		dataStore = new LinkedList<Pair>();

		for (int i = 0; i < child.size(); i++) {
		    final MapleData childdata = child.get(i);
		    dataStore.add(new Pair(MapleDataTool.getInt(childdata.getChildByPath("id")),
			    MapleDataTool.getInt(childdata.getChildByPath("count"), 0)));
		}
		break;
	    }
	    case npc:
	    case questComplete:
	    case pop:
	    case interval:
	    case mbmin:
	    case lvmax:
	    case lvmin: {
		intStore = MapleDataTool.getInt(data, -1);
		break;
	    }
	    case end: {
		stringStore = MapleDataTool.getString(data, null);
		break;
	    }
	    case mob: {
		final List<MapleData> child = data.getChildren();
		dataStore = new LinkedList<Pair>();

		for (int i = 0; i < child.size(); i++) {
		    final MapleData childdata = child.get(i);
		    dataStore.add(new Pair(MapleDataTool.getInt(childdata.getChildByPath("id"), 0),
			    MapleDataTool.getInt(childdata.getChildByPath("count"), 0)));
		}
		break;
	    }
	    case fieldEnter: {
		final MapleData zeroField = data.getChildByPath("0");
		if (zeroField != null) {
		    intStore = MapleDataTool.getInt(zeroField);
		} else {
		    intStore = -1;
		}
		break;
	    }
	}
    }

    public boolean check(MapleCharacter c, Integer npcid) {
	switch (type) {
	    case job:
		for (Pair a : dataStore) {
		    if (a.getRight().equals(c.getJob()) || c.isGM()) {
			return true;
		    }
		}
		return false;
	    case skill: {
		for (Pair a : dataStore) {
		    final boolean acquire = ((Integer) a.getRight()) > 0;
		    final int skill = (Integer) a.getLeft();

		    if (acquire) {
			if (c.getMasterLevel(SkillFactory.getSkill(skill)) == 0) {
			    return false;
			}
		    } else {
			if (c.getMasterLevel(SkillFactory.getSkill(skill)) > 0) {
			    return false;
			}
		    }
		}
		return true;
	    }
	    case quest:
		for (Pair a : dataStore) {
		    final MapleQuestStatus q = c.getQuest(MapleQuest.getInstance((Integer) a.getLeft()));
		    final int state = (Integer) a.getRight();
		    if (state != 0) {
			if (q == null && state == 0) {
			    continue;
			}
			if (q == null || q.getStatus() != state) {
			    return false;
			}
		    }
		}
		return true;
	    case item:
		MapleInventoryType iType;
		int itemId;
		short quantity;

		for (Pair a : dataStore) {
		    itemId = (Integer) a.getLeft();
		    quantity = 0;
		    iType = GameConstants.getInventoryType(itemId);
		    for (IItem item : c.getInventory(iType).listById(itemId)) {
			quantity += item.getQuantity();
		    }
		    final int count = (Integer) a.getRight();
		    if (quantity < count || count <= 0 && quantity > 0) {
			return false;
		    }
		}
		return true;
	    case lvmin:
		return c.getLevel() >= intStore;
	    case lvmax:
		return c.getLevel() <= intStore;
	    case end:
		final String timeStr = stringStore;
		final Calendar cal = Calendar.getInstance();
		cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
		return cal.getTimeInMillis() >= System.currentTimeMillis();
	    case mob:
		for (Pair a : dataStore) {
		    final int mobId = (Integer) a.getLeft();
		    final int killReq = (Integer) a.getRight();
		    if (c.getQuest(quest).getMobKills(mobId) < killReq) {
			return false;
		    }
		}
		return true;
	    case npc:
		return npcid == null || npcid == intStore;
	    case fieldEnter:
		if (intStore != -1) {
		    return intStore == c.getMapId();
		}
		return false;
	    case mbmin:
		if (c.getMonsterBook().getTotalCards() >= intStore) {
		    return true;
		}
		return false;
	    case pop:
		return c.getFame() <= intStore;
	    case questComplete:
		if (c.getNumQuest() >= intStore) {
		    return true;
		}
		return false;
	    case interval:
		return c.getQuest(quest).getStatus() != 2 || c.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - intStore * 60 * 1000;
//			case PET:
//			case MIN_PET_TAMENESS:
	    default:
		return true;
	}
    }

    public MapleQuestRequirementType getType() {
	return type;
    }

    @Override
    public String toString() {
	return type.toString();
    }
}
