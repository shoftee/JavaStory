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
import javastory.game.data.ReactorInfo;
import javastory.scripting.ReactorScriptManager;
import javastory.tools.Pair;
import javastory.tools.packets.ChannelPackets;

public class Reactor extends AbstractGameMapObject {

	private final int rid;
	private final ReactorInfo stats;
	private byte state;
	private int delay;
	private GameMap map;
	private String name;
	private boolean timerActive, alive;

	public Reactor(final ReactorInfo stats, final int rid) {
		this.stats = stats;
		this.rid = rid;
		this.alive = true;
	}

	public final byte getFacingDirection() {
		return this.stats.getFacingDirection();
	}

	public void setTimerActive(final boolean active) {
		this.timerActive = active;
	}

	public boolean isTimerActive() {
		return this.timerActive;
	}

	public int getReactorId() {
		return this.rid;
	}

	public void setState(final byte state) {
		this.state = state;
	}

	public byte getState() {
		return this.state;
	}

	public boolean isAlive() {
		return this.alive;
	}

	public void setAlive(final boolean alive) {
		this.alive = alive;
	}

	public void setDelay(final int delay) {
		this.delay = delay;
	}

	public int getDelay() {
		return this.delay;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.REACTOR;
	}

	public int getReactorType() {
		return this.stats.getType(this.state);
	}

	public void setMap(final GameMap map) {
		this.map = map;
	}

	public GameMap getMap() {
		return this.map;
	}

	public Pair<Integer, Integer> getReactItem() {
		return this.stats.getReactItem(this.state);
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.destroyReactor(this));
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		client.write(ChannelPackets.spawnReactor(this));
	}

	public void forceStartReactor(final ChannelClient c) {
		ReactorScriptManager.getInstance().act(c, this);
	}

	// hitReactor command for item-triggered reactors
	public void hitReactor(final ChannelClient c) {
		this.hitReactor(0, (short) 0, c);
	}

	public void hitReactor(final int charPos, final short stance, final ChannelClient c) {
		if (this.stats.getType(this.state) < 999 && this.stats.getType(this.state) != -1) {
			// type 2 = only hit from right (kerning swamp plants), 00 is air
			// left 02 is ground left

			if (!(this.stats.getType(this.state) == 2 && (charPos == 0 || charPos == 2))) { // next
																					// state
				this.state = this.stats.getNextState(this.state);

				if (this.stats.getNextState(this.state) == -1) { // end of reactor
					if (this.stats.getType(this.state) < 100) { // reactor broken
						if (this.delay > 0) {
							this.map.destroyReactor(this.getObjectId());
						} else {// trigger as normal
							this.map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
						}
					} else { // item-triggered on final step
						this.map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
					}
					ReactorScriptManager.getInstance().act(c, this);
				} else { // reactor not broken yet
					this.map.broadcastMessage(ChannelPackets.triggerReactor(this, stance));
					if (this.state == this.stats.getNextState(this.state)) { // current state =
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
		final int height = this.stats.getBottomRight().y - this.stats.getTopLeft().y;
		final int width = this.stats.getBottomRight().x - this.stats.getTopLeft().x;
		final int origX = this.getPosition().x + this.stats.getTopLeft().x;
		final int origY = this.getPosition().y + this.stats.getTopLeft().y;

		return new Rectangle(origX, origY, width, height);
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Reactor " + this.getObjectId() + " of id " + this.rid + " at position " + this.getPosition().toString() + " state" + this.state + " type " + this.stats.getType(this.state);
	}
}
