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
import java.awt.Rectangle;
import java.util.Map;

import javastory.game.IdQuantityEntry;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

import com.google.common.collect.Maps;

public class ReactorInfo {

	private final Rectangle bounds;
	private final Map<Integer, StateData> stateInfo = Maps.newHashMap();
	private final Map<Integer, Integer> stateGraph = Maps.newHashMap();

	public ReactorInfo(int id, WzData data) {
		WzData info = data.getChildByPath("0/event/0");
		Rectangle bounds = null;
		boolean areaSet = false;
		if (info != null) {
			int stateId = 0;
			while (data != null) {
				IdQuantityEntry reactionEntry = null;
				final int type = WzDataTool.getIntConvert("type", data);
				if (type == 100) {
					// Reactor is triggered by an item cluster.
					final int itemId = WzDataTool.getIntConvert("0", data);
					final int quantity = WzDataTool.getIntConvert("1", data);
					reactionEntry = new IdQuantityEntry(itemId, quantity);
					if (!areaSet) {
						final Point lt = WzDataTool.getVector("lt", data);
						final Point rb = WzDataTool.getVector("rb", data);
						bounds = new Rectangle(lt.x, lt.y, rb.x - lt.x, rb.y - lt.y);
						areaSet = true;
					}
					StateData state = new StateData(type, reactionEntry);
					stateInfo.put(type, state);
				}
				final int nextStateId = WzDataTool.getIntConvert("state", data);
				stateGraph.put(stateId, nextStateId);
				stateId++;
				data = data.getChildByPath(stateId + "/event/0");
			}
		} else {
			// sit there and look pretty; likely a reactor such as Zakum/Papulatus doors that shows if player can enter
			stateInfo.put(0, new StateData(999));
			stateGraph.put(0, 0);
		}
		this.bounds = bounds;
	}

	public int getNextState(final int stateId) {
		final Integer nextId = stateGraph.get(stateId);
		return nextId == null ? -1 : nextId.intValue();
	}

	public int getType(final int stateId) {
		final StateData nextState = this.stateInfo.get(stateId);
		if (nextState != null) {
			return nextState.getType();
		} else {
			return -1;
		}
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public IdQuantityEntry getReactionItem(final int stateId) {
		final StateData nextState = this.stateInfo.get(stateId);
		if (nextState != null) {
			return nextState.getReactionItem();
		} else {
			return null;
		}
	}

	private static class StateData {

		private final int type;
		private final IdQuantityEntry reactionItem;

		private StateData(final int type) {
			this.type = type;
			this.reactionItem = null;
		}

		private StateData(final int type, final IdQuantityEntry reactionItem) {
			this.type = type;
			this.reactionItem = reactionItem;
		}

		private int getType() {
			return this.type;
		}

		private IdQuantityEntry getReactionItem() {
			return this.reactionItem;
		}
	}
}
