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
package javastory.channel.maps;

import java.awt.Rectangle;

import javastory.channel.ChannelClient;
import javastory.scripting.ReactorScriptManager;
import javastory.tools.Pair;
import javastory.tools.packets.ChannelPackets;

public class Reactor extends AbstractGameMapObject {

	private int rid;
	private ReactorInfo stats;
	private byte state;
	private int delay;
	private GameMap map;
	private String name;
	private boolean timerActive, alive;

	public Reactor(ReactorInfo stats, int rid) {
		this.stats = stats;
		this.rid = rid;
		alive = true;
	}

	public final byte getFacingDirection() {
		return stats.getFacingDirection();
	}

	public void setTimerActive(boolean active) {
		this.timerActive = active;
	}

	public boolean isTimerActive() {
		return timerActive;
	}

	public int getReactorId() {
		return rid;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public byte getState() {
		return state;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getDelay() {
		return delay;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.REACTOR;
	}

	public int getReactorType() {
		return stats.getType(state);
	}

	public void setMap(GameMap map) {
		this.map = map;
	}

	public GameMap getMap() {
		return map;
	}

	public Pair<Integer, Integer> getReactItem() {
		return stats.getReactItem(state);
	}

	@Override
	public void sendDestroyData(ChannelClient client) {
		client.write(ChannelPackets.destroyReactor(this));
	}

	@Override
	public void sendSpawnData(ChannelClient client) {
		client.write(ChannelPackets.spawnReactor(this));
	}

	public void forceStartReactor(ChannelClient c) {
		ReactorScriptManager.getInstance().act(c, this);
	}

	// hitReactor command for item-triggered reactors
	public void hitReactor(ChannelClient c) {
		hitReactor(0, (short) 0, c);
	}

	public void hitReactor(int charPos, short stance, ChannelClient c) {
		if (stats.getType(state) < 999 && stats.getType(state) != -1) {
			// type 2 = only hit from right (kerning swamp plants), 00 is air
			// left 02 is ground left

			if (!(stats.getType(state) == 2 && (charPos == 0 || charPos == 2))) { // next
																					// state
				state = stats.getNextState(state);

				if (stats.getNextState(state) == -1) { // end of reactor
					if (stats.getType(state) < 100) { // reactor broken
						if (delay > 0) {
							map.destroyReactor(getObjectId());
						} else {// trigger as normal
							map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
						}
					} else { // item-triggered on final step
						map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
					}
					ReactorScriptManager.getInstance().act(c, this);
				} else { // reactor not broken yet
					map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
					if (state == stats.getNextState(state)) { // current state =
																// next state,
																// looping
																// reactor
						ReactorScriptManager.getInstance().act(c, this);
					}
				}
			}
		}
	}

	public Rectangle getArea() {
		int height = stats.getBottomRight().y - stats.getTopLeft().y;
		int width = stats.getBottomRight().x - stats.getTopLeft().x;
		int origX = getPosition().x + stats.getTopLeft().x;
		int origY = getPosition().y + stats.getTopLeft().y;

		return new Rectangle(origX, origY, width, height);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Reactor " + getObjectId() + " of id " + rid + " at position " + getPosition().toString() + " state" + state + " type " + stats.getType(state);
	}
}
