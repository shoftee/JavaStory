/*
 * This file is part of the ZeroFusion MapleStory Server Copyright (C) 2008
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de> ZeroFusion organized by "RMZero213"
 * <RMZero213@hotmail.com>
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

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.tools.packets.ChannelPackets;

public class Dragon extends AbstractAnimatedGameMapObject {

	private final int owner;
	private final int jobid;

	public Dragon(final ChannelCharacter owner) {
		super();
		this.owner = owner.getId();
		this.jobid = owner.getJobId();
		if (this.jobid < 2200 || this.jobid > 2218) {
			throw new RuntimeException("Trying to create a dragon for a non-Evan");
		}
		this.setPosition(owner.getPosition());
		this.setStance(4);
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		client.write(ChannelPackets.spawnDragon(this));
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removeDragon(this.owner));
	}

	public int getOwner() {
		return this.owner;
	}

	public int getJobId() {
		return this.jobid;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.SUMMON;
	}
}
