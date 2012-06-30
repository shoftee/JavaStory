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
import javastory.game.IdQuantityEntry;
import javastory.scripting.ReactorScriptManager;
import javastory.tools.packets.ChannelPackets;

public class Reactor extends AbstractGameMapObject {

	private final int reactorId;
	private final MapReactorInfo localInfo;
	private int stateId;
	private int delay;
	private GameMap map;
	private String name;
	private boolean timerActive, alive;

	public Reactor(final MapReactorInfo info, final int reactorId) {
		this.localInfo = info;
		this.reactorId = reactorId;
		this.alive = true;
	}

	public final byte getFacingDirection() {
		return this.localInfo.getFacingDirection();
	}

	public void setTimerActive(final boolean active) {
		this.timerActive = active;
	}

	public boolean isTimerActive() {
		return this.timerActive;
	}

	public int getReactorId() {
		return this.reactorId;
	}

	public void setStateId(final byte state) {
		this.stateId = state;
	}

	public int getStateId() {
		return this.stateId;
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
		return this.localInfo.getPrototype().getType(this.stateId);
	}

	public void setMap(final GameMap map) {
		this.map = map;
	}

	public GameMap getMap() {
		return this.map;
	}

	public IdQuantityEntry getReactItem() {
		return this.localInfo.getPrototype().getReactionItem(this.stateId);
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
		if (this.localInfo.getType(this.stateId) < 999 && this.localInfo.getType(this.stateId) != -1) {
			// type 2 = only hit from right (kerning swamp plants), 00 is air
			// left 02 is ground left

			if (!(this.localInfo.getType(this.stateId) == 2 && (charPos == 0 || charPos == 2))) { // next
				// state
				this.stateId = this.localInfo.getNextState(this.stateId);

				if (this.localInfo.getNextState(this.stateId) == -1) { // end of reactor
					if (this.localInfo.getType(this.stateId) < 100) { // reactor broken
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
					if (this.stateId == this.localInfo.getNextState(this.stateId)) { // current state =
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
		final Rectangle bounds = this.localInfo.getPrototype().getBounds();
		final Rectangle area = new Rectangle(bounds);
		area.add(this.getPosition());
		return area;
	}

	public String getName() {
		return this.localInfo.getName();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Reactor instance ").append(this.getObjectId()).append("; ");
		builder.append("prototype ").append(this.reactorId).append("; ");
		builder.append("position ").append(this.getPosition().toString()).append("; ");
		builder.append("state ").append(this.stateId).append("; ");
		builder.append("type ").append(this.localInfo.getType(this.stateId)).append("; ");
		return builder.toString();
	}
}
