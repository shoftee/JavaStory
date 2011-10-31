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
package javastory.channel.life;

import javastory.channel.ChannelClient;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.server.ShopFactory;
import javastory.game.data.NpcInfo;
import javastory.tools.packets.ChannelPackets;

public class Npc extends AbstractLoadedLife {

	private final NpcInfo stats;
	private boolean custom = false;

	public Npc(final int id, final NpcInfo stats) {
		super(id);
		this.stats = stats;
	}

	public final boolean hasShop() {
		return ShopFactory.getInstance().getShopForNPC(this.getId()) != null;
	}

	public final void sendShop(final ChannelClient c) {
		ShopFactory.getInstance().getShopForNPC(this.getId()).sendShop(c);
	}

	@Override
	public final void sendSpawnData(final ChannelClient client) {

		if (this.getId() >= 9901000 && this.getId() <= 9901551) {
			if (!this.stats.getName().equals("")) {
				client.write(ChannelPackets.spawnPlayerNpc(this.stats, this.getId()));
				client.write(ChannelPackets.spawnNpcRequestController(this, false));
			}
		} else {
			client.write(ChannelPackets.spawnNpc(this, true));
			client.write(ChannelPackets.spawnNpcRequestController(this, true));
		}
	}

	@Override
	public final void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removeNpc(this.getObjectId()));
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.NPC;
	}

	public final String getName() {
		return this.stats.getName();
	}

	public final boolean isCustom() {
		return this.custom;
	}

	public final void setCustom(final boolean custom) {
		this.custom = custom;
	}
}
