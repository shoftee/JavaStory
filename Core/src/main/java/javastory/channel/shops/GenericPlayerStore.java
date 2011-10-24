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

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.packet.PlayerShopPacket;

public class GenericPlayerStore extends AbstractPlayerShop {

	private boolean open;
	private ChannelCharacter owner;
	private List<String> bannedList = new ArrayList<>();

	public GenericPlayerStore(ChannelCharacter owner, int itemId, String desc) {
		super(owner, itemId, desc);
		this.owner = owner;
		open = false;
	}

	@Override
	public void buy(ChannelClient c, int item, short quantity) {
		PlayerShopItem pItem = items.get(item);
		if (pItem.bundles > 0) {
			owner.getClient().write(PlayerShopPacket.shopItemUpdate(this));
		}
	}

	@Override
	public byte getShopType() {
		return PlayerShop.PLAYER_SHOP;
	}

	@Override
	public void closeShop(boolean saveItems, boolean remove) {
		owner.getMap().broadcastMessage(PlayerShopPacket.removeCharBox(owner));
		owner.getMap().removeMapObject(this);

		if (saveItems) {
			saveItems();
		}
		owner.setPlayerShop(null);
	}

	public void banPlayer(String name) {
		if (!bannedList.contains(name)) {
			bannedList.add(name);
		}
		for (int i = 0; i < 3; i++) {
			ChannelCharacter chr = getVisitor(i);
			if (chr.getName().equals(name)) {
				chr.getClient().write(PlayerShopPacket.shopErrorMessage(5, 1));
				chr.setPlayerShop(null);
				removeVisitor(chr);
			}
		}
	}

	@Override
	public void setOpen(boolean open) {
		this.open = open;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	public boolean isBanned(String name) {
		if (bannedList.contains(name)) {
			return true;
		}
		return false;
	}

	public ChannelCharacter getMCOwner() {
		return owner;
	}

	@Override
	public void sendDestroyData(ChannelClient client) {
	}

	@Override
	public void sendSpawnData(ChannelClient client) {
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.SHOP;
	}
}
