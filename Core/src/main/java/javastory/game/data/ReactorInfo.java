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
import java.util.Map;

import javastory.tools.Pair;

import com.google.common.collect.Maps;

public class ReactorInfo {

	private byte facingDirection;
	private Point tl;
	private Point br;
	private final Map<Byte, StateData> stateInfo = Maps.newHashMap();

	public final void setFacingDirection(final byte facingDirection) {
		this.facingDirection = facingDirection;
	}

	public final byte getFacingDirection() {
		return this.facingDirection;
	}

	public void setTopLeft(final Point tl) {
		this.tl = tl;
	}

	public void setBottomRight(final Point br) {
		this.br = br;
	}

	public Point getTopLeft() {
		return this.tl;
	}

	public Point getBottomRight() {
		return this.br;
	}

	public void addState(final byte state, final int type, final Pair<Integer, Integer> reactItem, final byte nextState) {
		final StateData newState = new StateData(type, reactItem, nextState);
		this.stateInfo.put(state, newState);
	}

	public byte getNextState(final byte state) {
		final StateData nextState = this.stateInfo.get(state);
		if (nextState != null) {
			return nextState.getNextState();
		} else {
			return -1;
		}
	}

	public int getType(final byte state) {
		final StateData nextState = this.stateInfo.get(state);
		if (nextState != null) {
			return nextState.getType();
		} else {
			return -1;
		}
	}

	public Pair<Integer, Integer> getReactItem(final byte state) {
		final StateData nextState = this.stateInfo.get(state);
		if (nextState != null) {
			return nextState.getReactItem();
		} else {
			return null;
		}
	}

	private static class StateData {

		private final int type;
		private final Pair<Integer, Integer> reactItem;
		private final byte nextState;

		private StateData(final int type, final Pair<Integer, Integer> reactItem, final byte nextState) {
			this.type = type;
			this.reactItem = reactItem;
			this.nextState = nextState;
		}

		private int getType() {
			return this.type;
		}

		private byte getNextState() {
			return this.nextState;
		}

		private Pair<Integer, Integer> getReactItem() {
			return this.reactItem;
		}
	}
}
