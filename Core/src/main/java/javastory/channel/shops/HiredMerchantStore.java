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

import java.util.concurrent.ScheduledFuture;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.packet.PlayerShopPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.game.GameConstants;
import javastory.game.Item;
import javastory.game.ItemFlag;
import javastory.server.TimerManager;

public class HiredMerchantStore extends AbstractPlayerShop {

	public ScheduledFuture<?> schedule;
	private GameMap map;
	private int storeid;
	private final long start;

	public HiredMerchantStore(final ChannelCharacter owner, final int itemId, final String desc) {
		super(owner, itemId, desc);
		this.start = System.currentTimeMillis();
		this.map = owner.getMap();
		this.schedule = TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				HiredMerchantStore.this.closeShop(true, true);
			}
		}, 1000 * 60 * 60 * 24);
	}

	@Override
	public byte getShopType() {
		return PlayerShop.HIRED_MERCHANT;
	}

	public final void setStoreid(final int storeId) {
		this.storeid = storeId;
	}

	@Override
	public void buy(final ChannelClient c, final int item, final short quantity) {
		final PlayerShopItem pItem = this.items.get(item);
		final Item shopItem = pItem.item;
		final Item newItem = shopItem.copy();
		final short perbundle = newItem.getQuantity();

		newItem.setQuantity((short) (quantity * perbundle));

		final byte flag = newItem.getFlag();

		if (ItemFlag.KARMA_EQ.check(flag)) {
			newItem.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
		} else if (ItemFlag.KARMA_USE.check(flag)) {
			newItem.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
		}
		final ChannelCharacter player = c.getPlayer();

		if (InventoryManipulator.addFromDrop(c, newItem, false)) {
			pItem.bundles -= quantity; // Number remaining in the store

			final int gainmeso = this.getMeso() + pItem.price * quantity;
			this.setMeso(gainmeso - GameConstants.EntrustedStoreTax(gainmeso));
			player.gainMeso(-pItem.price * quantity, false);
		} else {
			player.sendNotice(1, "Your inventory is full.");
		}
	}

	@Override
	public void closeShop(final boolean saveItems, final boolean remove) {
		if (this.schedule != null) {
			this.schedule.cancel(false);
		}
		if (saveItems) {
			this.saveItems();
		}
		if (remove) {
			ChannelServer.getInstance().removeMerchant(this);
			this.map.broadcastMessage(PlayerShopPacket.destroyHiredMerchant(this.getOwnerId()));
		}
		this.map.removeMapObject(this);

		this.map = null;
		this.schedule = null;
	}

	public int getTimeLeft() {
		return (int) ((System.currentTimeMillis() - this.start) / 1000);
	}

	public GameMap getMap() {
		return this.map;
	}

	public final int getStoreId() {
		return this.storeid;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.HIRED_MERCHANT;
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(PlayerShopPacket.destroyHiredMerchant(this.getOwnerId()));
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		client.write(PlayerShopPacket.spawnHiredMerchant(this));
	}
}
