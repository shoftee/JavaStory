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

import javastory.channel.ChannelClient;
import javastory.io.GamePacket;
import javastory.tools.packets.ChannelPackets;

public class GameMapEffect {

	private final String msg;
	private final int itemId;
	private boolean active = true;

	public GameMapEffect(final String msg, final int itemId) {
		this.msg = msg;
		this.itemId = itemId;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	public GamePacket makeDestroyData() {
		return ChannelPackets.removeMapEffect();
	}

	public GamePacket makeStartData() {
		return ChannelPackets.startMapEffect(this.msg, this.itemId, this.active);
	}

	public void sendStartData(final ChannelClient c) {
		c.write(ChannelPackets.startMapEffect(this.msg, this.itemId, this.active));
	}
}
