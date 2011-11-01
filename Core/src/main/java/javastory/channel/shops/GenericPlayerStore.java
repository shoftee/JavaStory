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
package javastory.channel.shops;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.packet.PlayerShopPacket;

public class GenericPlayerStore extends AbstractPlayerShop {

	private boolean open;
	private final ChannelCharacter owner;
	private final List<String> bannedList = Lists.newArrayList();

	public GenericPlayerStore(final ChannelCharacter owner, final int itemId, final String desc) {
		super(owner, itemId, desc);
		this.owner = owner;
		this.open = false;
	}

	@Override
	public void buy(final ChannelClient c, final int item, final short quantity) {
		final PlayerShopItem pItem = this.items.get(item);
		if (pItem.bundles > 0) {
			this.owner.getClient().write(PlayerShopPacket.shopItemUpdate(this));
		}
	}

	@Override
	public byte getShopType() {
		return PlayerShop.PLAYER_SHOP;
	}

	@Override
	public void closeShop(final boolean saveItems, final boolean remove) {
		this.owner.getMap().broadcastMessage(PlayerShopPacket.removeCharBox(this.owner));
		this.owner.getMap().removeMapObject(this);

		if (saveItems) {
			this.saveItems();
		}
		this.owner.setPlayerShop(null);
	}

	public void banPlayer(final String name) {
		if (!this.bannedList.contains(name)) {
			this.bannedList.add(name);
		}
		for (int i = 0; i < 3; i++) {
			final ChannelCharacter chr = this.getVisitor(i);
			if (chr.getName().equals(name)) {
				chr.getClient().write(PlayerShopPacket.shopErrorMessage(5, 1));
				chr.setPlayerShop(null);
				this.removeVisitor(chr);
			}
		}
	}

	@Override
	public void setOpen(final boolean open) {
		this.open = open;
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	public boolean isBanned(final String name) {
		if (this.bannedList.contains(name)) {
			return true;
		}
		return false;
	}

	public ChannelCharacter getMCOwner() {
		return this.owner;
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.SHOP;
	}
}
